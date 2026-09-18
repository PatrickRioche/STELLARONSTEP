package fr.stellaronstep.app.data

import fr.stellaronstep.app.core.onstep.MountStatus
import fr.stellaronstep.app.core.onstep.OnStepDiagnostics
import fr.stellaronstep.app.core.onstep.OnStepStatusParser
import fr.stellaronstep.app.core.onstep.OnStepTcpClient
import fr.stellaronstep.app.core.onstep.SlewDirection
import fr.stellaronstep.app.core.onstep.SlewRate
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.roundToInt

class OnStepRepository(
    private val client: OnStepTcpClient
) {
    suspend fun readStatus(): MountStatus {
        val status = client.queryHash(":GU#")
        val ra = client.queryHash(":GR#")
        val dec = client.queryHash(":GD#")
        return OnStepStatusParser.parse(status, ra, dec)
    }

    suspend fun readDiagnostics(): OnStepDiagnostics {
        suspend fun read(command: String): String =
            runCatching {
                client.queryHash(command).trim()
            }.getOrElse {
                "N/A"
            }

        return OnStepDiagnostics(
            latitude = read(":Gt#"),
            longitudeOnStep = read(":Gg#"),
            date = read(":GC#"),
            localTime = read(":GL#"),
            utcOffset = read(":GG#"),
            siderealTime = read(":GS#"),
            dateTimeReady = read(":GX89#"),
            lastError = read(":GE#"),
            mountType = read(":GXEM#"),
            axis1Deg = read(":GX42#"),
            axis2Deg = read(":GX43#"),
            altitude = read(":GA#"),
            azimuth = read(":GZ#"),
            horizonLimit = read(":Gh#"),
            overheadLimit = read(":Go#"),
            pierSide = read(":Gm#"),
            rawStatus = read(":GU#"),
            eastPastMeridianMin = read(":GXE9#"),
            westPastMeridianMin = read(":GXEA#"),
            axis1MinDeg = read(":GXEe#"),
            axis1MaxDeg = read(":GXEw#"),
            axis2MinDeg = read(":GXEC#"),
            axis2MaxDeg = read(":GXED#")
        )
    }

    suspend fun setSlewRate(rate: SlewRate) {
        client.send(rate.command)
    }

    suspend fun startMove(
        direction: SlewDirection,
        rate: SlewRate
    ) {
        client.send(rate.command)
        delay(80)
        client.send(direction.startCommand)
    }

    suspend fun stopMove(direction: SlewDirection) {
        client.send(direction.stopCommand)
    }

    suspend fun emergencyStop() {
        client.send(":Q#")
    }

    private suspend fun lastErrorCode(): String =
        runCatching {
            client.queryHash(":GE#").trim()
        }.getOrElse {
            "??"
        }

    private fun errorText(code: String): String =
        when (code.padStart(2, '0')) {
            "00" -> "aucune erreur"
            "01" -> "echec generique"
            "02" -> "commande inconnue"
            "03" -> "reponse invalide"
            "04" -> "parametre hors plage"
            "05" -> "format de parametre invalide"
            "08" -> "monture ni parkee ni au HOME"
            "09" -> "monture deja parkee"
            "10" -> "echec PARK"
            "11" -> "monture non parkee"
            "12" -> "aucune position PARK memorisee"
            "13" -> "echec GOTO"
            "15" -> "cible sous l'horizon"
            "16" -> "cible au-dessus de la limite haute"
            "17" -> "controleur en veille"
            "18" -> "monture parkee"
            "19" -> "GOTO deja actif"
            "20" -> "hors limites configurees"
            "21" -> "defaut materiel"
            "22" -> "monture deja en mouvement"
            "23" -> "erreur de slew non specifiee / autorite de position possiblement non valide"
            "25" -> "succes explicite"
            else -> "erreur OnStepX $code"
        }

    suspend fun syncPhoneClock(
        date: String,
        time: String,
        utcOffset: String
    ): String {
        val offsetOk =
            client.queryByte(":SG$utcOffset#") == "1"

        delay(80)

        val dateOk =
            client.queryByte(":SC$date#") == "1"

        delay(80)

        val timeOk =
            client.queryByte(":SL$time#") == "1"

        delay(250)

        val ready =
            runCatching {
                client.queryHash(":GX89#").trim()
            }.getOrElse {
                "?"
            }

        return if (
            offsetOk &&
            dateOk &&
            timeOk &&
            ready == "0"
        ) {
            "Date/heure OnStepX synchronisees avec le telephone"
        } else {
            "Sync horloge incomplete | SG=${if (offsetOk) "1" else "0"} SC=${if (dateOk) "1" else "0"} SL=${if (timeOk) "1" else "0"} GX89=$ready"
        }
    }

    suspend fun initializeFromPhone(
        date: String,
        time: String,
        utcOffset: String,
        latitude: Double,
        androidLongitude: Double
    ): String {
        val latitudeValue =
            formatLatitude(latitude)

        /*
         * Android: Est positif, Ouest negatif.
         * Convention OnStep/LX200 utilisee ici:
         * Est negatif, Ouest positif.
         */
        val onStepLongitude =
            -androidLongitude

        val longitudeValue =
            formatLongitude(onStepLongitude)

        val utcOk =
            client.queryByte(
                ":SG$utcOffset#"
            ) == "1"

        delay(80)

        val latitudeOk =
            client.queryByte(
                ":St$latitudeValue#"
            ) == "1"

        delay(80)

        val longitudeOk =
            client.queryByte(
                ":Sg$longitudeValue#"
            ) == "1"

        delay(80)

        val dateOk =
            client.queryByte(
                ":SC$date#"
            ) == "1"

        delay(80)

        val timeOk =
            client.queryByte(
                ":SL$time#"
            ) == "1"

        delay(250)

        val ready =
            runCatching {
                client.queryHash(":GX89#").trim()
            }.getOrElse {
                "?"
            }

        val readLatitude =
            runCatching {
                client.queryHash(":Gt#").trim()
            }.getOrElse {
                "?"
            }

        val readLongitude =
            runCatching {
                client.queryHash(":Gg#").trim()
            }.getOrElse {
                "?"
            }

        val readDate =
            runCatching {
                client.queryHash(":GC#").trim()
            }.getOrElse {
                "?"
            }

        val readTime =
            runCatching {
                client.queryHash(":GL#").trim()
            }.getOrElse {
                "?"
            }

        return if (
            utcOk &&
            latitudeOk &&
            longitudeOk &&
            dateOk &&
            timeOk &&
            ready == "0"
        ) {
            "INITIALISATION OK | Lat=$readLatitude | Lon=$readLongitude | Date=$readDate | Heure=$readTime | Faire maintenant RESET HOME"
        } else {
            "INITIALISATION INCOMPLETE | SG=${bool(utcOk)} St=${bool(latitudeOk)} Sg=${bool(longitudeOk)} SC=${bool(dateOk)} SL=${bool(timeOk)} GX89=$ready"
        }
    }

    private fun bool(value: Boolean): String =
        if (value) "1" else "0"

    private fun formatLatitude(
        latitude: Double
    ): String {
        val safe =
            latitude.coerceIn(
                -90.0,
                90.0
            )

        val sign =
            if (safe < 0.0) "-" else "+"

        val totalSeconds =
            (abs(safe) * 3600.0)
                .roundToInt()

        val degrees =
            totalSeconds / 3600

        val minutes =
            (totalSeconds % 3600) / 60

        val seconds =
            totalSeconds % 60

        return "%s%02d*%02d:%02d".format(
            sign,
            degrees,
            minutes,
            seconds
        )
    }

    private fun formatLongitude(
        longitude: Double
    ): String {
        val safe =
            longitude.coerceIn(
                -180.0,
                180.0
            )

        val sign =
            if (safe < 0.0) "-" else "+"

        val totalSeconds =
            (abs(safe) * 3600.0)
                .roundToInt()

        val degrees =
            totalSeconds / 3600

        val minutes =
            (totalSeconds % 3600) / 60

        val seconds =
            totalSeconds % 60

        return "%s%03d*%02d:%02d".format(
            sign,
            degrees,
            minutes,
            seconds
        )
    }
    suspend fun tracking(enabled: Boolean): Boolean {
        val before = readStatus()

        if (enabled && before.parked) {
            return false
        }

        if (enabled) {
            /*
             * Force explicit sidereal tracking rate before enabling tracking.
             * :TQ# has no reply.
             */
            client.send(":TQ#")
            delay(100)
        } else {
            /*
             * Stop any residual guide/slew state before disabling tracking.
             */
            client.send(":Q#")
            delay(80)
        }

        var accepted =
            client.queryByte(
                if (enabled) ":Te#" else ":Td#"
            ) == "1"

        if (!accepted) {
            return false
        }

        repeat(6) {
            delay(250)

            val live =
                runCatching {
                    readStatus()
                }.getOrNull()

            if (
                live != null &&
                live.tracking == enabled
            ) {
                return true
            }
        }

        /*
         * Some WiFi command channels acknowledge before the state
         * transition is visible. Retry once, then trust GU state.
         */
        if (enabled) {
            client.send(":TQ#")
            delay(100)
        }

        accepted =
            client.queryByte(
                if (enabled) ":Te#" else ":Td#"
            ) == "1"

        if (!accepted) {
            return false
        }

        repeat(6) {
            delay(250)

            val live =
                runCatching {
                    readStatus()
                }.getOrNull()

            if (
                live != null &&
                live.tracking == enabled
            ) {
                return true
            }
        }

        return false
    }

    suspend fun park(): Boolean =
        client.queryByte(":hP#") == "1"

    suspend fun setParkPosition(): Boolean =
        client.queryByte(":hQ#") == "1"

    suspend fun unpark(): Boolean =
        client.queryByte(":hR#") == "1"

    suspend fun resetHome(): String {
        client.send(":Q#")
        delay(100)

        runCatching {
            tracking(false)
        }

        delay(100)

        client.send(":hF#")
        delay(120)

        val error =
            lastErrorCode()

        if (error != "00") {
            return "RESET HOME refuse | GE=$error (${errorText(error)})"
        }

        delay(250)

        val live =
            runCatching {
                readStatus()
            }.getOrNull()

        val homeInfo =
            runCatching {
                client.queryHash(":h?#").trim()
            }.getOrElse {
                "N/A"
            }

        return if (live != null) {
            "RESET HOME OK | HOME=${if (live.atHome) "OUI" else "NON"} | h?=$homeInfo | GU=${live.raw}"
        } else {
            "RESET HOME OK | h?=$homeInfo"
        }
    }
    suspend fun goHome(): String {
        val before = readStatus()
        val beforeRaw = before.raw

        val homeInfo =
            runCatching {
                client.queryHash(":h?#").trim()
            }.getOrElse {
                "N/A"
            }

        val dateReady =
            runCatching {
                client.queryHash(":GX89#").trim()
            }.getOrElse {
                "?"
            }

        if (before.atHome) {
            return "HOME deja atteint | deplacer d'abord la monture puis retester | h?=$homeInfo | GU=$beforeRaw"
        }

        if (before.parked) {
            if (!unpark()) {
                val error = lastErrorCode()
                return "HOME impossible : UNPARK refuse | GE=$error (${errorText(error)})"
            }
            delay(350)
        }

        client.send(":Q#")
        delay(120)

        runCatching {
            tracking(false)
        }

        delay(120)

        val startStatus =
            runCatching {
                readStatus()
            }.getOrElse {
                before
            }

        val startRa = startStatus.ra
        val startDec = startStatus.dec

        client.send(":hC#")
        delay(120)

        val commandError =
            lastErrorCode()

        if (commandError != "00") {
            return "HOME refuse | GE=$commandError (${errorText(commandError)}) | GX89=$dateReady | h?=$homeInfo | GU=${startStatus.raw}"
        }

        var last = startStatus
        var movementSeen = false

        repeat(20) {
            delay(500)

            val live =
                runCatching {
                    readStatus()
                }.getOrNull()

            if (live != null) {
                if (
                    live.ra != startRa ||
                    live.dec != startDec ||
                    live.slewing ||
                    live.raw.contains('h')
                ) {
                    movementSeen = true
                }

                last = live

                if (live.atHome) {
                    return "HOME atteint | GE=00 | h?=$homeInfo | GU=${live.raw}"
                }
            }
        }

        return if (movementSeen) {
            "HOME mouvement detecte mais non termine | GE=00 | h?=$homeInfo | GU=${last.raw}"
        } else {
            "HOME accepte mais aucun mouvement detecte | GE=00 | GX89=$dateReady | h?=$homeInfo | GU=${last.raw}"
        }
    }
    suspend fun goto(
        ra: String,
        dec: String
    ): String {
        val cleanRa = normalizeRa(ra)
            ?: return "RA invalide - format HH:MM:SS"

        val cleanDec = normalizeDec(dec)
            ?: return "DEC invalide - format +DD*MM:SS"

        val before = readStatus()

        if (before.parked) {
            return "GOTO refuse : monture parkee - faire UNPARK"
        }

        if (!before.tracking) {
            val trackingAccepted = tracking(true)

            if (!trackingAccepted) {
                return "GOTO refuse : impossible d'activer le suivi"
            }

            delay(250)
        }

        val raReply = client.queryByte(":Sr$cleanRa#")

        if (raReply != "1") {
            return "GOTO refuse : RA non acceptee (Sr=$raReply)"
        }

        delay(80)

        val decReply = client.queryByte(":Sd$cleanDec#")

        if (decReply != "1") {
            return "GOTO refuse : DEC non acceptee (Sd=$decReply)"
        }

        delay(80)

        val code = client.queryByte(":MS#")
        delay(80)

        val internalError =
            lastErrorCode()

        val text = when (code) {
            "0" -> "GOTO accepte"
            "1" -> "GOTO refuse : cible sous l'horizon"
            "2" -> "GOTO refuse : cible au-dessus de la limite haute"
            "3" -> "GOTO refuse : controleur en veille"
            "4" -> "GOTO refuse : monture parkee"
            "5" -> "GOTO refuse : GOTO deja en cours"
            "6" -> "GOTO refuse : hors limites"
            "7" -> "GOTO refuse : defaut materiel"
            "8" -> "GOTO refuse : monture deja en mouvement"
            "9" -> "GOTO refuse : erreur non specifiee"
            else -> "GOTO : reponse OnStep $code"
        }

        if (code != "0") {
            return "$text | MS=$code | GE=$internalError (${errorText(internalError)})"
        }

        delay(400)

        val live = runCatching {
            readStatus()
        }.getOrNull()

        return if (live?.slewing == true) {
            "$text - mouvement en cours"
        } else {
            "$text - commande recue, verifier mouvement"
        }
    }

    private fun normalizeRa(value: String): String? {
        val m = Regex("""^(\d{1,2}):(\d{1,2}):(\d{1,2})$""")
            .matchEntire(value.trim().replace(" ", ""))
            ?: return null

        val h = m.groupValues[1].toIntOrNull() ?: return null
        val min = m.groupValues[2].toIntOrNull() ?: return null
        val sec = m.groupValues[3].toIntOrNull() ?: return null

        if (h !in 0..23 || min !in 0..59 || sec !in 0..59) {
            return null
        }

        return "%02d:%02d:%02d".format(h, min, sec)
    }

    private fun normalizeDec(value: String): String? {
        val clean = value.trim()
            .replace(" ", "")
            .replace("deg", "*")
            .replace("d", "*")
            .replace("'", ":")
            .replace("\"", "")

        val m = Regex("""^([+-]?)(\d{1,2})\*(\d{1,2}):(\d{1,2})$""")
            .matchEntire(clean)
            ?: return null

        val sign = if (m.groupValues[1] == "-") "-" else "+"
        val deg = m.groupValues[2].toIntOrNull() ?: return null
        val min = m.groupValues[3].toIntOrNull() ?: return null
        val sec = m.groupValues[4].toIntOrNull() ?: return null

        if (deg !in 0..90 || min !in 0..59 || sec !in 0..59) {
            return null
        }

        return "%s%02d*%02d:%02d".format(sign, deg, min, sec)
    }

    suspend fun startAlignment(stars: Int): Boolean =
        client.queryByte(":A${stars.coerceIn(1, 9)}#") == "1"

    suspend fun alignmentStatus(): String =
        client.queryHash(":A?#")

    suspend fun acceptAlignmentStar(): Boolean =
        client.queryByte(":A+#") == "1"

    suspend fun saveAlignment(): Boolean =
        client.queryByte(":AW#") == "1"
}
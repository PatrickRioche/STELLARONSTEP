package fr.stellaronstep.app.data

import fr.stellaronstep.app.core.onstep.MountStatus
import fr.stellaronstep.app.core.onstep.OnStepStatusParser
import fr.stellaronstep.app.core.onstep.OnStepTcpClient
import fr.stellaronstep.app.core.onstep.SlewDirection
import fr.stellaronstep.app.core.onstep.SlewRate
import kotlinx.coroutines.delay

class OnStepRepository(
    private val client: OnStepTcpClient
) {
    suspend fun readStatus(): MountStatus {
        val status = client.queryHash(":GU#")
        val ra = client.queryHash(":GR#")
        val dec = client.queryHash(":GD#")
        return OnStepStatusParser.parse(status, ra, dec)
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

    suspend fun tracking(enabled: Boolean): Boolean =
        client.queryByte(if (enabled) ":Te#" else ":Td#") == "1"

    suspend fun park(): Boolean =
        client.queryByte(":hP#") == "1"

    suspend fun unpark(): Boolean =
        client.queryByte(":hR#") == "1"

    suspend fun goHome(): String {
        val before = readStatus()

        if (before.atHome) {
            return "HOME deja atteint"
        }

        if (before.parked) {
            if (!unpark()) {
                return "HOME impossible : UNPARK refuse"
            }
            delay(350)
        }

        runCatching { tracking(false) }
        delay(150)

        client.send(":hC#")

        repeat(6) {
            delay(500)
            val live = runCatching { readStatus() }.getOrNull()
            if (live?.atHome == true) {
                return "HOME atteint"
            }
        }

        return "HOME commande envoye - attente du marqueur H"
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

        val text = when (code) {
            "0" -> "GOTO accepte"
            "1" -> "GOTO refuse : cible sous l'horizon"
            "2" -> "GOTO refuse : cible au-dessus de la limite haute"
            "3" -> "GOTO refuse : controleur en veille"
            "4" -> "GOTO refuse : monture parkee"
            "5" -> "GOTO refuse : GOTO deja en cours"
            "6" -> "GOTO refuse : cible hors limites"
            "7" -> "GOTO refuse : defaut materiel"
            "8" -> "GOTO refuse : monture deja en mouvement"
            else -> "GOTO : reponse OnStep $code"
        }

        if (code != "0") {
            return text
        }

        delay(400)

        val live = runCatching { readStatus() }.getOrNull()

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
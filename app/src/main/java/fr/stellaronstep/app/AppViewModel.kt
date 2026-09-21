package fr.stellaronstep.app

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import fr.stellaronstep.app.core.onstep.MountStatus
import fr.stellaronstep.app.core.onstep.OnStepConnectionConfig
import fr.stellaronstep.app.core.onstep.OnStepDiagnostics
import fr.stellaronstep.app.core.onstep.SlewDirection
import fr.stellaronstep.app.core.onstep.SlewRate
import fr.stellaronstep.app.data.OnStepRepository
import fr.stellaronstep.app.data.OnStepSession
import fr.stellaronstep.app.data.PhoneLocationProvider
import fr.stellaronstep.app.data.SettingsStore
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.abs

class AppViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val settings =
        SettingsStore(application)

    var config by mutableStateOf(settings.load())
        private set

    var status by mutableStateOf(MountStatus())
        private set

    var diagnostics by mutableStateOf(OnStepDiagnostics())
        private set

    var message by mutableStateOf<String?>(null)
        private set

    var busy by mutableStateOf(false)
        private set

    var selectedRate by mutableStateOf(SlewRate.SLEW)
        private set

    var movingDirection by mutableStateOf<SlewDirection?>(null)
        private set
    private val client =
        OnStepSession.client(application)

    private val repository =
        OnStepSession.repository(application)

    private val phoneLocation =
        PhoneLocationProvider(application)

    private var pollingJob: Job? = null

    /*
     * Les commandes de mouvement manuel sont volontairement suivies.
     * Cela évite qu'une commande directionnelle retardée parte après
     * un STOP puis après le démarrage d'un HOME/GOTO/PARK.
     */
    private var manualMoveJob: Job? = null
    private var manualStopJob: Job? = null

    private var consecutivePollFailures = 0

    fun updateConfig(value: OnStepConnectionConfig) {
        config = value
        settings.save(value)
        message = "Configuration enregistree"
    }

    fun connectAndPoll() {
        if (pollingJob?.isActive == true) {
            return
        }

        /*
         * Polling strictement sequentiel.
         * L'ancien code appelait refreshStatus(), qui relancait une
         * coroutine et pouvait empiler des lectures TCP.
         */
        pollingJob =
            viewModelScope.launch {
                while (isActive) {
                    try {
                        status =
                            repository.readStatus()

                        consecutivePollFailures =
                            0
                    } catch (
                        exception: Exception
                    ) {
                        consecutivePollFailures++

                        /*
                         * Une lecture ratee ne suffit plus a faire clignoter
                         * l'etat de connexion.
                         */
                        if (
                            consecutivePollFailures >=
                            3
                        ) {
                            status =
                                status.copy(
                                    connected =
                                        false
                                )
                        }
                    }

                    delay(1500)
                }
            }
    }

    fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }

    fun refreshStatus(
        silent: Boolean = false
    ) = action(silent) {
        status = repository.readStatus()

        if (!silent) {
            message = "Etat actualise"
        }
    }

    fun refreshDiagnostics() = action {
        diagnostics = repository.readDiagnostics()
        status = repository.readStatus()
        message = "Diagnostic OnStepX actualise"
    }

    fun syncPhoneClock() = action {
        val now =
            ZonedDateTime.now()

        val date =
            now.format(
                DateTimeFormatter.ofPattern("MM/dd/yyyy")
            )

        val time =
            now.format(
                DateTimeFormatter.ofPattern("HH:mm:ss")
            )

        /*
         * OnStep convention:
         * UTC = local time + GG offset.
         * Android gives local = UTC + device offset,
         * therefore OnStep offset is the opposite sign.
         */
        val onStepOffsetSeconds =
            -now.offset.totalSeconds

        val absolute =
            abs(onStepOffsetSeconds)

        val hours =
            absolute / 3600

        val minutes =
            (absolute % 3600) / 60

        val sign =
            if (onStepOffsetSeconds < 0) "-" else "+"

        val utcOffset =
            "%s%02d:%02d".format(
                sign,
                hours,
                minutes
            )

        message =
            repository.syncPhoneClock(
                date,
                time,
                utcOffset
            )

        diagnostics =
            repository.readDiagnostics()

        status =
            repository.readStatus()
    }

    fun initializeFromPhone() = action {
        if (!phoneLocation.hasLocationPermission()) {
            message =
                "Autorisation de localisation requise"
            return@action
        }

        val location =
            phoneLocation.currentLocation()

        if (location == null) {
            message =
                "Position GPS indisponible - activer la localisation du telephone puis recommencer"
            return@action
        }

        val now =
            ZonedDateTime.now()

        val date =
            now.format(
                DateTimeFormatter.ofPattern(
                    "MM/dd/yyyy"
                )
            )

        val time =
            now.format(
                DateTimeFormatter.ofPattern(
                    "HH:mm:ss"
                )
            )

        /*
         * OnStep: UTC = heure locale + offset.
         * Android: heure locale = UTC + offset.
         * Le signe doit donc etre inverse.
         */
        val onStepOffsetSeconds =
            -now.offset.totalSeconds

        val absolute =
            abs(onStepOffsetSeconds)

        val hours =
            absolute / 3600

        val minutes =
            (absolute % 3600) / 60

        val sign =
            if (onStepOffsetSeconds < 0) {
                "-"
            } else {
                "+"
            }

        val utcOffset =
            "%s%02d:%02d".format(
                sign,
                hours,
                minutes
            )

        message =
            repository.initializeFromPhone(
                date = date,
                time = time,
                utcOffset = utcOffset,
                latitude = location.latitude,
                androidLongitude = location.longitude
            )

        diagnostics =
            repository.readDiagnostics()

        status =
            repository.readStatus()
    }
    fun initializePreferredFromPhone() = action {
        if (!phoneLocation.hasLocationPermission()) {
            message =
                "Autorisation de localisation requise"
            return@action
        }

        val now =
            ZonedDateTime.now()

        val date =
            now.format(
                DateTimeFormatter.ofPattern(
                    "MM/dd/yyyy"
                )
            )

        val time =
            now.format(
                DateTimeFormatter.ofPattern(
                    "HH:mm:ss"
                )
            )

        val onStepOffsetSeconds =
            -now.offset.totalSeconds

        val absolute =
            abs(onStepOffsetSeconds)

        val hours =
            absolute / 3600

        val minutes =
            (absolute % 3600) / 60

        val sign =
            if (onStepOffsetSeconds < 0) {
                "-"
            } else {
                "+"
            }

        val utcOffset =
            "%s%02d:%02d".format(
                sign,
                hours,
                minutes
            )

        val location =
            runCatching {
                phoneLocation.currentLocation()
            }.getOrNull()

        message =
            if (location != null) {
                repository.initializeFromPhone(
                    date = date,
                    time = time,
                    utcOffset = utcOffset,
                    latitude = location.latitude,
                    androidLongitude = location.longitude
                )
            } else {
                val clockResult =
                    repository.syncPhoneClock(
                        date = date,
                        time = time,
                        utcOffset = utcOffset
                    )

                "GPS indisponible - position OnStepX conservee | $clockResult"
            }

        diagnostics =
            repository.readDiagnostics()

        status =
            repository.readStatus()
    }
    fun setRate(rate: SlewRate) {
        selectedRate = rate

        action {
            repository.setSlewRate(rate)
            message = "Vitesse : ${rate.label}"
        }
    }

    fun startMove(direction: SlewDirection) {
        if (!status.connected) {
            message = "Monture non connectee"
            return
        }

        if (status.parked) {
            message = "Mouvement refuse : monture parkee"
            return
        }

        /*
         * Attendre le STOP précédent avant de lancer un nouveau mouvement.
         * Le délai interne de startMove() est annulable : une libération
         * rapide du bouton ne peut donc plus démarrer le mouvement après STOP.
         */
        manualMoveJob?.cancel()

        val previousStop = manualStopJob
        val rate = selectedRate

        movingDirection = direction

        manualMoveJob =
            viewModelScope.launch {
                try {
                    previousStop?.join()

                    if (movingDirection != direction) {
                        return@launch
                    }

                    repository.startMove(
                        direction,
                        rate
                    )

                    if (movingDirection == direction) {
                        message =
                            "${direction.label} - ${rate.label}"
                    }
                } catch (
                    exception: Exception
                ) {
                    if (movingDirection == direction) {
                        movingDirection = null
                    }

                    message =
                        exception.message
                            ?: "Erreur mouvement OnStep"
                }
            }
    }

    fun stopMove(
        direction: SlewDirection
    ) {
        val startJob =
            manualMoveJob

        manualMoveJob = null

        if (movingDirection == direction) {
            movingDirection = null
        }

        manualStopJob =
            viewModelScope.launch {
                try {
                    /*
                     * Annuler et terminer la séquence START avant d'envoyer STOP.
                     * Sinon :RM# peut être suivi de :Mn# après le STOP.
                     */
                    startJob?.cancel()
                    startJob?.join()

                    repository.stopMove(direction)
                    message = "Mouvement arrete"
                } catch (
                    exception: Exception
                ) {
                    message =
                        exception.message
                            ?: "Erreur arret mouvement"
                }
            }
    }

    private suspend fun prepareForAutomaticMotion() {
        val startJob =
            manualMoveJob

        manualMoveJob = null
        movingDirection = null

        /*
         * 1. Empêcher tout START retardé.
         * 2. Attendre le STOP envoyé à la libération du bouton.
         * 3. Envoyer un STOP global.
         * 4. Laisser OnStepX stabiliser son état avant HOME/GOTO/PARK.
         */
        startJob?.cancel()
        startJob?.join()

        manualStopJob?.join()
        manualStopJob = null

        repository.emergencyStop()
        delay(180)
    }

    fun stopAll() {
        action {
            prepareForAutomaticMotion()
            message = "STOP GLOBAL envoye"
        }
    }

    fun tracking(
        enabled: Boolean
    ) = action {
        val ok =
            repository.tracking(enabled)

        status =
            repository.readStatus()

        message =
            if (ok) {
                if (enabled) {
                    "Suivi sideral ON"
                } else {
                    "Suivi OFF"
                }
            } else {
                if (enabled && status.parked) {
                    "Suivi impossible : monture parkee"
                } else {
                    "Commande suivi ${if (enabled) "ON" else "OFF"} non confirmee | GU=${status.raw}"
                }
            }
    }

    fun park() = action {
        prepareForAutomaticMotion()

        val ok = repository.park()

        delay(200)

        status =
            runCatching {
                repository.readStatus()
            }.getOrElse {
                status
            }

        message =
            if (ok) {
                "PARK demande"
            } else {
                "PARK refuse par OnStepX"
            }
    }

    fun setParkPosition() = action {
        val ok =
            repository.setParkPosition()

        status =
            repository.readStatus()

        message =
            if (ok) {
                "RESET PARK OK - position actuelle memorisee"
            } else {
                "RESET PARK refuse par OnStepX"
            }
    }

    fun resetHome() = action {
        prepareForAutomaticMotion()

        message =
            repository.resetHome()

        status =
            repository.readStatus()
    }

    fun unpark() = action {
        val before =
            status

        val ok =
            repository.unpark()

        delay(350)

        status =
            repository.readStatus()

        message =
            when {
                !ok ->
                    "UNPARK refuse par OnStepX"

                before.parked &&
                    !status.parked ->
                    "UNPARK OK - monture operationnelle"

                !before.parked &&
                    before.atHome ->
                    "UNPARK OK depuis HOME - limites et suivi OnStepX initialises"

                else ->
                    "UNPARK OK"
            }
    }

    fun goHome() = action {
        prepareForAutomaticMotion()

        message = repository.goHome()
        status = repository.readStatus()
    }

    fun goto(
        ra: String,
        dec: String
    ) = action {
        prepareForAutomaticMotion()

        message = repository.goto(ra, dec)
        status = repository.readStatus()
    }

    fun startAlignment(
        stars: Int
    ) = action {
        prepareForAutomaticMotion()

        message =
            if (repository.startAlignment(stars)) {
                "Alignement $stars etoile(s) demarre"
            } else {
                "Alignement refuse"
            }
    }

    fun alignmentStatus() = action {
        message =
            "Alignement: ${repository.alignmentStatus()}"
    }

    fun acceptAlignmentStar() = action {
        message =
            if (repository.acceptAlignmentStar()) {
                "Etoile acceptee"
            } else {
                "Correction refusee"
            }
    }

    fun saveAlignment() = action {
        message =
            if (repository.saveAlignment()) {
                "Modele d'alignement sauvegarde"
            } else {
                "Sauvegarde refusee"
            }
    }

    private fun action(
        silent: Boolean = false,
        block: suspend () -> Unit
    ) {
        viewModelScope.launch {
            if (!silent) {
                busy = true
            }

            try {
                block()
            } catch (e: Exception) {
                status =
                    status.copy(
                        connected = false
                    )

                movingDirection = null

                if (!silent) {
                    message =
                        e.message
                            ?: "Erreur OnStep"
                }
            } finally {
                if (!silent) {
                    busy = false
                }
            }
        }
    }
}
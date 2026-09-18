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
import fr.stellaronstep.app.core.onstep.OnStepTcpClient
import fr.stellaronstep.app.core.onstep.SlewDirection
import fr.stellaronstep.app.core.onstep.SlewRate
import fr.stellaronstep.app.data.OnStepRepository
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
        OnStepTcpClient { config }

    private val repository =
        OnStepRepository(client)

    private var pollingJob: Job? = null

    fun updateConfig(value: OnStepConnectionConfig) {
        config = value
        settings.save(value)
        message = "Configuration enregistree"
    }

    fun connectAndPoll() {
        if (pollingJob?.isActive == true) {
            return
        }

        pollingJob = viewModelScope.launch {
            while (isActive) {
                refreshStatus(silent = true)
                delay(1200)
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

        movingDirection = direction

        action {
            repository.startMove(
                direction,
                selectedRate
            )
            message = "${direction.label} - ${selectedRate.label}"
        }
    }

    fun stopMove(
        direction: SlewDirection
    ) = action {
        repository.stopMove(direction)

        if (movingDirection == direction) {
            movingDirection = null
        }

        message = "Mouvement arrete"
    }

    fun stopAll() {
        movingDirection = null

        action {
            repository.emergencyStop()
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
        val ok = repository.park()

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
        message =
            repository.resetHome()

        status =
            repository.readStatus()
    }

    fun unpark() = action {
        repository.unpark()
        message = "Unpark demande"
        delay(300)
        status = repository.readStatus()
    }

    fun goHome() = action {
        message = repository.goHome()
        status = repository.readStatus()
    }

    fun goto(
        ra: String,
        dec: String
    ) = action {
        message = repository.goto(ra, dec)
        status = repository.readStatus()
    }

    fun startAlignment(
        stars: Int
    ) = action {
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
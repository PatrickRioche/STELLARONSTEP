package fr.stellaronstep.app

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import fr.stellaronstep.app.core.onstep.MountStatus
import fr.stellaronstep.app.core.onstep.OnStepConnectionConfig
import fr.stellaronstep.app.core.onstep.OnStepTcpClient
import fr.stellaronstep.app.core.onstep.SlewDirection
import fr.stellaronstep.app.core.onstep.SlewRate
import fr.stellaronstep.app.data.OnStepRepository
import fr.stellaronstep.app.data.SettingsStore
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val settings = SettingsStore(application)

    var config by mutableStateOf(settings.load())
        private set
    var status by mutableStateOf(MountStatus())
        private set
    var message by mutableStateOf<String?>(null)
        private set
    var busy by mutableStateOf(false)
        private set

    private val client = OnStepTcpClient { config }
    private val repository = OnStepRepository(client)
    private var pollingJob: Job? = null

    fun updateConfig(value: OnStepConnectionConfig) {
        config = value
        settings.save(value)
        message = "Configuration enregistrée"
    }

    fun connectAndPoll() {
        if (pollingJob?.isActive == true) return
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

    fun refreshStatus(silent: Boolean = false) = action(silent) {
        status = repository.readStatus()
        if (!silent) message = "État actualisé"
    }

    fun setRate(rate: SlewRate) = action { repository.setSlewRate(rate); message = "Vitesse: ${rate.label}" }
    fun startMove(direction: SlewDirection) = action { repository.startMove(direction) }
    fun stopMove(direction: SlewDirection) = action { repository.stopMove(direction) }
    fun stopAll() = action { repository.emergencyStop(); message = "Mouvement arrêté" }
    fun tracking(enabled: Boolean) = action { repository.tracking(enabled); refreshStatus(silent = true) }
    fun park() = action { repository.park(); message = "Park demandé" }
    fun unpark() = action { repository.unpark(); message = "Unpark demandé" }
    fun goHome() = action { repository.goHome(); message = "Retour HOME demandé" }
    fun goto(ra: String, dec: String) = action { message = repository.goto(ra, dec) }
    fun startAlignment(stars: Int) = action { message = if (repository.startAlignment(stars)) "Alignement $stars étoile(s) démarré" else "Alignement refusé" }
    fun alignmentStatus() = action { message = "Alignement: ${repository.alignmentStatus()}" }
    fun acceptAlignmentStar() = action { message = if (repository.acceptAlignmentStar()) "Étoile acceptée" else "Correction refusée" }
    fun saveAlignment() = action { message = if (repository.saveAlignment()) "Modèle d'alignement sauvegardé" else "Sauvegarde refusée" }

    private fun action(silent: Boolean = false, block: suspend () -> Unit) {
        viewModelScope.launch {
            if (!silent) busy = true
            try {
                block()
            } catch (e: Exception) {
                status = status.copy(connected = false)
                if (!silent) message = e.message ?: "Erreur OnStep"
            } finally {
                if (!silent) busy = false
            }
        }
    }
}

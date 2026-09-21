package fr.stellaronstep.app.feature.align

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import fr.stellaronstep.app.data.OnStepSession
import fr.stellaronstep.app.data.PhoneLocationProvider
import fr.stellaronstep.app.feature.goto.CatalogQuery
import fr.stellaronstep.app.feature.goto.GotoCatalogRepository
import fr.stellaronstep.app.feature.goto.ObserverLocation
import fr.stellaronstep.app.feature.goto.SkyMath
import fr.stellaronstep.app.feature.goto.SkyTarget
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sqrt

enum class AlignLocationSource(
    val label: String
) {
    AUTO("Auto"),
    PHONE("Téléphone"),
    ONSTEP("OnStepX")
}

data class AlignProcessState(
    val rawProcess: String = "",
    val rawGeneral: String = "",
    val maxStars: Int = 6,
    val currentStar: Int = 0,
    val requestedStars: Int = 0,
    val modelLevel: Int = 0,
    val active: Boolean = false,
    val completed: Boolean = false
) {
    val acceptedStars: Int
        get() = when {
            active -> (currentStar - 1).coerceAtLeast(0)
            completed && requestedStars > 0 -> requestedStars
            else -> modelLevel
        }
}

data class AlignStarResult(
    val index: Int,
    val name: String,
    val direction: String,
    val magnitude: Double?,
    val correctionArcmin: Double?
)

data class AlignModelResult(
    val altitudeCorrectionArcsec: Double?,
    val azimuthCorrectionArcsec: Double?
) {
    val altitudeCorrectionArcmin: Double?
        get() =
            altitudeCorrectionArcsec
                ?.div(60.0)

    val azimuthCorrectionArcmin: Double?
        get() =
            azimuthCorrectionArcsec
                ?.div(60.0)

    val amplitudeArcmin: Double?
        get() {
            val alt =
                altitudeCorrectionArcsec
                    ?: return null

            val az =
                azimuthCorrectionArcsec
                    ?: return null

            return sqrt(
                alt * alt +
                    az * az
            ) / 60.0
        }
}

class AlignViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val client =
        OnStepSession.client(application)

    private val onStep =
        OnStepSession.repository(application)

    private val locationProvider =
        PhoneLocationProvider(application)

    private val catalog =
        GotoCatalogRepository(application)

    var process by mutableStateOf(AlignProcessState())
        private set

    var observer by mutableStateOf<ObserverLocation?>(null)
        private set

    var locationSource by mutableStateOf(AlignLocationSource.AUTO)
        private set

    var candidates by mutableStateOf<List<SkyTarget>>(emptyList())
        private set

    var selectedTarget by mutableStateOf<SkyTarget?>(null)
        private set

    var busy by mutableStateOf(false)
        private set

    var loadingCandidates by mutableStateOf(false)
        private set

    var gotoActive by mutableStateOf(false)
        private set

    var targetReadyForCentering by mutableStateOf(false)
        private set

    var starResults by mutableStateOf<List<AlignStarResult>>(emptyList())
        private set

    var modelResult by mutableStateOf<AlignModelResult?>(null)
        private set

    var activeModelResult by mutableStateOf<AlignModelResult?>(null)
        private set

    var activeModelStars by mutableStateOf(0)
        private set

    var nvSavedThisSession by mutableStateOf(false)
        private set

    var message by mutableStateOf<String?>(null)
        private set

    var error by mutableStateOf<String?>(null)
        private set

    private var usedTargetIds = emptySet<String>()

    init {
        refresh()
        loadCandidates()
    }

    fun refresh() = action {
        process = readProcess()
        refreshActiveModel()
    }

    fun selectLocationSource(
        source: AlignLocationSource
    ) {
        if (locationSource == source) return

        locationSource = source
        selectedTarget = null
        targetReadyForCentering = false
        loadCandidates()
    }

    fun loadCandidates() {
        viewModelScope.launch {
            loadingCandidates = true
            error = null

            try {
                loadCandidatesInternal()
            } catch (exception: Exception) {
                error =
                    exception.message
                        ?: "Impossible de charger les étoiles d'alignement"
            } finally {
                loadingCandidates = false
            }
        }
    }

    fun selectTarget(target: SkyTarget) {
        if (gotoActive) return

        selectedTarget = target
        targetReadyForCentering = false
        error = null
        message =
            "${target.name} sélectionnée — lancer le GOTO"
    }

    fun startAlignment(stars: Int) = action {
        val live = onStep.readStatus()

        when {
            live.parked -> {
                error = "Alignement impossible : monture parquée"
                return@action
            }

            live.slewing -> {
                error = "Alignement impossible pendant un mouvement"
                return@action
            }

            !live.atHome -> {
                error =
                    "Placer d'abord la monture en position HOME polaire avant de démarrer l'alignement"
                return@action
            }
        }

        val availableMax =
            process.maxStars
                .takeIf { it > 0 }
                ?.coerceAtMost(6)
                ?: 6

        val safeStars =
            stars.coerceIn(1, availableMax)

        if (!onStep.startAlignment(safeStars)) {
            error = "OnStepX a refusé le démarrage de l'alignement"
            return@action
        }

        delay(200)

        process = readProcess()
        usedTargetIds = emptySet()
        starResults = emptyList()
        modelResult = null
        activeModelResult = null
        activeModelStars = 0
        nvSavedThisSession = false
        selectedTarget = null
        targetReadyForCentering = false

        loadCandidatesInternal()

        message =
            "Alignement $safeStars étoile(s) démarré — choisir l'étoile 1"
    }

    fun gotoSelected() {
        val target = selectedTarget

        if (target == null) {
            error = "Choisir d'abord une étoile"
            return
        }

        if (!process.active) {
            error = "Aucun alignement en cours"
            return
        }

        if (gotoActive) return

        viewModelScope.launch {
            error = null
            targetReadyForCentering = false

            try {
                val live = onStep.readStatus()

                if (live.parked) {
                    throw IllegalStateException(
                        "Monture parquée — faire UNPARK"
                    )
                }

                if (live.slewing) {
                    throw IllegalStateException(
                        "Un mouvement est déjà en cours"
                    )
                }

                gotoActive = true
                message = "GOTO vers ${target.name}..."

                val result =
                    onStep.goto(
                        SkyMath.formatRa(target.raHours),
                        SkyMath.formatDec(target.decDeg)
                    )

                if (!result.startsWith("GOTO accepte")) {
                    throw IllegalStateException(result)
                }

                repeat(240) { index ->
                    delay(500)

                    val status =
                        onStep.readStatus()

                    if (index >= 1 && !status.slewing) {
                        gotoActive = false
                        targetReadyForCentering = true
                        message =
                            "${target.name} atteinte — centrer directement avec les commandes ci-dessous puis valider"
                        return@launch
                    }
                }

                gotoActive = false
                error =
                    "GOTO toujours actif après 120 s — vérifier la monture"
            } catch (exception: Exception) {
                gotoActive = false
                targetReadyForCentering = false
                error =
                    exception.message
                        ?: "Erreur pendant le GOTO d'alignement"
            }
        }
    }

    fun stopGoto() {
        viewModelScope.launch {
            runCatching {
                onStep.emergencyStop()
            }

            gotoActive = false
            targetReadyForCentering = false
            message = "GOTO arrêté"
        }
    }

    fun acceptCurrentStar() = action {
        val target = selectedTarget

        if (!process.active) {
            error = "Aucun alignement en cours"
            return@action
        }

        if (target == null) {
            error = "Aucune étoile sélectionnée"
            return@action
        }

        if (!targetReadyForCentering) {
            error =
                "Lancer d'abord le GOTO vers l'étoile et attendre la fin du pointage"
            return@action
        }

        val live =
            onStep.readStatus()

        if (live.slewing) {
            error =
                "Attendre la fin du mouvement avant de valider le centrage"
            return@action
        }

        val acceptedIndex =
            process.currentStar

        val currentRa =
            SkyMath.parseRaHours(
                live.ra
            )

        val currentDec =
            SkyMath.parseDecDeg(
                live.dec
            )

        val correctionArcmin =
            if (
                currentRa != null &&
                currentDec != null
            ) {
                SkyMath.angularSeparationDeg(
                    currentRa,
                    currentDec,
                    target.raHours,
                    target.decDeg
                ) * 60.0
            } else {
                null
            }

        if (!onStep.acceptAlignmentStar()) {
            error =
                "OnStepX a refusé la validation de l'étoile $acceptedIndex"
            return@action
        }

        starResults =
            starResults +
                AlignStarResult(
                    index = acceptedIndex,
                    name = target.name,
                    direction = target.azimuthDirection,
                    magnitude = target.magnitude,
                    correctionArcmin = correctionArcmin
                )

        usedTargetIds =
            usedTargetIds + target.id

        delay(300)

        process = readProcess()
        selectedTarget = null
        targetReadyForCentering = false

        if (process.active) {
            loadCandidatesInternal()

            message =
                "Étoile $acceptedIndex validée — choisir l'étoile ${process.currentStar}"
        } else {
            modelResult =
                readModelResult()

            activeModelResult =
                modelResult

            activeModelStars =
                readModelStarCount()

            message =
                "Alignement terminé — vérifier les écarts puis sauvegarder le modèle"
        }
    }

    fun saveModel() = action {
        process = readProcess()

        if (process.active) {
            error =
                "Terminer toutes les étoiles avant de sauvegarder le modèle"
            return@action
        }

        if (!process.completed && process.modelLevel <= 0) {
            error =
                "Aucun modèle d'alignement valide à sauvegarder"
            return@action
        }

        if (!onStep.saveAlignment()) {
            error =
                "OnStepX a refusé la sauvegarde du modèle"
            return@action
        }

        nvSavedThisSession = true

        message =
            "Modèle d'alignement sauvegardé en mémoire NV OnStepX"
    }

    private suspend fun refreshActiveModel() {
        activeModelResult =
            readModelResult()

        activeModelStars =
            readModelStarCount()
    }

    private suspend fun readModelStarCount(): Int =
        runCatching {
            client.queryHash(
                ":GX09#"
            ).trim().toIntOrNull()
                ?: 0
        }.getOrDefault(0)

    private suspend fun readModelResult(): AlignModelResult {
        fun parseArcsec(
            value: String
        ): Double? =
            value
                .trim()
                .toDoubleOrNull()

        val altitude =
            runCatching {
                parseArcsec(
                    client.queryHash(
                        ":GX02#"
                    )
                )
            }.getOrNull()

        val azimuth =
            runCatching {
                parseArcsec(
                    client.queryHash(
                        ":GX03#"
                    )
                )
            }.getOrNull()

        return AlignModelResult(
            altitudeCorrectionArcsec =
                altitude,
            azimuthCorrectionArcsec =
                azimuth
        )
    }

    private suspend fun readProcess(): AlignProcessState {
        val rawProcess =
            onStep.alignmentStatus().trim()

        val rawGeneral =
            runCatching {
                client.queryHash(":GW#").trim()
            }.getOrDefault("")

        val maxStars =
            rawProcess.getOrNull(0)
                ?.digitToIntOrNull()
                ?: 6

        val currentStar =
            rawProcess.getOrNull(1)
                ?.digitToIntOrNull()
                ?: 0

        val requestedStars =
            rawProcess.getOrNull(2)
                ?.digitToIntOrNull()
                ?: 0

        val modelLevel =
            rawGeneral.getOrNull(2)
                ?.digitToIntOrNull()
                ?: 0

        val active =
            requestedStars > 0 &&
                currentStar in 1..requestedStars

        val completed =
            !active &&
                (
                    modelLevel > 0 ||
                        (
                            requestedStars > 0 &&
                                currentStar > requestedStars
                            )
                    )

        return AlignProcessState(
            rawProcess = rawProcess,
            rawGeneral = rawGeneral,
            maxStars = maxStars,
            currentStar = currentStar,
            requestedStars = requestedStars,
            modelLevel = modelLevel,
            active = active,
            completed = completed
        )
    }

    private suspend fun loadCandidatesInternal() {
        val resolvedObserver =
            resolveObserver()

        observer = resolvedObserver

        if (resolvedObserver == null) {
            candidates = emptyList()
            error =
                when (locationSource) {
                    AlignLocationSource.PHONE ->
                        "Position téléphone indisponible — autoriser la localisation ou choisir OnStepX"

                    AlignLocationSource.ONSTEP ->
                        "Position OnStepX indisponible — initialiser latitude/longitude dans CONFIG"

                    AlignLocationSource.AUTO ->
                        "Position observateur indisponible — vérifier le téléphone ou OnStepX dans CONFIG"
                }
            return
        }

        val sectors =
            listOf(
                "N",
                "NE",
                "E",
                "SE",
                "S",
                "SW",
                "W",
                "NW"
            )

        val result =
            catalog.search(
                resolvedObserver,
                CatalogQuery(
                    category = "star",
                    minAltitudeDeg = 25.0,
                    maxMagnitude = 2.99,
                    sort = "magnitude",
                    order = "asc",
                    limit = 100
                )
            ).objects.filter {
                it.magnitude != null &&
                    it.magnitude < 3.0 &&
                    abs(it.decDeg) <= 75.0 &&
                    it.id !in usedTargetIds
            }

        /*
         * ALIGN : sélection locale par secteur du ciel.
         * Pour chaque direction (N, NE, E, SE, S, SW, W, NW),
         * conserver uniquement les étoiles les plus brillantes.
         * La magnitude est strictement < 3 et l'altitude >= 25°.
         */
        candidates =
            sectors.flatMap { sector ->
                result
                    .filter {
                        it.azimuthDirection == sector
                    }
                    .sortedWith(
                        compareBy<SkyTarget> {
                            it.magnitude
                        }.thenByDescending {
                            it.altitudeDeg
                        }
                    )
                    .take(3)
            }

        if (candidates.isEmpty()) {
            error =
                "Aucune étoile adaptée n'est actuellement assez haute dans le catalogue"
        }
    }

    private suspend fun resolveObserver(): ObserverLocation? {
        suspend fun fromPhone(): ObserverLocation? {
            if (!locationProvider.hasLocationPermission()) {
                return null
            }

            val phone =
                runCatching {
                    locationProvider.currentLocation()
                }.getOrNull()
                    ?: return null

            return ObserverLocation(
                latitude = phone.latitude,
                longitude = phone.longitude,
                source = "Téléphone"
            )
        }

        suspend fun fromOnStep(): ObserverLocation? {
            val diagnostics =
                runCatching {
                    onStep.readDiagnostics()
                }.getOrNull()
                    ?: return null

            val latitude =
                SkyMath.parseDms(diagnostics.latitude)
                    ?: return null

            val onStepLongitude =
                SkyMath.parseDms(diagnostics.longitudeOnStep)
                    ?: return null

            return ObserverLocation(
                latitude = latitude,
                longitude = -onStepLongitude,
                source = "OnStepX"
            )
        }

        return when (locationSource) {
            AlignLocationSource.PHONE ->
                fromPhone()

            AlignLocationSource.ONSTEP ->
                fromOnStep()

            AlignLocationSource.AUTO ->
                fromPhone()
                    ?: fromOnStep()
        }
    }

    private fun action(
        block: suspend () -> Unit
    ) {
        viewModelScope.launch {
            busy = true
            error = null

            try {
                block()
            } catch (exception: Exception) {
                error =
                    exception.message
                        ?: "Erreur OnStepX"
            } finally {
                busy = false
            }
        }
    }
}

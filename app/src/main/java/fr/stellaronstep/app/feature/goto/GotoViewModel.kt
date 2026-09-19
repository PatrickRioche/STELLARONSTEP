package fr.stellaronstep.app.feature.goto

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import fr.stellaronstep.app.core.onstep.OnStepTcpClient
import fr.stellaronstep.app.data.OnStepRepository
import fr.stellaronstep.app.data.PhoneLocationProvider
import fr.stellaronstep.app.data.SettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max

class GotoViewModel(
    application: Application
) : AndroidViewModel(application) {
    private val settings = SettingsStore(application)
    private val client = OnStepTcpClient { settings.load() }
    private val onStep = OnStepRepository(client)
    private val locationProvider = PhoneLocationProvider(application)
    private val catalog = GotoCatalogRepository(application)
    private val preferences =
        application.getSharedPreferences(
            "stellaronstep_goto",
            0
        )

    var observer by mutableStateOf<ObserverLocation?>(null)
        private set

    var catalogResult by mutableStateOf<CatalogResult?>(null)
        private set

    var solarTargets by mutableStateOf<List<SkyTarget>>(emptyList())
        private set

    var selectedTarget by mutableStateOf<SkyTarget?>(null)
        private set

    var loading by mutableStateOf(false)
        private set

    var error by mutableStateOf<String?>(null)
        private set

    var gotoState by mutableStateOf(GotoMotionState())
        private set

    init {
        restoreSelected()
        refreshObserver()
    }

    fun refreshObserver() {
        viewModelScope.launch {
            loading = true
            error = null
            try {
                val phone =
                    if (locationProvider.hasLocationPermission()) {
                        locationProvider.currentLocation()
                    } else {
                        null
                    }

                observer =
                    if (phone != null) {
                        ObserverLocation(
                            latitude = phone.latitude,
                            longitude = phone.longitude,
                            source = "Téléphone"
                        )
                    } else {
                        val diagnostics = onStep.readDiagnostics()
                        val latitude = SkyMath.parseDms(diagnostics.latitude)
                        val onStepLongitude =
                            SkyMath.parseDms(diagnostics.longitudeOnStep)

                        if (latitude != null && onStepLongitude != null) {
                            ObserverLocation(
                                latitude = latitude,
                                longitude = -onStepLongitude,
                                source = "OnStepX"
                            )
                        } else {
                            null
                        }
                    }

                if (observer == null) {
                    error =
                        "Position observateur indisponible. Utiliser CONFIG > INITIALISER DEPUIS LE TELEPHONE."
                } else {
                    refreshSelectedHorizontal()
                }
            } catch (exception: Exception) {
                error = exception.message ?: "Position indisponible"
            } finally {
                loading = false
            }
        }
    }

    fun loadCatalog(query: CatalogQuery) {
        val location = observer
        if (location == null) {
            error = "Position observateur requise"
            return
        }

        viewModelScope.launch {
            loading = true
            error = null
            try {
                catalogResult = catalog.search(location, query)
            } catch (exception: Exception) {
                error = exception.message ?: "Erreur catalogue"
            } finally {
                loading = false
            }
        }
    }

    fun loadSolarSystem() {
        val location = observer
        if (location == null) {
            error = "Position observateur requise"
            return
        }

        viewModelScope.launch {
            loading = true
            error = null
            try {
                solarTargets =
                    withContext(Dispatchers.Default) {
                        SolarSystemProvider.targets(location)
                    }
            } catch (exception: Exception) {
                error = exception.message ?: "Erreur système solaire"
            } finally {
                loading = false
            }
        }
    }

    fun select(target: SkyTarget) {
        selectedTarget = target
        error = null

        preferences.edit()
            .putString("id", target.id)
            .putString("name", target.name)
            .putString("reference", target.reference)
            .putString("objectType", target.objectType)
            .putString("objectTypeLabel", target.objectTypeLabel)
            .putString("constellation", target.constellation)
            .putString("raHours", target.raHours.toString())
            .putString("decDeg", target.decDeg.toString())
            .putString("trackingMode", target.trackingMode.name)
            .putBoolean("solarWarning", target.solarWarning)
            .putString("symbol", target.symbol)
            .apply()
    }

    fun gotoTarget(target: SkyTarget) {
        if (gotoState.active) return

        if (!target.aboveHorizon) {
            error = "Cible sous l'horizon : GOTO indisponible"
            return
        }

        viewModelScope.launch {
            error = null
            gotoState =
                GotoMotionState(
                    active = true,
                    targetName = target.name,
                    status = "starting",
                    message = "Pointage en cours...",
                    progress = 0.0,
                    targetRaHours = target.raHours,
                    targetDecDeg = target.decDeg,
                    trackingLabel = target.trackingMode.label
                )

            try {
                val before = onStep.readStatus()

                if (before.parked) {
                    throw IllegalStateException(
                        "Monture parquée - faire UNPARK"
                    )
                }

                // Choix du taux adapté à la cible.
                client.send(target.trackingMode.onStepCommand)
                delay(100)

                if (!before.tracking) {
                    val accepted = client.queryByte(":Te#") == "1"
                    if (!accepted) {
                        throw IllegalStateException(
                            "Impossible d'activer le suivi ${target.trackingMode.label}"
                        )
                    }
                    delay(350)
                }

                val startRa = SkyMath.parseRaHours(before.ra)
                val startDec = SkyMath.parseDecDeg(before.dec)
                val startDistance =
                    if (startRa != null && startDec != null) {
                        max(
                            0.001,
                            SkyMath.angularSeparationDeg(
                                startRa,
                                startDec,
                                target.raHours,
                                target.decDeg
                            )
                        )
                    } else {
                        1.0
                    }

                val result =
                    onStep.goto(
                        SkyMath.formatRa(target.raHours),
                        SkyMath.formatDec(target.decDeg)
                    )

                if (!result.startsWith("GOTO accepte")) {
                    throw IllegalStateException(result)
                }

                // Le repository peut réappliquer le sidéral si le statut tracking
                // n'était pas encore visible : on restaure toujours le taux cible.
                client.send(target.trackingMode.onStepCommand)

                repeat(240) { index ->
                    delay(500)
                    val live = onStep.readStatus()
                    val currentRa = SkyMath.parseRaHours(live.ra)
                    val currentDec = SkyMath.parseDecDeg(live.dec)

                    val progress =
                        if (currentRa != null && currentDec != null) {
                            val remaining =
                                SkyMath.angularSeparationDeg(
                                    currentRa,
                                    currentDec,
                                    target.raHours,
                                    target.decDeg
                                )
                            (1.0 - remaining / startDistance)
                                .coerceIn(0.0, 0.99)
                        } else {
                            null
                        }

                    val done = index >= 1 && !live.slewing

                    gotoState =
                        gotoState.copy(
                            active = !done,
                            status = if (done) "tracking" else "slewing",
                            message =
                                if (done) {
                                    "Position cible atteinte"
                                } else {
                                    "Pointage en cours..."
                                },
                            progress = if (done) 1.0 else progress,
                            currentRaHours = currentRa,
                            currentDecDeg = currentDec
                        )

                    if (done) return@launch
                }

                gotoState =
                    gotoState.copy(
                        active = false,
                        message = "Pointage toujours en cours après 120 s"
                    )
            } catch (exception: Exception) {
                gotoState =
                    gotoState.copy(
                        active = false,
                        message = null,
                        error = exception.message ?: "Erreur GOTO"
                    )
            }
        }
    }

    fun gotoCoordinates(ra: String, dec: String) {
        val raHours = SkyMath.parseRaHours(ra)
        val decDeg = SkyMath.parseDecDeg(dec)

        if (raHours == null || decDeg == null) {
            error = "Coordonnées invalides : AD HH:MM:SS / Dec +DD*MM:SS"
            return
        }

        val location = observer
        val horizontal =
            if (location != null) {
                SkyMath.horizontal(
                    raHours,
                    decDeg,
                    location.latitude,
                    location.longitude
                )
            } else {
                SkyMath.Horizontal(
                    altitudeDeg = 90.0,
                    azimuthDeg = 0.0,
                    direction = "N"
                )
            }

        val target =
            SkyTarget(
                id = "manual:$ra:$dec",
                name = "Coordonnées manuelles",
                reference = null,
                objectType = "manual",
                objectTypeLabel = "Coordonnées",
                constellation = null,
                raHours = raHours,
                decDeg = decDeg,
                magnitude = null,
                magnitudeBand = null,
                majorAxisArcmin = null,
                minorAxisArcmin = null,
                aliases = null,
                altitudeDeg = horizontal.altitudeDeg,
                azimuthDeg = horizontal.azimuthDeg,
                azimuthDirection = horizontal.direction,
                visible = horizontal.altitudeDeg > 0.0,
                aboveHorizon = horizontal.altitudeDeg > 0.0
            )

        select(target)
        gotoTarget(target)
    }

    fun stopGoto() {
        viewModelScope.launch {
            runCatching { onStep.emergencyStop() }
            gotoState =
                gotoState.copy(
                    active = false,
                    status = "stopped",
                    message = "GOTO arrêté"
                )
        }
    }

    private fun restoreSelected() {
        val name = preferences.getString("name", null) ?: return
        val ra =
            preferences.getString("raHours", null)
                ?.toDoubleOrNull() ?: return
        val dec =
            preferences.getString("decDeg", null)
                ?.toDoubleOrNull() ?: return

        val tracking =
            runCatching {
                TargetTrackingMode.valueOf(
                    preferences.getString(
                        "trackingMode",
                        TargetTrackingMode.SIDEREAL.name
                    )!!
                )
            }.getOrDefault(TargetTrackingMode.SIDEREAL)

        selectedTarget =
            SkyTarget(
                id = preferences.getString("id", "saved")!!,
                name = name,
                reference = preferences.getString("reference", null),
                objectType = preferences.getString("objectType", "unknown")!!,
                objectTypeLabel =
                    preferences.getString("objectTypeLabel", "Objet")!!,
                constellation = preferences.getString("constellation", null),
                raHours = ra,
                decDeg = dec,
                magnitude = null,
                magnitudeBand = null,
                majorAxisArcmin = null,
                minorAxisArcmin = null,
                aliases = null,
                altitudeDeg = 0.0,
                azimuthDeg = 0.0,
                azimuthDirection = "N",
                visible = false,
                aboveHorizon = false,
                solarWarning = preferences.getBoolean("solarWarning", false),
                symbol = preferences.getString("symbol", null),
                trackingMode = tracking
            )
    }

    private fun refreshSelectedHorizontal() {
        val target = selectedTarget ?: return
        val location = observer ?: return
        val horizontal =
            SkyMath.horizontal(
                target.raHours,
                target.decDeg,
                location.latitude,
                location.longitude
            )

        selectedTarget =
            target.copy(
                altitudeDeg = horizontal.altitudeDeg,
                azimuthDeg = horizontal.azimuthDeg,
                azimuthDirection = horizontal.direction,
                visible = horizontal.altitudeDeg > 0.0,
                aboveHorizon = horizontal.altitudeDeg > 0.0
            )
    }
}

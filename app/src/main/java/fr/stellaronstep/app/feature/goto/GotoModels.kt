package fr.stellaronstep.app.feature.goto

data class ObserverLocation(
    val latitude: Double,
    val longitude: Double,
    val source: String
)

enum class TargetTrackingMode(
    val label: String,
    val onStepCommand: String
) {
    SIDEREAL("sidéral", ":TQ#"),
    LUNAR("lunaire", ":TL#"),
    SOLAR("solaire", ":TS#")
}

data class SkyTarget(
    val id: String,
    val name: String,
    val reference: String?,
    val objectType: String,
    val objectTypeLabel: String,
    val constellation: String?,
    val raHours: Double,
    val decDeg: Double,
    val magnitude: Double?,
    val magnitudeBand: String?,
    val majorAxisArcmin: Double?,
    val minorAxisArcmin: Double?,
    val aliases: String?,
    val altitudeDeg: Double,
    val azimuthDeg: Double,
    val azimuthDirection: String,
    val visible: Boolean,
    val aboveHorizon: Boolean,
    val solarWarning: Boolean = false,
    val symbol: String? = null,
    val trackingMode: TargetTrackingMode = TargetTrackingMode.SIDEREAL,
    val source: String = ""
)

data class CatalogQuery(
    val category: String = "all",
    val text: String = "",
    val minAltitudeDeg: Double = 15.0,
    val direction: String? = null,
    val constellations: Set<String> = emptySet(),
    val maxMagnitude: Double? = null,
    val sort: String = "magnitude",
    val order: String = "asc",
    val offset: Int = 0,
    val limit: Int = 30
)

data class CatalogResult(
    val query: CatalogQuery,
    val explicitSearch: Boolean,
    val matchedCount: Int,
    val visibleCount: Int,
    val returnedCount: Int,
    val objects: List<SkyTarget>
)

data class GotoMotionState(
    val active: Boolean = false,
    val targetName: String? = null,
    val status: String? = null,
    val message: String? = null,
    val error: String? = null,
    val progress: Double? = null,
    val currentRaHours: Double? = null,
    val currentDecDeg: Double? = null,
    val targetRaHours: Double? = null,
    val targetDecDeg: Double? = null,
    val trackingLabel: String? = null
)

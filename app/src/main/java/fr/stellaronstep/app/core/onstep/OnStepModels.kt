package fr.stellaronstep.app.core.onstep

data class OnStepConnectionConfig(
    val host: String = "192.168.0.1",
    val port: Int = 9999,
    val timeoutMs: Int = 1800
)

data class MountStatus(
    val raw: String = "",
    val connected: Boolean = false,
    val tracking: Boolean = false,
    val slewing: Boolean = false,
    val parked: Boolean = false,
    val parking: Boolean = false,
    val parkFailed: Boolean = false,
    val atHome: Boolean = false,
    val pierSide: String? = null,
    val ra: String? = null,
    val dec: String? = null
)

data class OnStepDiagnostics(
    val latitude: String = "--",
    val longitudeOnStep: String = "--",
    val date: String = "--",
    val localTime: String = "--",
    val utcOffset: String = "--",
    val siderealTime: String = "--",
    val dateTimeReady: String = "--",
    val lastError: String = "--",
    val mountType: String = "--",
    val axis1Deg: String = "--",
    val axis2Deg: String = "--",
    val altitude: String = "--",
    val azimuth: String = "--",
    val horizonLimit: String = "--",
    val overheadLimit: String = "--",
    val pierSide: String = "--",
    val rawStatus: String = "--",
    val eastPastMeridianMin: String = "--",
    val westPastMeridianMin: String = "--",
    val axis1MinDeg: String = "--",
    val axis1MaxDeg: String = "--",
    val axis2MinDeg: String = "--",
    val axis2MaxDeg: String = "--"
)

enum class SlewDirection(
    val label: String,
    val startCommand: String,
    val stopCommand: String
) {
    NORTH("Nord", ":Mn#", ":Qn#"),
    SOUTH("Sud", ":Ms#", ":Qs#"),
    EAST("Est", ":Me#", ":Qe#"),
    WEST("Ouest", ":Mw#", ":Qw#")
}

enum class SlewRate(
    val label: String,
    val command: String
) {
    GUIDE("Guidage", ":RG#"),
    CENTER("Centrage", ":RC#"),
    MOVE("Deplacement", ":RM#"),
    SLEW("Rapide 50 %", ":RS#"),
    MAX("MAX 100 %", ":R9#")
}
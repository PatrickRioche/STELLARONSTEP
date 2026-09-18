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
    GUIDE("Guide", ":RG#"),
    CENTER("Center", ":RC#"),
    MOVE("Move", ":RM#"),
    SLEW("Slew", ":RS#")
}
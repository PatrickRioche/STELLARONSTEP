package fr.stellaronstep.app.core.onstep

object OnStepStatusParser {
    fun parse(rawReply: String, ra: String? = null, dec: String? = null): MountStatus {
        val raw = rawReply.trim().trimEnd('#')
        val notTracking = raw.contains('n')
        val noGoto = raw.contains('N')

        return MountStatus(
            raw = raw,
            connected = true,
            tracking = !notTracking,
            slewing = !noGoto,
            parked = raw.contains('P'),
            parking = raw.contains('I'),
            parkFailed = raw.contains('F'),
            atHome = raw.contains('H'),
            pierSide = when {
                raw.contains('T') -> "East"
                raw.contains('W') -> "West"
                else -> null
            },
            ra = ra?.trim()?.trimEnd('#'),
            dec = dec?.trim()?.trimEnd('#')
        )
    }
}

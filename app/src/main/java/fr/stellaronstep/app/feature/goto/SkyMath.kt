package fr.stellaronstep.app.feature.goto

import java.text.Normalizer
import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.tan

object SkyMath {
    data class Horizontal(
        val altitudeDeg: Double,
        val azimuthDeg: Double,
        val direction: String
    )

    fun horizontal(
        raHours: Double,
        decDeg: Double,
        latitudeDeg: Double,
        longitudeDeg: Double,
        epochMillis: Long = System.currentTimeMillis()
    ): Horizontal {
        val jd = epochMillis.toDouble() / 86_400_000.0 + 2_440_587.5
        val t = (jd - 2_451_545.0) / 36_525.0
        val gmst =
            280.46061837 +
                360.98564736629 * (jd - 2_451_545.0) +
                0.000387933 * t * t -
                (t * t * t) / 38_710_000.0

        val lst = normalizeDegrees(gmst + longitudeDeg)
        val hourAngle = normalizeSignedDegrees(lst - raHours * 15.0)
        val ha = Math.toRadians(hourAngle)
        val dec = Math.toRadians(decDeg)
        val lat = Math.toRadians(latitudeDeg)

        val sinAlt =
            (sin(dec) * sin(lat) + cos(dec) * cos(lat) * cos(ha))
                .coerceIn(-1.0, 1.0)

        val altitude = asin(sinAlt)
        val azimuth =
            atan2(
                -sin(ha),
                tan(dec) * cos(lat) - sin(lat) * cos(ha)
            )

        val altDeg = Math.toDegrees(altitude)
        val azDeg = normalizeDegrees(Math.toDegrees(azimuth))

        return Horizontal(
            altitudeDeg = altDeg,
            azimuthDeg = azDeg,
            direction = azimuthDirection(azDeg)
        )
    }

    fun azimuthDirection(azimuthDeg: Double): String {
        val directions = listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
        val index = (((azimuthDeg + 22.5) / 45.0).toInt()) % 8
        return directions[index]
    }

    fun angularSeparationDeg(
        ra1Hours: Double,
        dec1Deg: Double,
        ra2Hours: Double,
        dec2Deg: Double
    ): Double {
        val ra1 = ra1Hours * 15.0 * PI / 180.0
        val ra2 = ra2Hours * 15.0 * PI / 180.0
        val dec1 = dec1Deg * PI / 180.0
        val dec2 = dec2Deg * PI / 180.0
        val value =
            (sin(dec1) * sin(dec2) +
                cos(dec1) * cos(dec2) * cos(ra1 - ra2))
                .coerceIn(-1.0, 1.0)
        return Math.toDegrees(acos(value))
    }

    fun formatRa(raHours: Double): String {
        val normalized = ((raHours % 24.0) + 24.0) % 24.0
        val total = (normalized * 3600.0).roundToInt() % 86_400
        val h = total / 3600
        val m = (total % 3600) / 60
        val s = total % 60
        return "%02d:%02d:%02d".format(h, m, s)
    }

    fun formatDec(decDeg: Double): String {
        val safe = decDeg.coerceIn(-90.0, 90.0)
        val sign = if (safe < 0.0) "-" else "+"
        val total = (kotlin.math.abs(safe) * 3600.0).roundToInt()
        val d = total / 3600
        val m = (total % 3600) / 60
        val s = total % 60
        return "%s%02d*%02d:%02d".format(sign, d, m, s)
    }

    fun parseRaHours(value: String?): Double? {
        if (value.isNullOrBlank()) return null
        val match =
            Regex("""^(\d{1,2}):(\d{1,2}):(\d{1,2}(?:\.\d+)?)$""")
                .matchEntire(value.trim().trimEnd('#')) ?: return null
        val h = match.groupValues[1].toDoubleOrNull() ?: return null
        val m = match.groupValues[2].toDoubleOrNull() ?: return null
        val s = match.groupValues[3].toDoubleOrNull() ?: return null
        if (h < 0.0 || h >= 24.0 || m < 0.0 || m >= 60.0 || s < 0.0 || s >= 60.0) return null
        return h + m / 60.0 + s / 3600.0
    }

    fun parseDecDeg(value: String?): Double? {
        if (value.isNullOrBlank()) return null
        val raw = value.trim().trimEnd('#').replace("°", "*")
        val match =
            Regex("""^([+-]?)(\d{1,2})[*:](\d{1,2})[:'](\d{1,2}(?:\.\d+)?)$""")
                .matchEntire(raw) ?: return null
        val sign = if (match.groupValues[1] == "-") -1.0 else 1.0
        val d = match.groupValues[2].toDoubleOrNull() ?: return null
        val m = match.groupValues[3].toDoubleOrNull() ?: return null
        val s = match.groupValues[4].toDoubleOrNull() ?: return null
        if (d > 90.0 || m >= 60.0 || s >= 60.0) return null
        return sign * (d + m / 60.0 + s / 3600.0)
    }

    fun parseDms(value: String?): Double? {
        if (value.isNullOrBlank()) return null
        val raw =
            value.trim()
                .replace("°", "*")
                .replace("'", ":")
                .trimEnd('#')
        val match =
            Regex("""^([+-]?)(\d{1,3})[*:](\d{1,2})(?::(\d{1,2}(?:\.\d+)?))?$""")
                .matchEntire(raw) ?: return null
        val sign = if (match.groupValues[1] == "-") -1.0 else 1.0
        val d = match.groupValues[2].toDoubleOrNull() ?: return null
        val m = match.groupValues[3].toDoubleOrNull() ?: 0.0
        val s = match.groupValues[4].toDoubleOrNull() ?: 0.0
        return sign * (d + m / 60.0 + s / 3600.0)
    }

    fun foldText(value: String): String {
        val normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
        return normalized
            .replace(Regex("""\p{Mn}+"""), "")
            .lowercase()
            .trim()
    }

    private fun normalizeDegrees(value: Double): Double {
        var v = value % 360.0
        if (v < 0.0) v += 360.0
        return v
    }

    private fun normalizeSignedDegrees(value: Double): Double {
        var v = normalizeDegrees(value)
        if (v >= 180.0) v -= 360.0
        return v
    }
}

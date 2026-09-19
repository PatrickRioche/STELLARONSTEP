package fr.stellaronstep.app.feature.goto

import io.github.cosinekitty.astronomy.Aberration
import io.github.cosinekitty.astronomy.Body
import io.github.cosinekitty.astronomy.EquatorEpoch
import io.github.cosinekitty.astronomy.Observer
import io.github.cosinekitty.astronomy.Refraction
import io.github.cosinekitty.astronomy.Time
import io.github.cosinekitty.astronomy.equator
import io.github.cosinekitty.astronomy.horizon

object SolarSystemProvider {
    private data class Definition(
        val body: Body,
        val name: String,
        val symbol: String,
        val type: String,
        val label: String,
        val warning: Boolean,
        val tracking: TargetTrackingMode
    )

    private val definitions =
        listOf(
            Definition(Body.Sun, "Soleil", "☉", "sun", "Soleil", true, TargetTrackingMode.SOLAR),
            Definition(Body.Moon, "Lune", "☾", "moon", "Lune", false, TargetTrackingMode.LUNAR),
            Definition(Body.Mercury, "Mercure", "☿", "planet", "Planète", false, TargetTrackingMode.SIDEREAL),
            Definition(Body.Venus, "Vénus", "♀", "planet", "Planète", false, TargetTrackingMode.SIDEREAL),
            Definition(Body.Mars, "Mars", "♂", "planet", "Planète", false, TargetTrackingMode.SIDEREAL),
            Definition(Body.Jupiter, "Jupiter", "♃", "planet", "Planète", false, TargetTrackingMode.SIDEREAL),
            Definition(Body.Saturn, "Saturne", "♄", "planet", "Planète", false, TargetTrackingMode.SIDEREAL),
            Definition(Body.Uranus, "Uranus", "♅", "planet", "Planète", false, TargetTrackingMode.SIDEREAL),
            Definition(Body.Neptune, "Neptune", "♆", "planet", "Planète", false, TargetTrackingMode.SIDEREAL)
        )

    fun targets(observerLocation: ObserverLocation): List<SkyTarget> {
        val time =
            Time.fromMillisecondsSince1970(
                System.currentTimeMillis()
            )
        val observer =
            Observer(
                observerLocation.latitude,
                observerLocation.longitude,
                0.0
            )

        return definitions.map { definition ->
            // StellarPilot uses apparent/topocentric coordinates for moving bodies.
            val equatorial =
                equator(
                    definition.body,
                    time,
                    observer,
                    EquatorEpoch.OfDate,
                    Aberration.Corrected
                )
            val horizontal =
                horizon(
                    time,
                    observer,
                    equatorial.ra,
                    equatorial.dec,
                    Refraction.Normal
                )

            SkyTarget(
                id = "solar:${definition.body}",
                name = definition.name,
                reference = definition.name,
                objectType = definition.type,
                objectTypeLabel = definition.label,
                constellation = null,
                raHours = equatorial.ra,
                decDeg = equatorial.dec,
                magnitude = null,
                magnitudeBand = null,
                majorAxisArcmin = null,
                minorAxisArcmin = null,
                aliases = null,
                altitudeDeg = horizontal.altitude,
                azimuthDeg = horizontal.azimuth,
                azimuthDirection = SkyMath.azimuthDirection(horizontal.azimuth),
                visible = horizontal.altitude > 0.0,
                aboveHorizon = horizontal.altitude > 0.0,
                solarWarning = definition.warning,
                symbol = definition.symbol,
                trackingMode = definition.tracking,
                source = "Astronomy Engine v2.1.19"
            )
        }.sortedByDescending { it.altitudeDeg }
    }
}

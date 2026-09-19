package fr.stellaronstep.app.feature.goto

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SkyMathTest {
    @Test
    fun formatsRa() {
        assertEquals("12:30:00", SkyMath.formatRa(12.5))
    }

    @Test
    fun formatsDec() {
        assertEquals("-30*15:00", SkyMath.formatDec(-30.25))
    }

    @Test
    fun parsesCoordinates() {
        assertEquals(
            12.5,
            SkyMath.parseRaHours("12:30:00")!!,
            1e-9
        )
        assertEquals(
            -30.25,
            SkyMath.parseDecDeg("-30*15:00")!!,
            1e-9
        )
    }

    @Test
    fun directionSectors() {
        assertEquals("N", SkyMath.azimuthDirection(0.0))
        assertEquals("E", SkyMath.azimuthDirection(90.0))
        assertTrue(
            SkyMath.angularSeparationDeg(
                1.0, 10.0, 1.0, 10.0
            ) < 1e-6
        )
    }
}

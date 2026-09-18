package fr.onsteppilot.app.core.onstep

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OnStepStatusParserTest {
    @Test
    fun parkedNotTracking() {
        val status = OnStepStatusParser.parse("nNPEW260#")
        assertTrue(status.parked)
        assertFalse(status.tracking)
        assertFalse(status.slewing)
    }

    @Test
    fun unparkedTracking() {
        val status = OnStepStatusParser.parse("NpeEW260#")
        assertFalse(status.parked)
        assertTrue(status.tracking)
        assertFalse(status.slewing)
    }
}

package com.yeter.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CoreLogicTest {

    @Test fun isNewer_comparesNumbersNotText() {
        assertTrue(Updater.isNewer("v1.10.0", "v1.9.9"))
        assertTrue(Updater.isNewer("1.1.6", "v1.1.5"))
        assertFalse(Updater.isNewer("v1.1.5", "v1.1.5"))
        assertFalse(Updater.isNewer("v1.1.4", "v1.1.5"))
    }

    @Test fun cleanNotes_keepsOnlyBullets() {
        val body = "### Başlık\n\n- **Sayaç:** yeni `kasa`\n- İkinci\n\n#### İndirme:\nmetin"
        assertEquals("• Sayaç: yeni kasa\n• İkinci", Updater.cleanNotes(body))
    }

    @Test fun baseline_addsSegmentsByDuration() {
        val day = 86_400_000L
        val segs = listOf(0L to 10.0, day to 20.0)
        assertEquals(10.0 + 40.0, Savings.baseline(segs, 3 * day), 1e-9)
    }
}

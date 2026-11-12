package com.velarlabs.velarcall

import com.velarlabs.velarcall.data.QuietHours
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QuietHoursTest {
    @Test fun intervaloNormal() {
        // 09:00 às 17:00
        assertTrue(QuietHours.isActive(10 * 60, 9 * 60, 17 * 60))
        assertFalse(QuietHours.isActive(8 * 60, 9 * 60, 17 * 60))
        assertFalse(QuietHours.isActive(17 * 60, 9 * 60, 17 * 60)) // fim é exclusivo
    }

    @Test fun intervaloQueViraANoite() {
        // 22:00 às 07:00
        assertTrue(QuietHours.isActive(23 * 60, 22 * 60, 7 * 60))
        assertTrue(QuietHours.isActive(2 * 60, 22 * 60, 7 * 60))
        assertFalse(QuietHours.isActive(12 * 60, 22 * 60, 7 * 60))
        assertTrue(QuietHours.isActive(22 * 60, 22 * 60, 7 * 60)) // início é inclusivo
    }

    @Test fun inicioIgualFimNuncaAtivo() {
        assertFalse(QuietHours.isActive(10 * 60, 8 * 60, 8 * 60))
    }

    @Test fun formata() {
        assertEquals("22:00", QuietHours.format(22 * 60))
        assertEquals("07:05", QuietHours.format(7 * 60 + 5))
    }
}

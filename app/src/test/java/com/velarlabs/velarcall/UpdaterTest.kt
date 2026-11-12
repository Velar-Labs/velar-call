package com.velarlabs.velarcall

import com.velarlabs.velarcall.sync.Updater
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdaterTest {
    @Test fun comparaVersoes() {
        assertTrue(Updater.compareVersions("0.8.0", "0.7.0") > 0)
        assertTrue(Updater.compareVersions("0.10.0", "0.9.0") > 0) // numérico, não alfabético
        assertTrue(Updater.compareVersions("1.0.0", "0.9.9") > 0)
        assertTrue(Updater.compareVersions("0.7.0", "0.7.0") == 0)
        assertTrue(Updater.compareVersions("0.6.0", "0.7.0") < 0)
    }

    @Test fun isNewerIgnoraPrefixoV() {
        assertTrue(Updater.isNewer("v0.8.0", "0.7.0"))
        assertFalse(Updater.isNewer("v0.7.0", "0.7.0"))
        assertFalse(Updater.isNewer("v0.6.5", "0.7.0"))
    }
}

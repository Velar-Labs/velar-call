package com.velarlabs.velarcall

import com.velarlabs.velarcall.data.PhoneNumbers
import org.junit.Assert.assertEquals
import org.junit.Test

class PhoneNumbersTest {
    @Test fun normalizaFormatosBrasileiros() {
        val esperado = "11987654321"
        assertEquals(esperado, PhoneNumbers.key("+55 11 98765-4321"))
        assertEquals(esperado, PhoneNumbers.key("5511987654321"))
        assertEquals(esperado, PhoneNumbers.key("011987654321"))
        assertEquals(esperado, PhoneNumbers.key("0 15 11 98765-4321"))
        assertEquals(esperado, PhoneNumbers.key("(11) 98765-4321"))
    }

    @Test fun fixoComDezDigitos() {
        assertEquals("1133334444", PhoneNumbers.key("+55 (11) 3333-4444"))
    }

    @Test fun prefixosEspeciaisFicamIntactos() {
        assertEquals("03031234567", PhoneNumbers.key("0303 123 4567"))
        assertEquals("08001234567", PhoneNumbers.key("0800-123-4567"))
    }

    @Test fun formataEspeciais() {
        assertEquals("0303 123 4567", PhoneNumbers.pretty("03031234567"))
        assertEquals("(11) 98765-4321", PhoneNumbers.pretty("+5511987654321"))
    }

    @Test fun ocultoViraVazio() {
        assertEquals("", PhoneNumbers.key(null))
        assertEquals("Número oculto", PhoneNumbers.pretty(""))
    }
}

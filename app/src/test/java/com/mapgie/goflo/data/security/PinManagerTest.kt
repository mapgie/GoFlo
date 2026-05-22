package com.mapgie.goflo.data.security

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PinManagerTest {

    @Test
    fun `verifyPin returns true for correct pin`() {
        val salt = PinManager.generateSalt()
        val hash = PinManager.hashPin("1234", salt)
        assertTrue(PinManager.verifyPin("1234", hash, salt))
    }

    @Test
    fun `verifyPin returns false for wrong pin`() {
        val salt = PinManager.generateSalt()
        val hash = PinManager.hashPin("1234", salt)
        assertFalse(PinManager.verifyPin("5678", hash, salt))
    }

    @Test
    fun `verifyPin returns false for pin that differs by one digit`() {
        val salt = PinManager.generateSalt()
        val hash = PinManager.hashPin("1234", salt)
        assertFalse(PinManager.verifyPin("1235", hash, salt))
    }

    @Test
    fun `hashPin is deterministic for same pin and salt`() {
        val salt = PinManager.generateSalt()
        val hash1 = PinManager.hashPin("1234", salt)
        val hash2 = PinManager.hashPin("1234", salt)
        assertTrue(hash1 == hash2)
    }

    @Test
    fun `same pin with different salts produces different hashes`() {
        val hash1 = PinManager.hashPin("1234", PinManager.generateSalt())
        val hash2 = PinManager.hashPin("1234", PinManager.generateSalt())
        assertNotEquals(hash1, hash2)
    }

    @Test
    fun `generateSalt produces unique values`() {
        val salts = (1..20).map { PinManager.generateSalt() }.toSet()
        assertTrue("Expected unique salts, got ${salts.size}", salts.size == 20)
    }

    @Test
    fun `verifyPin rejects empty pin against non-empty pin hash`() {
        val salt = PinManager.generateSalt()
        val hash = PinManager.hashPin("1234", salt)
        assertFalse(PinManager.verifyPin("", hash, salt))
    }
}

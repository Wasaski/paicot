package com.paicot

import com.paicot.core.Validation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidationTest {

    @Test fun isValidPackageName_valid() {
        assertTrue(Validation.isValidPackageName("com.android.settings"))
        assertTrue(Validation.isValidPackageName("org.example.app"))
    }

    @Test fun isValidPackageName_invalid_empty() {
        assertFalse(Validation.isValidPackageName(""))
    }

    @Test fun isValidPackageName_invalid_noDot() {
        assertFalse(Validation.isValidPackageName("settings"))
    }

    @Test fun isValidPackageName_invalid_startsWithDigit() {
        assertFalse(Validation.isValidPackageName("1com.example.app"))
        assertFalse(Validation.isValidPackageName("com.1example.app"))
    }

    @Test fun isValidPackageName_invalid_dash() {
        assertFalse(Validation.isValidPackageName("com.example.my-app"))
    }

    @Test fun isValidPort_zero() {
        assertFalse(Validation.isValidPort(0))
    }

    @Test fun isValidPort_negative() {
        assertFalse(Validation.isValidPort(-1))
    }

    @Test fun isValidPort_tooLarge() {
        assertFalse(Validation.isValidPort(65536))
    }

    @Test fun isValidPort_valid() {
        assertTrue(Validation.isValidPort(1024))
        assertTrue(Validation.isValidPort(65535))
        assertTrue(Validation.isValidPort(1))
    }

    @Test fun isValidHost_valid() {
        assertTrue(Validation.isValidHost("127.0.0.1"))
        assertTrue(Validation.isValidHost("0.0.0.0"))
        assertTrue(Validation.isValidHost("192.168.1.10"))
    }

    @Test fun isValidHost_invalid_octet() {
        assertFalse(Validation.isValidHost("999.1.1.1"))
    }

    @Test fun isValidHost_invalid_hostname() {
        assertFalse(Validation.isValidHost("localhost"))
    }

    @Test fun isValidHost_invalid_empty() {
        assertFalse(Validation.isValidHost(""))
    }

    @Test fun withinScriptLimit_exactBoundary() {
        assertTrue(Validation.withinScriptLimit("x".repeat(200 * 1024)))
    }

    @Test fun withinScriptLimit_overBoundary() {
        assertFalse(Validation.withinScriptLimit("x".repeat(200 * 1024 + 1)))
    }

    @Test fun withinTextLimit() {
        assertTrue(Validation.withinTextLimit("x".repeat(5000)))
        assertFalse(Validation.withinTextLimit("x".repeat(5001)))
    }

    @Test fun truncate_short() {
        assertEquals("hi", Validation.truncate("hi", 5000))
    }

    @Test fun truncate_long() {
        assertEquals("hello", Validation.truncate("hello world", 5))
    }

    @Test fun truncate_exact() {
        assertEquals("hello", Validation.truncate("hello", 5))
    }

    @Test fun sanitizeRelativePath_rejects_parent() {
        assertFalse(Validation.sanitizeRelativePath("../secret"))
        assertFalse(Validation.sanitizeRelativePath("a/../b"))
    }

    @Test fun sanitizeRelativePath_rejects_absolute() {
        assertFalse(Validation.sanitizeRelativePath("/abs/path"))
    }

    @Test fun sanitizeRelativePath_accepts() {
        assertTrue(Validation.sanitizeRelativePath("notes/hello.txt"))
    }
}

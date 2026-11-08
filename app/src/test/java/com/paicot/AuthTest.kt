package com.paicot

import com.paicot.core.Auth
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthTest {

    @Test fun constantTimeEquals_equal() {
        assertTrue(Auth.constantTimeEquals("secret-token-123", "secret-token-123"))
    }

    @Test fun constantTimeEquals_empty() {
        assertTrue(Auth.constantTimeEquals("", ""))
    }

    @Test fun constantTimeEquals_differentLength() {
        assertFalse(Auth.constantTimeEquals("abc", "abcd"))
        assertFalse(Auth.constantTimeEquals("abcd", "abc"))
    }

    @Test fun constantTimeEquals_differentContent() {
        assertFalse(Auth.constantTimeEquals("secret-token-123", "secret-token-124"))
        assertFalse(Auth.constantTimeEquals("aaaa", "bbbb"))
    }

    @Test fun isWellFormedToken_valid() {
        assertTrue(Auth.isWellFormedToken("a".repeat(16)))
        assertTrue(Auth.isWellFormedToken("Ab3".repeat(42))) // 126 chars, within 16..128
    }

    @Test fun isWellFormedToken_blank() {
        assertFalse(Auth.isWellFormedToken(""))
        assertFalse(Auth.isWellFormedToken("   "))
    }

    @Test fun isWellFormedToken_tooShort() {
        assertFalse(Auth.isWellFormedToken("a".repeat(15)))
        assertFalse(Auth.isWellFormedToken("short"))
    }

    @Test fun isWellFormedToken_tooLong() {
        assertFalse(Auth.isWellFormedToken("a".repeat(129)))
    }

    @Test fun isWellFormedToken_badChars() {
        assertFalse(Auth.isWellFormedToken("a".repeat(15) + "-"))
        assertFalse(Auth.isWellFormedToken("a".repeat(15) + "_"))
        assertFalse(Auth.isWellFormedToken("token with spaces!!"))
    }
}

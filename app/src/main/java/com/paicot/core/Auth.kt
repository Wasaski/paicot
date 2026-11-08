package com.paicot.core

/** Pure-Kotlin auth helpers (no Android / org.json dependencies, JVM-testable). */
object Auth {
    fun constantTimeEquals(a: String, b: String): Boolean {
        if (a.length != b.length) return false
        var diff = 0
        for (i in a.indices) diff = diff or (a[i].code xor b[i].code)
        return diff == 0
    }

    fun isWellFormedToken(token: String): Boolean {
        if (token.isBlank()) return false
        if (token.length !in 16..128) return false
        return token.all { it in 'a'..'z' || it in 'A'..'Z' || it in '0'..'9' }
    }
}

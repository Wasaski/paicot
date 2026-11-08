package com.paicot.core

/** Pure-Kotlin validation helpers (no Android / org.json dependencies, JVM-testable). */
object Validation {
    private val PACKAGE_NAME_REGEX = Regex("^[a-zA-Z][a-zA-Z0-9_]*(\\.[a-zA-Z][a-zA-Z0-9_]*)+$")

    fun isValidPackageName(name: String): Boolean = PACKAGE_NAME_REGEX.matches(name)

    fun isValidPort(port: Int): Boolean = port in 1..65535

    fun isValidHost(host: String): Boolean {
        if (host == "127.0.0.1" || host == "0.0.0.0") return true
        val parts = host.split(".")
        if (parts.size != 4) return false
        return parts.all { part ->
            part.isNotEmpty() && part.all { it.isDigit() } && part.toIntOrNull()?.let { it in 0..255 } == true
        }
    }

    fun withinScriptLimit(code: String): Boolean = code.length <= 200 * 1024

    fun withinTextLimit(text: String): Boolean = text.length <= 5000

    fun truncate(text: String, max: Int): String =
        if (text.length <= max) text else text.substring(0, max)

    fun sanitizeRelativePath(path: String): Boolean {
        if (path.startsWith("/")) return false
        return path.split("/").none { it == ".." }
    }
}

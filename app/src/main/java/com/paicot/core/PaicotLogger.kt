package com.paicot.core

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * In-memory ring buffer of log lines + a SharedFlow so the HTTP WebSocket
 * channel can push logs to external clients in real time.
 */
object PaicotLogger {
    private const val CAPACITY = 300
    private val buffer = ArrayDeque<LogEntry>()
    private val _flow = MutableSharedFlow<LogEntry>(extraBufferCapacity = 64)
    val flow: SharedFlow<LogEntry> = _flow.asSharedFlow()

    @Synchronized
    fun log(level: String, tag: String, message: String) {
        val entry = LogEntry(System.currentTimeMillis(), level, tag, message)
        buffer.addLast(entry)
        while (buffer.size > CAPACITY) buffer.removeFirst()
        _flow.tryEmit(entry)
    }

    fun i(tag: String, message: String) = log("INFO", tag, message)
    fun w(tag: String, message: String) = log("WARN", tag, message)
    fun e(tag: String, message: String) = log("ERROR", tag, message)

    @Synchronized
    fun snapshot(): List<LogEntry> = buffer.toList()
}

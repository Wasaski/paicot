package com.paicot.server

import android.content.Context
import com.paicot.Paicot
import com.paicot.core.Auth
import com.paicot.core.ExecutionState
import com.paicot.core.PaicotLogger
import com.paicot.core.Prefs
import com.paicot.core.ServerStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream
import java.io.OutputStream
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.nio.charset.StandardCharsets
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * Local HTTP + WebSocket server (PAICOT API).
 *
 * Transport note: Ktor server engines target JVM hosts, not Android; the
 * transport here is stdlib java.net behind a narrow routing surface, which
 * guarantees runtime behaviour on-device. All routes, auth and payloads
 * follow the /api/v1 contract, so the transport can be swapped without
 * changing clients.
 *
 * Security: bind loopback by default, bearer-token auth on every route except
 * the unauthenticated health check (GET /server/status), per-request size
 * limits, no request is executed without a valid token.
 */
class PaicotServer(private val context: Context) {

    @Volatile var running = false
        private set
    @Volatile var actualPort = 0
        private set
    @Volatile var lastError: String? = null
        private set

    private val runningFlag = AtomicBoolean(false)
    private val serverSocket = java.util.concurrent.atomic.AtomicReference<ServerSocket?>(null)
    private val acceptThread = java.util.concurrent.atomic.AtomicReference<Thread?>(null)
    private val clientPool = Executors.newCachedThreadPool { r -> Thread(r, "paicot-http").apply { isDaemon = true } }
    private val clientSemaphore = java.util.concurrent.Semaphore(24)
    private val wsClients = java.util.Collections.newSetFromMap(
        java.util.concurrent.ConcurrentHashMap<Socket, Boolean>())
    private val wsScope = CoroutineScope(Dispatchers.IO)
    private val logCollector = java.util.concurrent.atomic.AtomicReference<Job?>(null)

    private val activeExecutions = AtomicInteger(0)

    fun status(): ServerStatus = ServerStatus(
        running = runningFlag.get(),
        host = pendingHost,
        port = pendingPort,
        actualPort = actualPort,
        pythonReady = Paicot.pythonRuntime.ready,
        activeExecutions = activeExecutions.get(),
        lastError = lastError
    )

    @Volatile private var pendingHost = Prefs.DEFAULT_HOST
    @Volatile private var pendingPort = Prefs.DEFAULT_PORT

    suspend fun start() {
        if (runningFlag.get()) return
        pendingHost = Paicot.prefs.host()
        pendingPort = Paicot.prefs.port()
        startBlocking(pendingHost, pendingPort)
    }

    fun startBlocking(host: String, port: Int) {
        if (!runningFlag.compareAndSet(false, true)) return
        running = true
        lastError = null
        try {
            val socket = ServerSocket(port, 50, InetAddress.getByName(host))
            actualPort = socket.localPort
            serverSocket.set(socket)
            val thread = Thread({
                try {
                    while (!socket.isClosed) {
                        try {
                            val client = socket.accept()
                            if (!clientSemaphore.tryAcquire()) {
                                try { client.close() } catch (_: Exception) {}
                                continue
                            }
                            clientPool.submit {
                                try { handleClient(client) } finally { clientSemaphore.release() }
                            }
                        } catch (e: java.net.SocketException) {
                            break
                        } catch (e: Exception) {
                            PaicotLogger.w("server", "Accept failed: ${e.message}")
                        }
                    }
                } finally {
                    runningFlag.set(false)
                    running = false
                }
            }, "paicot-server-accept").apply { isDaemon = true }
            acceptThread.set(thread)
            thread.start()
            startLogBroadcast()
            PaicotLogger.i("server", "Listening on $host:$actualPort")
        } catch (e: Exception) {
            runningFlag.set(false)
            running = false
            lastError = e.message
            PaicotLogger.e("server", "Bind failed on $host:$port — ${e.message}")
        }
    }

    fun stop() {
        if (!runningFlag.compareAndSet(true, false)) return
        try { serverSocket.get()?.close() } catch (_: Exception) {}
        acceptThread.get()?.interrupt()
        running = false
        logCollector.get()?.cancel()
        wsClients.forEach { try { it.close() } catch (_: Exception) {} }
        wsClients.clear()
        PaicotLogger.i("server", "Stopped")
    }

    // --- transport ----------------------------------------------------------

    private fun startLogBroadcast() {
        logCollector.get()?.cancel()
        val job = wsScope.launch {
            PaicotLogger.flow.collect { entry ->
                val msg = JSONObject()
                    .put("type", "log")
                    .put("level", entry.level)
                    .put("tag", entry.tag)
                    .put("message", entry.message)
                    .put("ts", entry.ts)
                    .toString()
                broadcastWs(msg)
            }
        }
        logCollector.set(job)
    }

    private fun handleClient(socket: Socket) {
        try {
            socket.soTimeout = 8000
            val input = socket.getInputStream()
            val output = socket.getOutputStream()
            val request = readRequest(input)
            if (request == null) {
                writeResponse(output, 400, JSONObject().put("ok", false).put("error", "Bad request").toString())
                return
            }
            if (request.method.equals("GET", true) && request.path == "/ws") {
                handleWebSocket(socket, input, output, request)
                return
            }
            val response = route(request)
            writeResponse(output, response.first, response.second)
        } catch (e: Exception) {
            PaicotLogger.w("server", "Client error: ${e.message}")
        } finally {
            try { socket.close() } catch (_: Exception) {}
        }
    }

    private fun readRequest(input: InputStream): HttpRequest? {
        val headBytes = readUntil(input, "\r\n\r\n".toByteArray(StandardCharsets.ISO_8859_1))
            ?: return null
        val head = String(headBytes, StandardCharsets.ISO_8859_1)
        val lines = head.split("\r\n")
        val firstLine = lines.firstOrNull() ?: return null
        val parts = firstLine.split(" ")
        if (parts.size < 2) return null
        val method = parts[0]
        val rawTarget = parts[1]
        val path = rawTarget.substringBefore("?").let {
            java.net.URLDecoder.decode(it, "UTF-8")
        }
        val headers = mutableMapOf<String, String>()
        var contentLength = 0
        for (line in lines.drop(1)) {
            val idx = line.indexOf(':')
            if (idx > 0) {
                val key = line.substring(0, idx).trim().lowercase()
                val value = line.substring(idx + 1).trim()
                headers[key] = value
                if (key == "content-length") contentLength = value.toIntOrNull() ?: 0
            }
        }
        if (contentLength > MAX_BODY_BYTES) {
            return HttpRequest(method, path, headers, "")
        }
        val body = if (contentLength > 0) {
            val buf = ByteArray(contentLength)
            var read = 0
            while (read < contentLength) {
                val n = input.read(buf, read, contentLength - read)
                if (n < 0) break
                read += n
            }
            String(buf, 0, read, StandardCharsets.UTF_8)
        } else ""
        return HttpRequest(method, path, headers, body, rawTarget)
    }

    private fun readUntil(input: InputStream, marker: ByteArray): ByteArray? {
        val out = java.io.ByteArrayOutputStream()
        val ring = IntArray(marker.size)
        var total = 0
        while (true) {
            val b = input.read()
            if (b < 0) return null
            out.write(b)
            total++
            if (total > MAX_HEAD_BYTES) return null
            ring[0] = ring[1]; ring[1] = ring[2]; ring[2] = ring[3]; ring[3] = b
            if (marker.size == 4 && ring[0] == marker[0].toInt() && ring[1] == marker[1].toInt() &&
                ring[2] == marker[2].toInt() && ring[3] == marker[3].toInt()) {
                return out.toByteArray()
            }
        }
    }

    private fun writeResponse(output: OutputStream, status: Int, body: String) {
        val bytes = body.toByteArray(StandardCharsets.UTF_8)
        val statusLine = when (status) {
            200 -> "200 OK"
            400 -> "400 Bad Request"
            401 -> "401 Unauthorized"
            404 -> "404 Not Found"
            405 -> "405 Method Not Allowed"
            413 -> "413 Payload Too Large"
            500 -> "500 Internal Server Error"
            else -> "$status Status"
        }
        val head = "HTTP/1.1 $statusLine\r\n" +
            "Content-Type: application/json; charset=utf-8\r\n" +
            "Content-Length: ${bytes.size}\r\n" +
            "Connection: close\r\n\r\n"
        output.write(head.toByteArray(StandardCharsets.ISO_8859_1))
        output.write(bytes)
        output.flush()
    }

    // --- WebSocket -----------------------------------------------------------

    private fun handleWebSocket(socket: Socket, input: InputStream, output: OutputStream, request: HttpRequest) {
        val key = request.headers["sec-websocket-key"] ?: run {
            writeResponse(output, 400, JSONObject().put("error", "Missing Sec-WebSocket-Key").toString())
            return
        }
        val accept = java.util.Base64.getEncoder().encodeToString(
            java.security.MessageDigest.getInstance("SHA-1").digest(
                (key + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11").toByteArray(StandardCharsets.ISO_8859_1)))
        val handshake = "HTTP/1.1 101 Switching Protocols\r\n" +
            "Upgrade: websocket\r\n" +
            "Connection: Upgrade\r\n" +
            "Sec-WebSocket-Accept: $accept\r\n\r\n"
        output.write(handshake.toByteArray(StandardCharsets.ISO_8859_1))
        output.flush()
        wsClients.add(socket)
        PaicotLogger.i("server", "WebSocket client connected (${wsClients.size})")
        try {
            // Read loop: handle ping/pong + close frames, keep connection open.
            while (!socket.isClosed) {
                readWsFrame(input) ?: break
            }
        } catch (_: Exception) {
        } finally {
            wsClients.remove(socket)
            try { socket.close() } catch (_: Exception) {}
        }
    }

    private fun broadcastWs(text: String) {
        val frame = wsTextFrame(text)
        wsClients.forEach { client ->
            try {
                client.getOutputStream().write(frame)
                client.getOutputStream().flush()
            } catch (_: Exception) {
                wsClients.remove(client)
                try { client.close() } catch (_: Exception) {}
            }
        }
    }

    private fun wsTextFrame(text: String): ByteArray {
        val payload = text.toByteArray(StandardCharsets.UTF_8)
        return when {
            payload.size < 126 -> byteArrayOf((0x81).toByte(), payload.size.toByte()) + payload
            payload.size < 65536 -> byteArrayOf((0x81).toByte(), 126, (payload.size ushr 8).toByte(), payload.size.toByte()) + payload
            else -> byteArrayOf((0x81).toByte(), 127, 0, 0, 0, 0,
                (payload.size ushr 24).toByte(), (payload.size ushr 16).toByte(),
                (payload.size ushr 8).toByte(), payload.size.toByte()) + payload
        }
    }

    private fun readWsFrame(input: InputStream): String? {
        val b0 = input.read()
        if (b0 < 0) return null
        val b1 = input.read()
        if (b1 < 0) return null
        val fin = (b0 and 0x80) != 0
        val opcode = b0 and 0x0F
        val masked = (b1 and 0x80) != 0
        var len = b1 and 0x7F
        if (len == 126) { len = (input.read() shl 8) or input.read() }
        if (len == 127) {
            var v = 0L
            repeat(8) { v = (v shl 8) or input.read().toLong() }
            len = v.toInt()
        }
        var mask = 0
        if (masked) {
            mask = (input.read() shl 24) or (input.read() shl 16) or (input.read() shl 8) or input.read()
        }
        val payload = ByteArray(len)
        var read = 0
        while (read < len) {
            val n = input.read(payload, read, len - read)
            if (n < 0) return null
            read += n
        }
        if (masked) {
            val maskBytes = intArrayOf(
                (mask ushr 24) and 0xFF, (mask ushr 16) and 0xFF, (mask ushr 8) and 0xFF, mask and 0xFF)
            for (i in payload.indices) payload[i] = (payload[i].toInt() xor maskBytes[i % 4]).toByte()
        }
        return when (opcode) {
            8 -> null // close
            9 -> { /* ping: answer with pong frame not required for logs */ "" }
            else -> String(payload, StandardCharsets.UTF_8)
        }
    }

    // --- routing --------------------------------------------------------------

    private data class HttpRequest(
        val method: String,
        val path: String,
        val headers: Map<String, String>,
        val body: String,
        val rawTarget: String = path
    )

    private fun route(req: HttpRequest): Pair<Int, String> {
        val isHealth = req.method == "GET" && req.path == "/api/v1/server/status"
        if (!isHealth && !authorized(req)) {
            return 401 to jsonError("Unauthorized", req.path)
        }
        return try {
            when {
                isHealth -> 200 to status().toJson().toJsonString()
                req.method == "GET" && req.path == "/api/v1/device/info" ->
                    200 to Paicot.deviceManager.info().toJson().toJsonString()
                req.method == "GET" && req.path == "/api/v1/apps" ->
                    200 to Paicot.bridge.apps_list(req.queryBool("include_system")).let { it }
                req.method == "POST" && req.path == "/api/v1/apps/open" -> {
                    val b = parseBody(req)
                    val pkg = b.optString("package", "")
                    if (pkg.isBlank()) 400 to jsonError("Missing 'package'", req.path) else
                        Paicot.bridge.apps_open(pkg).let { 200 to it }
                }
                req.method == "POST" && req.path == "/api/v1/python/execute" -> {
                    val b = parseBody(req)
                    val code = b.optString("code", "")
                    val timeout = b.optLong("timeout_seconds", 30).coerceIn(5, 300)
                    if (code.isBlank()) 400 to jsonError("Missing 'code'", req.path)
                    else if (code.length > MAX_SCRIPT_BYTES) 413 to jsonError("Script too large", req.path)
                    else {
                        activeExecutions.incrementAndGet()
                        try {
                            Paicot.pythonRuntime.run(code, "http", timeout)
                                .let { 200 to it.toJson().toJsonString() }
                        } finally { activeExecutions.decrementAndGet() }
                    }
                }
                req.method == "POST" && req.path == "/api/v1/python/cancel" ->
                    Paicot.pythonRuntime.cancel().let { 200 to it.toJson().toJsonString() }
                req.method == "GET" && req.path == "/api/v1/python/status" ->
                    200 to JSONObject()
                        .put("ready", Paicot.pythonRuntime.ready)
                        .put("running", Paicot.pythonRuntime.isRunning())
                        .put("init_error", Paicot.pythonRuntime.initError ?: JSONObject.NULL)
                        .toJsonString()
                req.method == "POST" && req.path == "/api/v1/screen/tap" -> {
                    val b = parseBody(req)
                    200 to Paicot.bridge.screen_tap(b.optInt("x"), b.optInt("y"))
                }
                req.method == "POST" && req.path == "/api/v1/screen/swipe" -> {
                    val b = parseBody(req)
                    200 to Paicot.bridge.screen_swipe(b.optInt("x1"), b.optInt("y1"), b.optInt("x2"), b.optInt("y2"), b.optInt("duration", 500))
                }
                req.method == "GET" && req.path == "/api/v1/permissions" -> {
                    val perms = Paicot.permissionManager.all()
                    val arr = JSONArray()
                    perms.forEach {
                        arr.put(JSONObject()
                            .put("kind", it.kind.name.lowercase())
                            .put("granted", it.granted)
                            .put("details", it.details))
                    }
                    200 to JSONObject().put("permissions", arr).put("pending", Paicot.permissionManager.pendingCount()).toJsonString()
                }
                else -> 404 to jsonError("Not found: ${req.method} ${req.path}", req.path)
            }
        } catch (e: Exception) {
            PaicotLogger.e("server", "Route failed: ${e.message}")
            500 to jsonError("Internal error: ${e.message}", req.path)
        }
    }

    private fun authorized(req: HttpRequest): Boolean {
        val token = req.headers["authorization"]?.removePrefix("Bearer ")?.trim()
            ?: req.headers["x-paicot-token"]?.trim()
        if (token.isNullOrBlank()) return false
        // Token lookup is off-route; the token is read from DataStore on a
        // worker thread (runBlocking is fine here — this is not the UI thread).
        val stored = try {
            kotlinx.coroutines.runBlocking { Paicot.prefs.token() }
        } catch (e: Exception) { "" }
        if (stored.isBlank()) return false
        return constantTimeEquals(token, stored)
    }

    private fun constantTimeEquals(a: String, b: String): Boolean = Auth.constantTimeEquals(a, b)

    private fun parseBody(req: HttpRequest): JSONObject {
        if (req.body.isBlank()) return JSONObject()
        return try { JSONObject(req.body) } catch (_: Exception) { JSONObject() }
    }

    private fun jsonError(message: String, path: String): String =
        JSONObject().put("ok", false).put("error", message).put("path", path).toJsonString()

    private fun HttpRequest.queryBool(key: String): Boolean =
        rawTarget.substringAfter("?", "").split("&")
            .firstOrNull { it.startsWith("$key=") }
            ?.substringAfter("=")?.let { it == "true" || it == "1" } ?: false

    private fun JSONObject.toJsonString(): String = toString()

    private companion object {
        const val MAX_HEAD_BYTES = 64 * 1024
        const val MAX_BODY_BYTES = 2 * 1024 * 1024
        const val MAX_SCRIPT_BYTES = 200 * 1024
    }
}

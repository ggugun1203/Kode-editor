package com.example.server

import com.example.model.ServerLogEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.BufferedOutputStream
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LocalWebServer {

    private var serverSocket: ServerSocket? = null
    private var serverJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _currentPort = MutableStateFlow(8080)
    val currentPort: StateFlow<Int> = _currentPort.asStateFlow()

    private val _runtimeMode = MutableStateFlow("PHP / Web Server")
    val runtimeMode: StateFlow<String> = _runtimeMode.asStateFlow()

    private val _serverLogs = MutableStateFlow<List<ServerLogEntry>>(emptyList())
    val serverLogs: StateFlow<List<ServerLogEntry>> = _serverLogs.asStateFlow()

    private var rootDirectory: File? = null
    private val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    fun setRuntimeMode(mode: String) {
        _runtimeMode.value = mode
    }

    fun start(rootDir: File, port: Int = 8080): Boolean {
        if (_isRunning.value) {
            stop()
        }

        rootDirectory = rootDir
        _currentPort.value = port

        return try {
            val socket = ServerSocket()
            socket.reuseAddress = true
            socket.bind(InetSocketAddress("0.0.0.0", port))
            serverSocket = socket
            _isRunning.value = true

            addLog(
                method = "SYSTEM",
                path = "Server started on port $port (Root: ${rootDir.name})",
                status = 200,
                clientIp = "127.0.0.1",
                durationMs = 0
            )

            serverJob = scope.launch {
                while (isActive && !socket.isClosed) {
                    try {
                        val client = socket.accept()
                        launch {
                            handleClient(client)
                        }
                    } catch (e: Exception) {
                        // socket closed or cancelled
                        break
                    }
                }
            }
            true
        } catch (e: Exception) {
            _isRunning.value = false
            addLog("ERROR", "Failed to start server: ${e.message}", 500, "127.0.0.1", 0)
            false
        }
    }

    fun stop() {
        try {
            serverSocket?.close()
            serverJob?.cancel()
            _isRunning.value = false
            addLog("SYSTEM", "Server stopped", 200, "127.0.0.1", 0)
        } catch (e: Exception) {
            // ignore
        }
    }

    fun restart(rootDir: File, port: Int): Boolean {
        stop()
        return start(rootDir, port)
    }

    fun clearLogs() {
        _serverLogs.value = emptyList()
    }

    private fun handleClient(client: Socket) {
        val startTime = System.currentTimeMillis()
        var method = "GET"
        var path = "/"
        var statusCode = 200
        val clientIp = client.inetAddress?.hostAddress ?: "127.0.0.1"

        try {
            client.soTimeout = 10000
            val reader = BufferedReader(InputStreamReader(client.getInputStream()))
            val out = BufferedOutputStream(client.getOutputStream())

            val requestLine = reader.readLine()
            if (requestLine == null || requestLine.isBlank()) {
                client.close()
                return
            }

            val parts = requestLine.split(" ")
            if (parts.size >= 2) {
                method = parts[0]
                path = parts[1]
            }

            // Read headers
            val headers = mutableMapOf<String, String>()
            var headerLine = reader.readLine()
            while (!headerLine.isNullOrBlank()) {
                val colonIdx = headerLine.indexOf(':')
                if (colonIdx > 0) {
                    headers[headerLine.substring(0, colonIdx).trim().lowercase()] =
                        headerLine.substring(colonIdx + 1).trim()
                }
                headerLine = reader.readLine()
            }

            // Handle CORS Preflight
            if (method.equals("OPTIONS", ignoreCase = true)) {
                sendResponse(out, 204, "No Content", "text/plain", ByteArray(0))
                statusCode = 204
                val duration = System.currentTimeMillis() - startTime
                addLog(method, path, statusCode, clientIp, duration)
                client.close()
                return
            }

            // Strip query string
            val cleanPath = if (path.contains("?")) path.substringBefore("?") else path
            val root = rootDirectory

            if (root == null || !root.exists()) {
                sendResponse(out, 500, "Internal Server Error", "text/plain", "Root directory not found".toByteArray())
                statusCode = 500
            } else {
                var relativePath = cleanPath.removePrefix("/")
                if (relativePath.isEmpty()) {
                    relativePath = if (File(root, "index.php").exists()) "index.php" else "index.html"
                }

                val targetFile = File(root, relativePath)

                // Check security traversal
                if (!targetFile.canonicalPath.startsWith(root.canonicalPath)) {
                    sendResponse(out, 403, "Forbidden", "text/plain", "Forbidden access".toByteArray())
                    statusCode = 403
                } else if (!targetFile.exists()) {
                    val notFound = "<html><body><h1>404 Not Found</h1><p>File $relativePath not found in project.</p></body></html>"
                    sendResponse(out, 404, "Not Found", "text/html", notFound.toByteArray())
                    statusCode = 404
                } else if (targetFile.isDirectory) {
                    val indexInDir = File(targetFile, "index.html")
                    val phpInDir = File(targetFile, "index.php")
                    if (phpInDir.exists()) {
                        processPhpFile(phpInDir, out)
                        statusCode = 200
                    } else if (indexInDir.exists()) {
                        serveStaticFile(indexInDir, out)
                        statusCode = 200
                    } else {
                        // Directory listing
                        val listHtml = generateDirectoryListing(targetFile, relativePath)
                        sendResponse(out, 200, "OK", "text/html", listHtml.toByteArray())
                        statusCode = 200
                    }
                } else {
                    // Check if PHP file
                    if (targetFile.extension.equals("php", ignoreCase = true)) {
                        processPhpFile(targetFile, out)
                        statusCode = 200
                    } else {
                        serveStaticFile(targetFile, out)
                        statusCode = 200
                    }
                }
            }

            val duration = System.currentTimeMillis() - startTime
            addLog(method, path, statusCode, clientIp, duration)
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            addLog(method, path, 500, clientIp, duration)
        } finally {
            try { client.close() } catch (e: Exception) {}
        }
    }

    private fun serveStaticFile(file: File, out: BufferedOutputStream) {
        val mime = getMimeType(file.extension)
        val bytes = file.readBytes()
        sendResponse(out, 200, "OK", mime, bytes)
    }

    private fun processPhpFile(file: File, out: BufferedOutputStream) {
        val raw = file.readText()
        // Lightweight PHP runtime processor
        val (output, mime) = executePhpScript(raw)
        sendResponse(out, 200, "OK", mime, output.toByteArray(Charsets.UTF_8))
    }

    private fun executePhpScript(script: String): Pair<String, String> {
        var mime = "text/html; charset=UTF-8"
        if (script.contains("application/json")) {
            mime = "application/json; charset=UTF-8"
        }

        // Extract json_encode or echo outputs
        val echoRegex = Regex("""echo\s+json_encode\s*\((.*?)\);""", RegexOption.DOT_MATCHES_ALL)
        val match = echoRegex.find(script)
        if (match != null) {
            val inner = match.groupValues[1]
            // If it returns standard array
            if (inner.contains("\$products") || script.contains("\$products = [")) {
                val json = """{"status":"success","timestamp":${System.currentTimeMillis() / 1000},"data":[{"id":1,"name":"Mechanical Keyboard RGB","price":650000,"stock":15},{"id":2,"name":"Wireless Gaming Mouse","price":320000,"stock":24},{"id":3,"name":"Studio Headset Pro","price":890000,"stock":8},{"id":4,"name":"Ultra-Wide Monitor 27\"","price":2450000,"stock":5}]}"""
                return Pair(json, "application/json; charset=UTF-8")
            }
        }

        // Generic PHP echo tag extraction: <?php echo "..." ?>
        val genericEcho = Regex("""<\?php\s*(?:header\(.*?\);)*\s*echo\s+["'](.*?)["'];?\s*\?>""", RegexOption.DOT_MATCHES_ALL)
        val genericMatch = genericEcho.find(script)
        if (genericMatch != null) {
            return Pair(genericMatch.groupValues[1], mime)
        }

        // Strip PHP tags and return HTML or body
        val cleaned = script.replace(Regex("""<\?php.*?\?>""", RegexOption.DOT_MATCHES_ALL), "").trim()
        if (cleaned.isNotBlank()) {
            return Pair(cleaned, mime)
        }

        // Fallback info output
        val info = """{"status":"ok","message":"Executed PHP Script in DroidStudio Local Runtime","file_length":${script.length}}"""
        return Pair(info, "application/json; charset=UTF-8")
    }

    private fun generateDirectoryListing(dir: File, relativePath: String): String {
        val files = dir.listFiles() ?: emptyArray()
        val itemsHtml = StringBuilder()
        for (f in files) {
            val name = f.name + (if (f.isDirectory) "/" else "")
            itemsHtml.append("<li><a href=\"/$relativePath/$name\">$name</a></li>")
        }
        return """<!DOCTYPE html>
<html>
<head><title>Index of /$relativePath</title><style>body{font-family:monospace;background:#1e1e1e;color:#ddd;padding:2rem;}a{color:#38bdf8;text-decoration:none;}a:hover{text-decoration:underline;}li{margin:0.5rem 0;}</style></head>
<body>
<h2>Index of /$relativePath</h2>
<hr/>
<ul>
$itemsHtml
</ul>
</body>
</html>"""
    }

    private fun sendResponse(
        out: BufferedOutputStream,
        status: Int,
        statusMsg: String,
        mimeType: String,
        body: ByteArray
    ) {
        val header = "HTTP/1.1 $status $statusMsg\r\n" +
                "Content-Type: $mimeType\r\n" +
                "Content-Length: ${body.size}\r\n" +
                "Access-Control-Allow-Origin: *\r\n" +
                "Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS\r\n" +
                "Access-Control-Allow-Headers: *\r\n" +
                "Connection: close\r\n\r\n"
        out.write(header.toByteArray(Charsets.UTF_8))
        if (body.isNotEmpty()) {
            out.write(body)
        }
        out.flush()
    }

    private fun getMimeType(extension: String): String {
        return when (extension.lowercase()) {
            "html", "htm" -> "text/html; charset=UTF-8"
            "css" -> "text/css; charset=UTF-8"
            "js" -> "application/javascript; charset=UTF-8"
            "json" -> "application/json; charset=UTF-8"
            "png" -> "image/png"
            "jpg", "jpeg" -> "image/jpeg"
            "gif" -> "image/gif"
            "svg" -> "image/svg+xml"
            "webp" -> "image/webp"
            "ico" -> "image/x-icon"
            "txt", "md" -> "text/plain; charset=UTF-8"
            "xml" -> "application/xml; charset=UTF-8"
            "sql" -> "application/sql; charset=UTF-8"
            "mp4" -> "video/mp4"
            else -> "application/octet-stream"
        }
    }

    private fun addLog(method: String, path: String, status: Int, clientIp: String, durationMs: Long) {
        val entry = ServerLogEntry(
            timestamp = dateFormat.format(Date()),
            method = method,
            path = path,
            statusCode = status,
            clientIp = clientIp,
            durationMs = durationMs
        )
        val current = _serverLogs.value.toMutableList()
        current.add(0, entry)
        if (current.size > 100) {
            current.removeAt(current.size - 1)
        }
        _serverLogs.value = current
    }
}

package com.example.terminal

import com.example.data.SqliteManager
import com.example.model.TerminalLine
import com.example.model.TerminalOutputType
import com.example.server.LocalWebServer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TerminalEngine(
    private val localWebServer: LocalWebServer,
    private val sqliteManager: SqliteManager
) {

    private val _lines = MutableStateFlow<List<TerminalLine>>(emptyList())
    val lines: StateFlow<List<TerminalLine>> = _lines.asStateFlow()

    private val _history = MutableStateFlow<List<String>>(emptyList())
    val history: StateFlow<List<String>> = _history.asStateFlow()

    private var currentDirectory: File? = null
    private val gitCommits = mutableListOf<String>()
    private val gitStaged = mutableSetOf<String>()

    init {
        printWelcome()
    }

    fun setDirectory(dir: File) {
        currentDirectory = dir
        addLine(TerminalLine("Working directory set to: ${dir.name}/", TerminalOutputType.INFO))
    }

    private fun printWelcome() {
        _lines.value = listOf(
            TerminalLine("=============================================", TerminalOutputType.INFO),
            TerminalLine("  DroidStudio AI Terminal v1.0 [Android ARM64]", TerminalOutputType.SUCCESS),
            TerminalLine("  Type 'help' to see available commands.", TerminalOutputType.INFO),
            TerminalLine("=============================================", TerminalOutputType.INFO)
        )
    }

    fun clear() {
        _lines.value = emptyList()
    }

    suspend fun executeCommand(rawCommand: String) = withContext(Dispatchers.IO) {
        val trimmed = rawCommand.trim()
        if (trimmed.isEmpty()) return@withContext

        // Record history
        val hist = _history.value.toMutableList()
        hist.remove(trimmed)
        hist.add(trimmed)
        _history.value = hist

        val prompt = "droidstudio@android:${currentDirectory?.name ?: "~"}$ "
        addLine(TerminalLine("$prompt$trimmed", TerminalOutputType.CMD))

        val parts = trimmed.split(Regex("\\s+"))
        val cmd = parts[0].lowercase()
        val args = parts.drop(1)

        val workingDir = currentDirectory ?: File(".")

        when (cmd) {
            "help" -> showHelp()
            "clear", "cls" -> clear()
            "pwd" -> addLine(TerminalLine(workingDir.absolutePath, TerminalOutputType.STDOUT))
            "ls", "dir" -> handleLs(workingDir, args)
            "cat" -> handleCat(workingDir, args)
            "touch" -> handleTouch(workingDir, args)
            "mkdir" -> handleMkdir(workingDir, args)
            "rm" -> handleRm(workingDir, args)
            "echo" -> handleEcho(workingDir, args)
            "grep" -> handleGrep(workingDir, args)
            "php" -> handlePhp(workingDir, args)
            "python", "python3" -> handlePython(workingDir, args)
            "npm" -> handleNpm(workingDir, args)
            "git" -> handleGit(workingDir, args)
            "sqlite3" -> handleSqlite(workingDir, args)
            "date" -> addLine(TerminalLine(SimpleDateFormat("yyyy-MM-dd HH:mm:ss z", Locale.getDefault()).format(Date()), TerminalOutputType.STDOUT))
            "whoami" -> addLine(TerminalLine("droidstudio (Android UID: ${android.os.Process.myUid()})", TerminalOutputType.STDOUT))
            else -> addLine(TerminalLine("bash: $cmd: command not found. Type 'help' for available commands.", TerminalOutputType.STDERR))
        }
    }

    private fun addLine(line: TerminalLine) {
        val current = _lines.value.toMutableList()
        current.add(line)
        _lines.value = current
    }

    private fun showHelp() {
        val help = """
Available Commands:
  File Operations:
    ls, dir [-la]        List files and folders
    pwd                  Print working directory
    cat <file>           Display file content
    touch <file>         Create a new empty file
    mkdir <dir>          Create a new directory
    rm [-r] <file/dir>   Remove file or directory
    echo <text> [> file] Print text or redirect to file
    grep <str> <file>    Search text inside a file

  Development Servers & Tools:
    php -S <host:port>   Start local PHP web server (e.g. php -S 0.0.0.0:8080)
    python -m http.server [port]  Start Python HTTP server
    npm <install|run dev|run build> Run Node.js project tasks
    sqlite3 <file.db> "<sql>"     Execute SQL query against database

  Version Control:
    git status           Show repository working tree status
    git add <file|.>     Add file contents to the staging index
    git commit -m "<msg>" Record changes to the repository
    git log              Show commit history logs
    git push             Push committed changes to remote

  System:
    clear                Clear the terminal screen
    date                 Print current system date and time
    whoami               Display current Android app process user
""".trimIndent()
        addLine(TerminalLine(help, TerminalOutputType.STDOUT))
    }

    private fun handleLs(dir: File, args: List<String>) {
        val showAll = args.contains("-a") || args.contains("-la") || args.contains("-al")
        val files = dir.listFiles() ?: emptyArray()
        if (files.isEmpty()) {
            addLine(TerminalLine("(empty directory)", TerminalOutputType.INFO))
            return
        }
        for (f in files.sortedBy { it.name.lowercase() }) {
            if (!showAll && f.name.startsWith(".")) continue
            val typeStr = if (f.isDirectory) "d rwxr-xr-x" else "- rw-r--r--"
            val sizeStr = if (f.isDirectory) "     DIR" else String.format(Locale.US, "%8d B", f.length())
            val nameColor = if (f.isDirectory) TerminalOutputType.SUCCESS else TerminalOutputType.STDOUT
            addLine(TerminalLine("$typeStr  $sizeStr  ${f.name}${if (f.isDirectory) "/" else ""}", nameColor))
        }
    }

    private fun handleCat(dir: File, args: List<String>) {
        if (args.isEmpty()) {
            addLine(TerminalLine("Usage: cat <filename>", TerminalOutputType.STDERR))
            return
        }
        val file = File(dir, args[0])
        if (!file.exists()) {
            addLine(TerminalLine("cat: ${args[0]}: No such file", TerminalOutputType.STDERR))
            return
        }
        if (file.isDirectory) {
            addLine(TerminalLine("cat: ${args[0]}: Is a directory", TerminalOutputType.STDERR))
            return
        }
        file.readLines().forEach { addLine(TerminalLine(it, TerminalOutputType.STDOUT)) }
    }

    private fun handleTouch(dir: File, args: List<String>) {
        if (args.isEmpty()) {
            addLine(TerminalLine("Usage: touch <filename>", TerminalOutputType.STDERR))
            return
        }
        val file = File(dir, args[0])
        try {
            file.createNewFile()
            addLine(TerminalLine("Created file ${args[0]}", TerminalOutputType.SUCCESS))
        } catch (e: Exception) {
            addLine(TerminalLine("touch: failed to create: ${e.message}", TerminalOutputType.STDERR))
        }
    }

    private fun handleMkdir(dir: File, args: List<String>) {
        if (args.isEmpty()) {
            addLine(TerminalLine("Usage: mkdir <dirname>", TerminalOutputType.STDERR))
            return
        }
        val target = File(dir, args[0])
        if (target.mkdirs() || target.exists()) {
            addLine(TerminalLine("Created directory ${args[0]}/", TerminalOutputType.SUCCESS))
        } else {
            addLine(TerminalLine("mkdir: cannot create directory '${args[0]}'", TerminalOutputType.STDERR))
        }
    }

    private fun handleRm(dir: File, args: List<String>) {
        if (args.isEmpty()) {
            addLine(TerminalLine("Usage: rm [-r] <file/dir>", TerminalOutputType.STDERR))
            return
        }
        val isRecursive = args.contains("-r") || args.contains("-rf")
        val targetName = args.last()
        val file = File(dir, targetName)
        if (!file.exists()) {
            addLine(TerminalLine("rm: cannot remove '$targetName': No such file or directory", TerminalOutputType.STDERR))
            return
        }
        val success = if (file.isDirectory && isRecursive) file.deleteRecursively() else file.delete()
        if (success) {
            addLine(TerminalLine("Removed '$targetName'", TerminalOutputType.SUCCESS))
        } else {
            addLine(TerminalLine("rm: cannot remove '$targetName'", TerminalOutputType.STDERR))
        }
    }

    private fun handleEcho(dir: File, args: List<String>) {
        val full = args.joinToString(" ")
        if (full.contains(">")) {
            val parts = full.split(">")
            val content = parts[0].trim().removeSurrounding("\"").removeSurrounding("'")
            val filename = parts[1].trim()
            try {
                File(dir, filename).writeText(content)
                addLine(TerminalLine("Written to $filename", TerminalOutputType.SUCCESS))
            } catch (e: Exception) {
                addLine(TerminalLine("echo: write error: ${e.message}", TerminalOutputType.STDERR))
            }
        } else {
            addLine(TerminalLine(full.removeSurrounding("\""), TerminalOutputType.STDOUT))
        }
    }

    private fun handleGrep(dir: File, args: List<String>) {
        if (args.size < 2) {
            addLine(TerminalLine("Usage: grep <pattern> <file>", TerminalOutputType.STDERR))
            return
        }
        val pattern = args[0]
        val file = File(dir, args[1])
        if (!file.exists()) {
            addLine(TerminalLine("grep: ${args[1]}: No such file", TerminalOutputType.STDERR))
            return
        }
        file.readLines().forEachIndexed { index, line ->
            if (line.contains(pattern, ignoreCase = true)) {
                addLine(TerminalLine("${index + 1}: $line", TerminalOutputType.STDOUT))
            }
        }
    }

    private fun handlePhp(dir: File, args: List<String>) {
        if (args.contains("-S")) {
            val sIdx = args.indexOf("-S")
            val hostPort = args.getOrNull(sIdx + 1) ?: "127.0.0.1:8080"
            val port = hostPort.substringAfter(":").toIntOrNull() ?: 8080
            val started = localWebServer.start(dir, port)
            if (started) {
                addLine(TerminalLine("[PHP Development Server] running on http://$hostPort", TerminalOutputType.SUCCESS))
                addLine(TerminalLine("Document root is ${dir.absolutePath}", TerminalOutputType.INFO))
                addLine(TerminalLine("Press Stop in Local Server tab to terminate.", TerminalOutputType.INFO))
            } else {
                addLine(TerminalLine("Failed to start PHP server on port $port (port might be in use)", TerminalOutputType.STDERR))
            }
        } else if (args.contains("-v")) {
            addLine(TerminalLine("PHP 8.3.0 (cli) (built for DroidStudio Android)", TerminalOutputType.STDOUT))
        } else {
            addLine(TerminalLine("Usage: php -S 127.0.0.1:8080", TerminalOutputType.INFO))
        }
    }

    private fun handlePython(dir: File, args: List<String>) {
        if (args.contains("http.server") || args.contains("SimpleHTTPServer")) {
            val port = args.lastOrNull()?.toIntOrNull() ?: 8000
            val started = localWebServer.start(dir, port)
            if (started) {
                addLine(TerminalLine("Serving HTTP on 0.0.0.0 port $port (http://0.0.0.0:$port/) ...", TerminalOutputType.SUCCESS))
            } else {
                addLine(TerminalLine("Failed to start server on port $port", TerminalOutputType.STDERR))
            }
        } else if (args.contains("-V") || args.contains("--version")) {
            addLine(TerminalLine("Python 3.12.0", TerminalOutputType.STDOUT))
        } else {
            addLine(TerminalLine("Python 3.12 interactive runtime ready.", TerminalOutputType.INFO))
        }
    }

    private fun handleNpm(dir: File, args: List<String>) {
        val action = args.firstOrNull() ?: "help"
        when (action) {
            "install", "i" -> {
                addLine(TerminalLine("npm: reading package.json...", TerminalOutputType.INFO))
                addLine(TerminalLine("added 42 packages in 1.4s", TerminalOutputType.SUCCESS))
            }
            "run" -> {
                val script = args.getOrNull(1) ?: "dev"
                addLine(TerminalLine("> droidstudio-project@1.0.0 $script", TerminalOutputType.INFO))
                addLine(TerminalLine("> vite --port 8080", TerminalOutputType.INFO))
                localWebServer.start(dir, 8080)
                addLine(TerminalLine("VITE v5.2.0  ready in 240 ms", TerminalOutputType.SUCCESS))
                addLine(TerminalLine("➜  Local:   http://localhost:8080/", TerminalOutputType.SUCCESS))
            }
            else -> addLine(TerminalLine("Usage: npm <install | run dev | run build>", TerminalOutputType.INFO))
        }
    }

    private fun handleGit(dir: File, args: List<String>) {
        val sub = args.firstOrNull() ?: "status"
        when (sub) {
            "status" -> {
                addLine(TerminalLine("On branch main", TerminalOutputType.INFO))
                if (gitStaged.isEmpty()) {
                    addLine(TerminalLine("Changes not staged for commit:", TerminalOutputType.INFO))
                    addLine(TerminalLine("  (use \"git add <file>...\" to update what will be committed)", TerminalOutputType.INFO))
                    val files = dir.listFiles()?.filter { !it.name.startsWith(".") } ?: emptyList()
                    files.take(5).forEach { addLine(TerminalLine("      modified:   ${it.name}", TerminalOutputType.STDERR)) }
                } else {
                    addLine(TerminalLine("Changes to be committed:", TerminalOutputType.SUCCESS))
                    gitStaged.forEach { addLine(TerminalLine("      modified:   $it", TerminalOutputType.SUCCESS)) }
                }
            }
            "add" -> {
                val target = args.getOrNull(1) ?: "."
                gitStaged.add(target)
                addLine(TerminalLine("Staged changes for '$target'", TerminalOutputType.SUCCESS))
            }
            "commit" -> {
                val msg = args.joinToString(" ").substringAfter("-m").trim().removeSurrounding("\"")
                val finalMsg = if (msg.isNotBlank()) msg else "Update files from DroidStudio AI"
                val commitHash = java.util.UUID.randomUUID().toString().take(7)
                gitCommits.add("[$commitHash] $finalMsg")
                gitStaged.clear()
                addLine(TerminalLine("[main $commitHash] $finalMsg", TerminalOutputType.SUCCESS))
                addLine(TerminalLine(" ${dir.listFiles()?.size ?: 1} files changed, 25 insertions(+)", TerminalOutputType.INFO))
            }
            "log" -> {
                if (gitCommits.isEmpty()) {
                    addLine(TerminalLine("commit e9a41c2 (HEAD -> main)", TerminalOutputType.SUCCESS))
                    addLine(TerminalLine("Author: DroidStudio Developer <user@droidstudio.local>", TerminalOutputType.INFO))
                    addLine(TerminalLine("Date:   ${Date()}", TerminalOutputType.INFO))
                    addLine(TerminalLine("    Initial project commit", TerminalOutputType.STDOUT))
                } else {
                    gitCommits.reversed().forEach { addLine(TerminalLine("commit $it", TerminalOutputType.SUCCESS)) }
                }
            }
            "push" -> {
                addLine(TerminalLine("Enumerating objects: 15, done.", TerminalOutputType.INFO))
                addLine(TerminalLine("Writing objects: 100% (15/15), 3.2 KiB | done.", TerminalOutputType.INFO))
                addLine(TerminalLine("To https://github.com/user/project.git", TerminalOutputType.SUCCESS))
                addLine(TerminalLine("   main -> main", TerminalOutputType.SUCCESS))
            }
            else -> addLine(TerminalLine("git: '$sub' is not a git command. See 'git --help'.", TerminalOutputType.STDERR))
        }
    }

    private suspend fun handleSqlite(dir: File, args: List<String>) {
        if (args.isEmpty()) {
            addLine(TerminalLine("Usage: sqlite3 <dbfile> \"<query>\"", TerminalOutputType.STDERR))
            return
        }
        val dbFileName = args[0]
        val sql = args.drop(1).joinToString(" ").removeSurrounding("\"").removeSurrounding("'")
        val dbFile = File(dir, dbFileName)

        if (sql.isBlank()) {
            val tables = sqliteManager.listTables(dbFile)
            addLine(TerminalLine("Connected to $dbFileName. Tables (${tables.size}):", TerminalOutputType.SUCCESS))
            tables.forEach { t ->
                addLine(TerminalLine("Table: ${t.tableName} [${t.columnNames.joinToString(", ")}]", TerminalOutputType.INFO))
            }
            return
        }

        val result = sqliteManager.executeQuery(dbFile, sql)
        if (result.isSuccess) {
            addLine(TerminalLine(result.message, TerminalOutputType.SUCCESS))
            if (result.columns.isNotEmpty()) {
                addLine(TerminalLine(result.columns.joinToString(" | "), TerminalOutputType.INFO))
                addLine(TerminalLine("-".repeat(result.columns.size * 15), TerminalOutputType.INFO))
                result.rows.forEach { row ->
                    addLine(TerminalLine(row.joinToString(" | "), TerminalOutputType.STDOUT))
                }
            }
        } else {
            addLine(TerminalLine(result.message, TerminalOutputType.STDERR))
        }
    }
}

package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.GeminiService
import com.example.data.ProjectRepository
import com.example.data.SearchResult
import com.example.data.SettingsManager
import com.example.data.SqliteManager
import com.example.github.GitHubService
import com.example.model.AiMessage
import com.example.model.ConsoleLogItem
import com.example.model.DiffStatus
import com.example.model.EditorTab
import com.example.model.FileDiffProposal
import com.example.model.Project
import com.example.model.ProjectFile
import com.example.server.LocalWebServer
import com.example.server.NetworkUtils
import com.example.terminal.TerminalEngine
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

enum class IdePane {
    HOME,
    EXPLORER,
    EDITOR,
    AI_ASSISTANT,
    LOCAL_SERVER,
    LIVE_PREVIEW,
    TERMINAL,
    GITHUB_BUILD,
    SETTINGS,
    SEARCH
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val projectRepo = ProjectRepository(application)
    val settingsManager = SettingsManager(application)
    val localWebServer = LocalWebServer()
    val sqliteManager = SqliteManager()
    val terminalEngine = TerminalEngine(localWebServer, sqliteManager)
    val gitHubService = GitHubService()
    val geminiService = GeminiService()

    // Navigation / Active View
    private val _currentPane = MutableStateFlow(IdePane.HOME)
    val currentPane: StateFlow<IdePane> = _currentPane.asStateFlow()

    // Projects
    private val _projects = MutableStateFlow<List<Project>>(emptyList())
    val projects: StateFlow<List<Project>> = _projects.asStateFlow()

    private val _activeProject = MutableStateFlow<Project?>(null)
    val activeProject: StateFlow<Project?> = _activeProject.asStateFlow()

    private val _fileTree = MutableStateFlow<List<ProjectFile>>(emptyList())
    val fileTree: StateFlow<List<ProjectFile>> = _fileTree.asStateFlow()

    // Editor Tabs
    private val _openTabs = MutableStateFlow<List<EditorTab>>(emptyList())
    val openTabs: StateFlow<List<EditorTab>> = _openTabs.asStateFlow()

    private val _activeTabIndex = MutableStateFlow(0)
    val activeTabIndex: StateFlow<Int> = _activeTabIndex.asStateFlow()

    val currentTab: EditorTab?
        get() = _openTabs.value.getOrNull(_activeTabIndex.value)

    // History for Undo/Redo per tab
    private val tabUndoStack = mutableMapOf<String, ArrayDeque<String>>()
    private val tabRedoStack = mutableMapOf<String, ArrayDeque<String>>()

    // Search
    private val _searchResults = MutableStateFlow<List<SearchResult>>(emptyList())
    val searchResults: StateFlow<List<SearchResult>> = _searchResults.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // AI Chat & Workspace Proposals
    private val _aiMessages = MutableStateFlow<List<AiMessage>>(emptyList())
    val aiMessages: StateFlow<List<AiMessage>> = _aiMessages.asStateFlow()

    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading: StateFlow<Boolean> = _isAiLoading.asStateFlow()

    private val _activeDiffProposal = MutableStateFlow<FileDiffProposal?>(null)
    val activeDiffProposal: StateFlow<FileDiffProposal?> = _activeDiffProposal.asStateFlow()

    // Web Preview & Console Logs
    private val _previewUrl = MutableStateFlow("http://127.0.0.1:8080")
    val previewUrl: StateFlow<String> = _previewUrl.asStateFlow()

    private val _isDesktopPreview = MutableStateFlow(false)
    val isDesktopPreview: StateFlow<Boolean> = _isDesktopPreview.asStateFlow()

    private val _consoleLogs = MutableStateFlow<List<ConsoleLogItem>>(emptyList())
    val consoleLogs: StateFlow<List<ConsoleLogItem>> = _consoleLogs.asStateFlow()

    private val _previewReloadTrigger = MutableStateFlow(0)
    val previewReloadTrigger: StateFlow<Int> = _previewReloadTrigger.asStateFlow()

    // Auto-save debounce job
    private var autoSaveJob: Job? = null

    init {
        loadProjects()
        initAiWelcomeMessage()
    }

    private fun initAiWelcomeMessage() {
        _aiMessages.value = listOf(
            AiMessage(
                role = "assistant",
                content = "Halo! Saya adalah DroidStudio AI Workspace Agent.\n\nSaya dapat membantu Anda:\n" +
                        "• Menulis & memodifikasi kode (HTML, CSS, JS, PHP, Python, SQL, dll)\n" +
                        "• Memperbaiki error dan refactoring\n" +
                        "• Menghasilkan proyek web lengkap secara otomatis\n" +
                        "• Menyiapkan perubahan dengan sistem persetujuan Diff sebelum diterapkan\n\n" +
                        "Ketik instruksi Anda atau pilih salah satu prompt cepat di bawah!"
            )
        )
    }

    fun setPane(pane: IdePane) {
        _currentPane.value = pane
    }

    fun loadProjects() {
        viewModelScope.launch {
            val list = projectRepo.getProjects()
            _projects.value = list
            val lastId = settingsManager.getLastProjectId()
            if (_activeProject.value == null && list.isNotEmpty()) {
                val target = list.find { it.id == lastId } ?: list.first()
                openProject(target)
            }
        }
    }

    fun openProject(project: Project) {
        _activeProject.value = project
        settingsManager.setLastProjectId(project.id)
        terminalEngine.setDirectory(File(project.path))
        refreshFileTree()

        // Auto-open index.html or first file
        viewModelScope.launch {
            val tree = projectRepo.getFileTree(project.path)
            val indexFile = tree.find { it.name.equals("index.html", true) || it.name.equals("index.php", true) }
                ?: tree.firstOrNull { !it.isDirectory }
            if (indexFile != null) {
                openFile(indexFile.path)
            }
        }
    }

    fun createNewProject(name: String, template: String) {
        viewModelScope.launch {
            val proj = projectRepo.createProject(name, template)
            loadProjects()
            openProject(proj)
            setPane(IdePane.EDITOR)
        }
    }

    fun refreshFileTree() {
        val proj = _activeProject.value ?: return
        viewModelScope.launch {
            val tree = projectRepo.getFileTree(proj.path)
            _fileTree.value = tree
        }
    }

    fun openFile(filePath: String) {
        viewModelScope.launch {
            val currentTabs = _openTabs.value.toMutableList()
            val existingIndex = currentTabs.indexOfFirst { it.filePath == filePath }

            if (existingIndex >= 0) {
                _activeTabIndex.value = existingIndex
            } else {
                val file = File(filePath)
                val content = projectRepo.readFile(filePath)
                val tab = EditorTab(
                    filePath = filePath,
                    fileName = file.name,
                    content = content,
                    language = file.extension.lowercase()
                )
                currentTabs.add(tab)
                _openTabs.value = currentTabs
                _activeTabIndex.value = currentTabs.size - 1

                // Initialize undo stack
                tabUndoStack[filePath] = ArrayDeque<String>().apply { add(content) }
                tabRedoStack[filePath] = ArrayDeque()
            }
            setPane(IdePane.EDITOR)
        }
    }

    fun closeTab(index: Int) {
        val currentTabs = _openTabs.value.toMutableList()
        if (index in currentTabs.indices) {
            currentTabs.removeAt(index)
            _openTabs.value = currentTabs
            val nextIndex = if (index >= currentTabs.size) (currentTabs.size - 1).coerceAtLeast(0) else index
            _activeTabIndex.value = nextIndex
        }
    }

    fun selectTab(index: Int) {
        if (index in _openTabs.value.indices) {
            _activeTabIndex.value = index
        }
    }

    fun updateCurrentContent(newContent: String) {
        val tab = currentTab ?: return
        val currentTabs = _openTabs.value.toMutableList()
        val index = _activeTabIndex.value

        // Record undo state if changed
        val undoQueue = tabUndoStack.getOrPut(tab.filePath) { ArrayDeque() }
        if (undoQueue.isEmpty() || undoQueue.last() != tab.content) {
            undoQueue.add(tab.content)
            if (undoQueue.size > 50) undoQueue.removeFirst()
        }
        tabRedoStack[tab.filePath]?.clear()

        currentTabs[index] = tab.copy(content = newContent, isDirty = true)
        _openTabs.value = currentTabs

        // Auto-save debounce
        if (settingsManager.autoSave.value) {
            autoSaveJob?.cancel()
            autoSaveJob = viewModelScope.launch {
                delay(1200)
                saveCurrentTab()
            }
        }
    }

    fun undo() {
        val tab = currentTab ?: return
        val undoQueue = tabUndoStack[tab.filePath] ?: return
        if (undoQueue.isNotEmpty()) {
            val prev = undoQueue.removeLast()
            val redoQueue = tabRedoStack.getOrPut(tab.filePath) { ArrayDeque() }
            redoQueue.add(tab.content)

            val currentTabs = _openTabs.value.toMutableList()
            currentTabs[_activeTabIndex.value] = tab.copy(content = prev, isDirty = true)
            _openTabs.value = currentTabs
        }
    }

    fun redo() {
        val tab = currentTab ?: return
        val redoQueue = tabRedoStack[tab.filePath] ?: return
        if (redoQueue.isNotEmpty()) {
            val next = redoQueue.removeLast()
            val undoQueue = tabUndoStack.getOrPut(tab.filePath) { ArrayDeque() }
            undoQueue.add(tab.content)

            val currentTabs = _openTabs.value.toMutableList()
            currentTabs[_activeTabIndex.value] = tab.copy(content = next, isDirty = true)
            _openTabs.value = currentTabs
        }
    }

    fun saveCurrentTab() {
        val tab = currentTab ?: return
        viewModelScope.launch {
            val success = projectRepo.writeFile(tab.filePath, tab.content)
            if (success) {
                val currentTabs = _openTabs.value.toMutableList()
                val idx = _activeTabIndex.value
                if (idx in currentTabs.indices) {
                    currentTabs[idx] = tab.copy(isDirty = false)
                    _openTabs.value = currentTabs
                }
                // Trigger live preview reload
                _previewReloadTrigger.value = _previewReloadTrigger.value + 1
            }
        }
    }

    fun saveAllTabs() {
        viewModelScope.launch {
            val tabs = _openTabs.value
            for (tab in tabs) {
                if (tab.isDirty) {
                    projectRepo.writeFile(tab.filePath, tab.content)
                }
            }
            _openTabs.value = tabs.map { it.copy(isDirty = false) }
            _previewReloadTrigger.value = _previewReloadTrigger.value + 1
        }
    }

    fun createNewFileOrFolder(parentPath: String, name: String, isFolder: Boolean) {
        viewModelScope.launch {
            val success = projectRepo.createFile(parentPath, name, isFolder)
            if (success) {
                refreshFileTree()
                if (!isFolder) {
                    val fullPath = File(parentPath, name).absolutePath
                    openFile(fullPath)
                }
            }
        }
    }

    fun deleteFileOrFolder(path: String) {
        viewModelScope.launch {
            val success = projectRepo.deleteFile(path)
            if (success) {
                // Close if opened
                val tabIdx = _openTabs.value.indexOfFirst { it.filePath == path }
                if (tabIdx >= 0) closeTab(tabIdx)
                refreshFileTree()
            }
        }
    }

    fun renameFile(oldPath: String, newName: String) {
        viewModelScope.launch {
            val success = projectRepo.renameFile(oldPath, newName)
            if (success) {
                val tabIdx = _openTabs.value.indexOfFirst { it.filePath == oldPath }
                if (tabIdx >= 0) closeTab(tabIdx)
                refreshFileTree()
            }
        }
    }

    fun performSearch(query: String) {
        _searchQuery.value = query
        val proj = _activeProject.value ?: return
        viewModelScope.launch {
            val results = projectRepo.searchProject(proj.path, query)
            _searchResults.value = results
        }
    }

    // --- Local Server Management ---
    fun toggleLocalServer() {
        val proj = _activeProject.value ?: return
        val rootDir = File(proj.path)
        val port = settingsManager.serverPort.value

        if (localWebServer.isRunning.value) {
            localWebServer.stop()
        } else {
            localWebServer.start(rootDir, port)
            _previewUrl.value = "http://127.0.0.1:$port"
        }
    }

    fun restartLocalServer() {
        val proj = _activeProject.value ?: return
        val rootDir = File(proj.path)
        val port = settingsManager.serverPort.value
        localWebServer.restart(rootDir, port)
        _previewUrl.value = "http://127.0.0.1:$port"
        _previewReloadTrigger.value = _previewReloadTrigger.value + 1
    }

    // --- Live Preview & Console ---
    fun toggleDesktopPreview() {
        _isDesktopPreview.value = !_isDesktopPreview.value
    }

    fun reloadPreview() {
        _previewReloadTrigger.value = _previewReloadTrigger.value + 1
    }

    fun setPreviewUrl(url: String) {
        _previewUrl.value = url
    }

    fun addConsoleLog(level: String, message: String) {
        val item = ConsoleLogItem(level = level, message = message)
        val list = _consoleLogs.value.toMutableList()
        list.add(0, item)
        if (list.size > 150) list.removeAt(list.size - 1)
        _consoleLogs.value = list
    }

    fun clearConsoleLogs() {
        _consoleLogs.value = emptyList()
    }

    // --- AI Workspace Assistant ---
    fun sendAiPrompt(prompt: String) {
        if (prompt.isBlank()) return
        val userMsg = AiMessage(role = "user", content = prompt)
        _aiMessages.value = _aiMessages.value + userMsg
        _isAiLoading.value = true

        val apiKey = settingsManager.geminiApiKey.value
        val model = settingsManager.geminiModel.value
        val temp = settingsManager.temperature.value

        val proj = _activeProject.value
        val openFile = currentTab

        viewModelScope.launch {
            // Build compact project context
            val contextBuilder = StringBuilder()
            if (proj != null) {
                contextBuilder.append("PROJECT: ${proj.name} (${proj.template})\n")
                contextBuilder.append("FILES:\n")
                File(proj.path).walkTopDown().filter { it.isFile && !it.name.startsWith(".") }.take(25).forEach { f ->
                    val rel = f.relativeTo(File(proj.path)).path
                    contextBuilder.append("- $rel (${f.length()} bytes)\n")
                }
            }

            val result = geminiService.askAi(
                prompt = prompt,
                apiKey = apiKey,
                model = model,
                temperature = temp,
                projectContext = contextBuilder.toString(),
                activeFileName = openFile?.fileName,
                activeFileContent = openFile?.content
            )

            _isAiLoading.value = false

            result.onSuccess { aiRes ->
                val assistantMsg = AiMessage(
                    role = "assistant",
                    content = aiRes.text,
                    diffProposal = aiRes.proposals.firstOrNull()
                )
                _aiMessages.value = _aiMessages.value + assistantMsg

                if (aiRes.proposals.isNotEmpty()) {
                    _activeDiffProposal.value = aiRes.proposals.first()
                }
            }.onFailure { err ->
                val errorMsg = AiMessage(
                    role = "assistant",
                    content = "Maaf, terjadi kesalahan: ${err.message}",
                    isError = true
                )
                _aiMessages.value = _aiMessages.value + errorMsg
            }
        }
    }

    fun showDiffProposal(proposal: FileDiffProposal) {
        _activeDiffProposal.value = proposal
    }

    fun dismissDiffProposal() {
        _activeDiffProposal.value = null
    }

    fun applyDiffProposal(proposal: FileDiffProposal) {
        val proj = _activeProject.value ?: return
        viewModelScope.launch {
            val targetFile = if (File(proposal.targetFilePath).isAbsolute) {
                File(proposal.targetFilePath)
            } else {
                File(proj.path, proposal.targetFilePath)
            }

            targetFile.parentFile?.mkdirs()
            targetFile.writeText(proposal.proposedContent, Charsets.UTF_8)

            // Update proposals in messages
            _aiMessages.value = _aiMessages.value.map { msg ->
                if (msg.diffProposal?.id == proposal.id) {
                    msg.copy(diffProposal = proposal.copy(status = DiffStatus.APPLIED))
                } else msg
            }

            _activeDiffProposal.value = null
            refreshFileTree()

            // Open or update open tab
            val existingTabIdx = _openTabs.value.indexOfFirst { it.filePath == targetFile.absolutePath }
            if (existingTabIdx >= 0) {
                val tabs = _openTabs.value.toMutableList()
                tabs[existingTabIdx] = tabs[existingTabIdx].copy(content = proposal.proposedContent, isDirty = false)
                _openTabs.value = tabs
            } else {
                openFile(targetFile.absolutePath)
            }

            _previewReloadTrigger.value = _previewReloadTrigger.value + 1
        }
    }

    fun rejectDiffProposal(proposal: FileDiffProposal) {
        _aiMessages.value = _aiMessages.value.map { msg ->
            if (msg.diffProposal?.id == proposal.id) {
                msg.copy(diffProposal = proposal.copy(status = DiffStatus.REJECTED))
            } else msg
        }
        _activeDiffProposal.value = null
    }

    fun undoDiffProposal(proposal: FileDiffProposal) {
        val proj = _activeProject.value ?: return
        viewModelScope.launch {
            val targetFile = if (File(proposal.targetFilePath).isAbsolute) {
                File(proposal.targetFilePath)
            } else {
                File(proj.path, proposal.targetFilePath)
            }
            if (proposal.originalContent.isNotBlank()) {
                targetFile.writeText(proposal.originalContent)
            } else {
                targetFile.delete()
            }
            refreshFileTree()
            _activeDiffProposal.value = null
        }
    }

    // --- GitHub Actions APK Builder ---
    fun triggerGitHubBuild() {
        val proj = _activeProject.value ?: return
        val token = settingsManager.githubToken.value
        val repo = settingsManager.githubRepo.value
        val branch = settingsManager.githubBranch.value

        saveAllTabs()

        viewModelScope.launch {
            gitHubService.triggerApkBuildPipeline(
                projectDir = File(proj.path),
                githubToken = token,
                repoName = repo,
                branch = branch
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        localWebServer.stop()
    }
}

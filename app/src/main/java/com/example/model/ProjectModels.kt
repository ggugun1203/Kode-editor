package com.example.model

import java.io.File

data class Project(
    val id: String,
    val name: String,
    val path: String,
    val description: String = "",
    val template: String = "Web",
    val lastModified: Long = System.currentTimeMillis(),
    val fileCount: Int = 0
)

data class ProjectFile(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val extension: String = "",
    val size: Long = 0L,
    val lastModified: Long = 0L,
    val children: List<ProjectFile> = emptyList(),
    val isExpanded: Boolean = false
) {
    val file: File get() = File(path)
}

data class EditorTab(
    val filePath: String,
    val fileName: String,
    val content: String = "",
    val isDirty: Boolean = false,
    val cursorPosition: Int = 0,
    val language: String = "plaintext"
)

enum class DiffStatus {
    PENDING,
    APPLIED,
    REJECTED
}

data class FileDiffProposal(
    val id: String = java.util.UUID.randomUUID().toString(),
    val targetFilePath: String,
    val originalContent: String,
    val proposedContent: String,
    val summary: String,
    val status: DiffStatus = DiffStatus.PENDING
)

data class AiMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val role: String, // "user", "assistant", "system"
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val diffProposal: FileDiffProposal? = null,
    val isError: Boolean = false
)

data class ServerLogEntry(
    val id: Long = System.nanoTime(),
    val timestamp: String,
    val method: String,
    val path: String,
    val statusCode: Int,
    val clientIp: String,
    val durationMs: Long
)

enum class TerminalOutputType {
    CMD,
    STDOUT,
    STDERR,
    INFO,
    SUCCESS
}

data class TerminalLine(
    val text: String,
    val type: TerminalOutputType = TerminalOutputType.STDOUT
)

enum class BuildStatus {
    IDLE,
    PREPARING,
    CHECKING_CONFIG,
    SYNCING_GITHUB,
    TRIGGERING_WORKFLOW,
    BUILDING,
    SUCCESS,
    FAILED
}

data class BuildStep(
    val name: String,
    val status: BuildStatus,
    val message: String = ""
)

data class ConsoleLogItem(
    val level: String, // "LOG", "WARN", "ERROR", "INFO"
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)

package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BuildStatus
import com.example.ui.theme.BlueAccent
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.OrangeAccent
import com.example.ui.theme.PurpleAccent
import com.example.ui.theme.RedAccent
import com.example.ui.theme.VsCodeBg
import com.example.ui.theme.VsCodeHeaderBg
import com.example.ui.theme.VsCodeSidebarBg
import com.example.viewmodel.IdePane
import com.example.viewmodel.MainViewModel

@Composable
fun GitHubBuildPane(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val buildStatus by viewModel.gitHubService.buildStatus.collectAsState()
    val buildLogs by viewModel.gitHubService.buildLogs.collectAsState()
    val artifactUrl by viewModel.gitHubService.artifactUrl.collectAsState()
    val lastError by viewModel.gitHubService.lastErrorMessage.collectAsState()

    val githubToken by viewModel.settingsManager.githubToken.collectAsState()
    val githubRepo by viewModel.settingsManager.githubRepo.collectAsState()
    val githubBranch by viewModel.settingsManager.githubBranch.collectAsState()
    val activeProject by viewModel.activeProject.collectAsState()

    val isBuilding = buildStatus != BuildStatus.IDLE &&
            buildStatus != BuildStatus.SUCCESS &&
            buildStatus != BuildStatus.FAILED

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(VsCodeBg)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // GitHub Repository Header Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = VsCodeSidebarBg)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CloudUpload,
                                contentDescription = null,
                                tint = CyanAccent,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "GitHub Integration",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = if (githubRepo.isNotBlank()) "$githubRepo [$githubBranch]" else "Repository: Belum diatur",
                                    fontSize = 12.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        }

                        IconButton(
                            onClick = { viewModel.setPane(IdePane.SETTINGS) },
                            modifier = Modifier.testTag("github_settings_btn")
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.LightGray)
                        }
                    }

                    if (githubToken.isBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            color = Color(0xFF1E293B),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "Mode: Cloud Builder Standar (Token GitHub opsional). Anda tetap dapat membuat APK!",
                                fontSize = 11.sp,
                                color = Color(0xFF7DD3FC),
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                }
            }
        }

        // Dedicated Big Build Button
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Automated Cloud APK Builder",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Kompilasi source code proyek menjadi file APK Android via GitHub Actions tanpa memerlukan komputer.",
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8),
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = { viewModel.triggerGitHubBuild() },
                        enabled = !isBuilding,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (buildStatus == BuildStatus.SUCCESS) EmeraldGreen else Color(0xFF0284C7)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("build_apk_button")
                    ) {
                        if (isBuilding) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("SEDANG MEMBANGUN APK...", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        } else {
                            Icon(Icons.Default.CloudUpload, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("BUILD APK VIA GITHUB", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }

                    // Download APK button if finished
                    if (buildStatus == BuildStatus.SUCCESS && artifactUrl != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(artifactUrl))
                                context.startActivity(intent)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("download_apk_btn")
                        ) {
                            Icon(Icons.Default.CloudDownload, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("UNDUH FILE APK SEKARANG", fontWeight = FontWeight.Bold)
                        }
                    }

                    // Fix with AI if failed
                    if (buildStatus == BuildStatus.FAILED) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = {
                                val logSummary = buildLogs.takeLast(15).joinToString("\n")
                                viewModel.sendAiPrompt("Proses Build APK gagal dengan log error berikut:\n```\n$logSummary\n```\nAnalisis penyebab kegagalan dan berikan solusi perbaikannya.")
                                viewModel.setPane(IdePane.AI_ASSISTANT)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PurpleAccent),
                            modifier = Modifier.fillMaxWidth().testTag("ai_fix_build_btn")
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Minta AI Perbaiki Error Build")
                        }
                    }
                }
            }
        }

        // Live Build Stage Progress
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = VsCodeSidebarBg)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Tahapan Proses (Pipeline Stages)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    StageRow(label = "1. Preparing Project & Files", isDone = buildStatus.ordinal >= BuildStatus.PREPARING.ordinal && buildStatus != BuildStatus.IDLE)
                    StageRow(label = "2. Checking Android SDK & Gradle", isDone = buildStatus.ordinal >= BuildStatus.CHECKING_CONFIG.ordinal)
                    StageRow(label = "3. Syncing to GitHub Repository", isDone = buildStatus.ordinal >= BuildStatus.SYNCING_GITHUB.ordinal)
                    StageRow(label = "4. Triggering GitHub Actions Workflow", isDone = buildStatus.ordinal >= BuildStatus.TRIGGERING_WORKFLOW.ordinal)
                    StageRow(label = "5. Compiling Debug APK on Cloud", isDone = buildStatus.ordinal >= BuildStatus.BUILDING.ordinal)
                    StageRow(label = "6. Ready: Artifact Uploaded", isDone = buildStatus == BuildStatus.SUCCESS)
                }
            }
        }

        // Cloud Build Console Logs
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Build Output Console (${buildLogs.size})",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                if (buildLogs.isNotEmpty()) {
                    TextButton(onClick = { viewModel.gitHubService.reset() }) {
                        Text("Reset", fontSize = 11.sp, color = Color.Gray)
                    }
                }
            }
        }

        if (buildLogs.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Tekan 'BUILD APK VIA GITHUB' untuk memulai proses.", color = Color.Gray, fontSize = 12.sp)
                }
            }
        } else {
            items(buildLogs) { log ->
                val color = when {
                    log.contains("BERHASIL") || log.contains("SUCCESSFUL") -> EmeraldGreen
                    log.contains("Error") || log.contains("Gagal") || log.contains("FAILED") -> RedAccent
                    log.contains("✔") -> CyanAccent
                    else -> Color(0xFFCBD5E1)
                }
                Text(
                    text = log,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = color,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
private fun StageRow(label: String, isDone: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (isDone) Icons.Default.CheckCircle else Icons.Default.Sync,
            contentDescription = null,
            tint = if (isDone) EmeraldGreen else Color.DarkGray,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            color = if (isDone) Color.White else Color.Gray,
            fontWeight = if (isDone) FontWeight.Medium else FontWeight.Normal
        )
    }
}

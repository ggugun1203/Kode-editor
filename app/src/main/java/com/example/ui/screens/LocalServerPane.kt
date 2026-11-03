package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SqlQueryResult
import com.example.data.TableSchema
import com.example.model.ServerLogEntry
import com.example.server.NetworkUtils
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
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun LocalServerPane(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isRunning by viewModel.localWebServer.isRunning.collectAsState()
    val serverLogs by viewModel.localWebServer.serverLogs.collectAsState()
    val activeProject by viewModel.activeProject.collectAsState()
    val configuredPort by viewModel.settingsManager.serverPort.collectAsState()
    val runtimeMode by viewModel.localWebServer.runtimeMode.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Server & Logs, 1: SQLite Database Manager
    val localIp = remember { NetworkUtils.getLocalIpAddress() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(VsCodeBg)
    ) {
        // Top Tab Row: Server vs Database
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = VsCodeHeaderBg,
            contentColor = Color.White
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Dns, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Web Server (XAMPP)")
                    }
                }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Storage, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("SQLite Database")
                    }
                }
            )
        }

        if (selectedTab == 0) {
            // Server Controls & Logs
            ServerView(
                context = context,
                isRunning = isRunning,
                configuredPort = configuredPort,
                localIp = localIp,
                activeProjectName = activeProject?.name ?: "None",
                runtimeMode = runtimeMode,
                serverLogs = serverLogs,
                onToggleServer = { viewModel.toggleLocalServer() },
                onRestartServer = { viewModel.restartLocalServer() },
                onClearLogs = { viewModel.localWebServer.clearLogs() },
                onOpenPreview = { viewModel.setPane(IdePane.LIVE_PREVIEW) }
            )
        } else {
            // SQLite Database Explorer
            SqliteDatabaseView(
                viewModel = viewModel,
                activeProject = activeProject
            )
        }
    }
}

@Composable
private fun ServerView(
    context: Context,
    isRunning: Boolean,
    configuredPort: Int,
    localIp: String,
    activeProjectName: String,
    runtimeMode: String,
    serverLogs: List<ServerLogEntry>,
    onToggleServer: () -> Unit,
    onRestartServer: () -> Unit,
    onClearLogs: () -> Unit,
    onOpenPreview: () -> Unit
) {
    val localhostUrl = "http://127.0.0.1:$configuredPort"
    val wifiUrl = "http://$localIp:$configuredPort"

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Status Card
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
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .background(if (isRunning) EmeraldGreen else RedAccent, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isRunning) "SERVER BERJALAN (LIVE)" else "SERVER BERHENTI (OFFLINE)",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isRunning) EmeraldGreen else Color.LightGray
                            )
                        }

                        Surface(
                            color = Color(0xFF0F172A),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = runtimeMode,
                                fontSize = 11.sp,
                                color = CyanAccent,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Action Buttons (Start/Stop, Restart, Open Preview)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onToggleServer,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isRunning) RedAccent else EmeraldGreen
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("server_power_btn")
                        ) {
                            Icon(
                                imageVector = if (isRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isRunning) "Stop" else "Start Server")
                        }

                        if (isRunning) {
                            Button(
                                onClick = onRestartServer,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                                modifier = Modifier.testTag("server_restart_btn")
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            }

                            Button(
                                onClick = onOpenPreview,
                                colors = ButtonDefaults.buttonColors(containerColor = PurpleAccent),
                                modifier = Modifier.weight(1.2f).testTag("server_open_preview_btn")
                            ) {
                                Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Live Preview")
                            }
                        }
                    }
                }
            }
        }

        // URL Endpoints Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = VsCodeSidebarBg)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Alamat Akses Website",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    // Localhost URL
                    UrlRow(
                        label = "Internal Android (Localhost):",
                        url = localhostUrl,
                        onCopy = {
                            copyToClipboard(context, localhostUrl)
                        }
                    )

                    // Wi-Fi Local IP
                    UrlRow(
                        label = "Jaringan Wi-Fi (Akses dari Laptop/HP lain):",
                        url = wifiUrl,
                        icon = Icons.Default.Wifi,
                        onCopy = {
                            copyToClipboard(context, wifiUrl)
                        }
                    )

                    Text(
                        text = "Root Folder: $activeProjectName/ (Port: $configuredPort)",
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
            }
        }

        // Live Server Request Logs
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Server Logs (${serverLogs.size})",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                IconButton(onClick = onClearLogs, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Clear Logs", tint = Color.Gray, modifier = Modifier.size(16.dp))
                }
            }
        }

        if (serverLogs.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Belum ada request. Buka Live Preview untuk memuat situs.", color = Color.Gray, fontSize = 12.sp)
                }
            }
        } else {
            items(serverLogs) { log ->
                ServerLogRow(log = log)
            }
        }
    }
}

@Composable
private fun UrlRow(
    label: String,
    url: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector = Icons.Default.Dns,
    onCopy: () -> Unit
) {
    Column {
        Text(text = label, fontSize = 11.sp, color = Color(0xFF94A3B8))
        Spacer(modifier = Modifier.height(2.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0F172A), RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = url,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color.White
                )
            }
            IconButton(onClick = onCopy, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = CyanAccent, modifier = Modifier.size(14.dp))
            }
        }
    }
}

@Composable
private fun ServerLogRow(log: ServerLogEntry) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(6.dp),
        colors = CardDefaults.cardColors(containerColor = VsCodeSidebarBg)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                // Method badge
                Surface(
                    color = when (log.method) {
                        "GET" -> Color(0xFF0284C7)
                        "POST" -> Color(0xFF10B981)
                        "ERROR" -> RedAccent
                        else -> Color(0xFF475569)
                    },
                    shape = RoundedCornerShape(3.dp)
                ) {
                    Text(
                        text = log.method,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                Text(
                    text = log.path,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFFE2E8F0)
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = when {
                        log.statusCode in 200..299 -> EmeraldGreen.copy(alpha = 0.2f)
                        log.statusCode in 400..499 -> OrangeAccent.copy(alpha = 0.2f)
                        else -> RedAccent.copy(alpha = 0.2f)
                    },
                    shape = RoundedCornerShape(3.dp)
                ) {
                    Text(
                        text = "${log.statusCode}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            log.statusCode in 200..299 -> EmeraldGreen
                            log.statusCode in 400..499 -> OrangeAccent
                            else -> RedAccent
                        },
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = log.timestamp,
                    fontSize = 10.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
private fun SqliteDatabaseView(
    viewModel: MainViewModel,
    activeProject: com.example.model.Project?
) {
    val coroutineScope = rememberCoroutineScope()
    var sqlInput by remember { mutableStateOf("SELECT * FROM products LIMIT 10;") }
    var queryResult by remember { mutableStateOf<SqlQueryResult?>(null) }
    var tableSchemas by remember { mutableStateOf<List<TableSchema>>(emptyList()) }

    val defaultDbFile = remember(activeProject?.path) {
        val root = if (activeProject != null) File(activeProject.path) else File(".")
        File(root, "database/app.db")
    }

    androidx.compose.runtime.LaunchedEffect(activeProject?.path) {
        tableSchemas = viewModel.sqliteManager.listTables(defaultDbFile)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = VsCodeSidebarBg)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "SQLite Query Executor",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color.White
                    )
                    Text(
                        text = "Database: database/app.db (Atau tabel proyek)",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )

                    OutlinedTextField(
                        value = sqlInput,
                        onValueChange = { sqlInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 4,
                        textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = Color.White),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanAccent,
                            unfocusedBorderColor = Color.DarkGray
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    val res = viewModel.sqliteManager.executeQuery(defaultDbFile, sqlInput)
                                    queryResult = res
                                    tableSchemas = viewModel.sqliteManager.listTables(defaultDbFile)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
                            modifier = Modifier.testTag("run_sql_query_btn")
                        ) {
                            Icon(Icons.Default.PlayArrow, null, tint = Color.Black, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Jalankan Query", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        queryResult?.let { res ->
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = if (res.isSuccess) Color(0xFF0F172A) else Color(0xFF450A0A))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = res.message,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (res.isSuccess) EmeraldGreen else RedAccent
                        )

                        if (res.columns.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = res.columns.joinToString(" | "),
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = CyanAccent,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            res.rows.forEach { row ->
                                Text(
                                    text = row.joinToString(" | "),
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("URL", text))
    Toast.makeText(context, "URL disalin: $text", Toast.LENGTH_SHORT).show()
}

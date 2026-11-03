package com.example.ui.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BlueAccent
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.PurpleAccent
import com.example.ui.theme.RedAccent
import com.example.ui.theme.VsCodeBg
import com.example.ui.theme.VsCodeHeaderBg
import com.example.ui.theme.VsCodeSidebarBg
import com.example.viewmodel.IdePane
import com.example.viewmodel.MainViewModel
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val geminiKey by viewModel.settingsManager.geminiApiKey.collectAsState()
    val geminiModel by viewModel.settingsManager.geminiModel.collectAsState()
    val temperature by viewModel.settingsManager.temperature.collectAsState()
    val githubToken by viewModel.settingsManager.githubToken.collectAsState()
    val githubRepo by viewModel.settingsManager.githubRepo.collectAsState()
    val githubBranch by viewModel.settingsManager.githubBranch.collectAsState()
    val serverPort by viewModel.settingsManager.serverPort.collectAsState()
    val autoSave by viewModel.settingsManager.autoSave.collectAsState()
    val fontSize by viewModel.settingsManager.fontSize.collectAsState()
    val editorTheme by viewModel.settingsManager.editorTheme.collectAsState()

    var keyInput by remember(geminiKey) { mutableStateOf(geminiKey) }
    var showKeyPassword by remember { mutableStateOf(false) }
    var isTestingConnection by remember { mutableStateOf(false) }
    var connectionTestResult by remember { mutableStateOf<String?>(null) }

    var tokenInput by remember(githubToken) { mutableStateOf(githubToken) }
    var showTokenPassword by remember { mutableStateOf(false) }
    var repoInput by remember(githubRepo) { mutableStateOf(githubRepo) }
    var branchInput by remember(githubBranch) { mutableStateOf(githubBranch) }
    var isTestingToken by remember { mutableStateOf(false) }
    var tokenTestResult by remember { mutableStateOf<String?>(null) }

    var showModelDropdown by remember { mutableStateOf(false) }
    val availableModels = listOf("gemini-2.5-flash", "gemini-1.5-flash", "gemini-1.5-pro")

    var showThemeDropdown by remember { mutableStateOf(false) }
    val availableThemes = listOf("VS Code Dark+", "Monokai Pro", "One Dark Pro", "GitHub Dark")

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(VsCodeBg)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Settings Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { viewModel.setPane(IdePane.HOME) },
                    modifier = Modifier.size(32.dp).testTag("settings_back_btn")
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Pengaturan (Settings)",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        // Section 1: Google Gemini AI Configuration
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = VsCodeSidebarBg)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AutoAwesome, null, tint = PurpleAccent, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Google Gemini AI Settings",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Text(
                        text = "Masukkan Google Gemini API Key milik Anda. Kunci disimpan dengan enkripsi Android Keystore dan tidak dibagikan.",
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8)
                    )

                    // API Key Field
                    OutlinedTextField(
                        value = keyInput,
                        onValueChange = { keyInput = it },
                        label = { Text("Gemini API Key") },
                        singleLine = true,
                        visualTransformation = if (showKeyPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showKeyPassword = !showKeyPassword }) {
                                Icon(
                                    if (showKeyPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "Toggle visibility",
                                    tint = Color.Gray
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PurpleAccent,
                            unfocusedBorderColor = Color.DarkGray
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_gemini_key_input")
                    )

                    // Save, Remove, and Test Connection Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.settingsManager.saveGeminiApiKey(keyInput)
                                Toast.makeText(context, "API Key berhasil disimpan!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PurpleAccent),
                            modifier = Modifier.weight(1f).testTag("save_gemini_key_btn")
                        ) {
                            Text("Simpan Kunci")
                        }

                        OutlinedButton(
                            onClick = {
                                isTestingConnection = true
                                connectionTestResult = null
                                coroutineScope.launch {
                                    val res = viewModel.geminiService.testConnection(keyInput, geminiModel)
                                    isTestingConnection = false
                                    connectionTestResult = if (res.isSuccess) {
                                        res.getOrNull()
                                    } else {
                                        "Error: ${res.exceptionOrNull()?.message}"
                                    }
                                }
                            },
                            modifier = Modifier.weight(1.2f).testTag("test_gemini_conn_btn")
                        ) {
                            if (isTestingConnection) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            } else {
                                Text("Uji Koneksi")
                            }
                        }

                        if (geminiKey.isNotBlank()) {
                            IconButton(
                                onClick = {
                                    viewModel.settingsManager.removeGeminiApiKey()
                                    keyInput = ""
                                    Toast.makeText(context, "API Key dihapus.", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Hapus Kunci", tint = RedAccent)
                            }
                        }
                    }

                    connectionTestResult?.let { resText ->
                        val isSuccess = !resText.startsWith("Error:")
                        Surface(
                            color = if (isSuccess) EmeraldGreen.copy(alpha = 0.15f) else RedAccent.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = resText,
                                fontSize = 11.sp,
                                color = if (isSuccess) EmeraldGreen else RedAccent,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }

                    // Model Selection
                    Column {
                        Text(text = "Pilih Model Gemini:", fontSize = 12.sp, color = Color.LightGray)
                        Spacer(modifier = Modifier.height(4.dp))
                        Box {
                            Surface(
                                onClick = { showModelDropdown = true },
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF1E293B),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = geminiModel, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                                    Text(text = "Ubah ▼", fontSize = 11.sp, color = CyanAccent)
                                }
                            }

                            DropdownMenu(
                                expanded = showModelDropdown,
                                onDismissRequest = { showModelDropdown = false },
                                modifier = Modifier.background(VsCodeHeaderBg)
                            ) {
                                availableModels.forEach { model ->
                                    DropdownMenuItem(
                                        text = { Text(model, color = Color.White) },
                                        onClick = {
                                            viewModel.settingsManager.setGeminiModel(model)
                                            showModelDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Temperature Slider
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "AI Creativity (Temperature):", fontSize = 12.sp, color = Color.LightGray)
                            Text(text = String.format("%.1f", temperature), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CyanAccent)
                        }
                        Slider(
                            value = temperature,
                            onValueChange = { viewModel.settingsManager.setTemperature(it) },
                            valueRange = 0.0f..1.0f,
                            steps = 10
                        )
                    }
                }
            }
        }

        // Section 2: GitHub Integration Configuration
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = VsCodeSidebarBg)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Key, null, tint = CyanAccent, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "GitHub Personal Access Token",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Text(
                        text = "Digunakan untuk push source code dan memicu build APK otomatis di GitHub Actions.",
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8)
                    )

                    OutlinedTextField(
                        value = tokenInput,
                        onValueChange = { tokenInput = it },
                        label = { Text("GitHub Token (ghp_...)") },
                        singleLine = true,
                        visualTransformation = if (showTokenPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showTokenPassword = !showTokenPassword }) {
                                Icon(if (showTokenPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility, null, tint = Color.Gray)
                            }
                        },
                        modifier = Modifier.fillMaxWidth().testTag("settings_github_token_input")
                    )

                    OutlinedTextField(
                        value = repoInput,
                        onValueChange = { repoInput = it },
                        label = { Text("Repository (username/repo-name)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = branchInput,
                        onValueChange = { branchInput = it },
                        label = { Text("Branch (cth: main)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.settingsManager.saveGithubToken(tokenInput)
                                viewModel.settingsManager.setGithubRepo(repoInput)
                                viewModel.settingsManager.setGithubBranch(branchInput)
                                Toast.makeText(context, "Konfigurasi GitHub disimpan!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f).testTag("save_github_settings_btn")
                        ) {
                            Text("Simpan Konfigurasi")
                        }

                        OutlinedButton(
                            onClick = {
                                isTestingToken = true
                                tokenTestResult = null
                                coroutineScope.launch {
                                    val res = viewModel.gitHubService.verifyToken(tokenInput)
                                    isTestingToken = false
                                    tokenTestResult = if (res.isSuccess) {
                                        "Terhubung sebagai @${res.getOrNull()}"
                                    } else {
                                        "Gagal: ${res.exceptionOrNull()?.message}"
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            if (isTestingToken) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            } else {
                                Text("Uji Token")
                            }
                        }
                    }

                    tokenTestResult?.let { res ->
                        val isSuccess = res.startsWith("Terhubung")
                        Text(
                            text = res,
                            fontSize = 11.sp,
                            color = if (isSuccess) EmeraldGreen else RedAccent
                        )
                    }
                }
            }
        }

        // Section 3: Editor & Server Preferences
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = VsCodeSidebarBg)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Preferensi Editor & Server",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    // Font Size Slider
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Ukuran Font Editor:", fontSize = 12.sp, color = Color.LightGray)
                            Text("${fontSize}sp", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CyanAccent)
                        }
                        Slider(
                            value = fontSize.toFloat(),
                            onValueChange = { viewModel.settingsManager.setFontSize(it.toInt()) },
                            valueRange = 10f..22f,
                            steps = 12
                        )
                    }

                    // Auto-Save Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Simpan Otomatis (Auto-Save)", fontSize = 13.sp, color = Color.White)
                            Text("Menyimpan file secara otomatis setelah mengetik", fontSize = 11.sp, color = Color.Gray)
                        }
                        Switch(
                            checked = autoSave,
                            onCheckedChange = { viewModel.settingsManager.setAutoSave(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = CyanAccent)
                        )
                    }

                    // Default Server Port
                    var portInput by remember(serverPort) { mutableStateOf(serverPort.toString()) }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Port Local Server:", fontSize = 13.sp, color = Color.White)
                        OutlinedTextField(
                            value = portInput,
                            onValueChange = {
                                portInput = it
                                it.toIntOrNull()?.let { p -> viewModel.settingsManager.setServerPort(p) }
                            },
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp),
                            modifier = Modifier.width(100.dp).height(46.dp)
                        )
                    }
                }
            }
        }
    }
}

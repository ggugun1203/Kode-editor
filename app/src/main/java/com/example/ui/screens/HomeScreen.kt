package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Project
import com.example.ui.theme.BlueAccent
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.PurpleAccent
import com.example.ui.theme.VsCodeBg
import com.example.ui.theme.VsCodeHeaderBg
import com.example.ui.theme.VsCodeSidebarBg
import com.example.viewmodel.IdePane
import com.example.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val projects by viewModel.projects.collectAsState()
    val activeProject by viewModel.activeProject.collectAsState()

    var showNewProjectDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(VsCodeBg)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Header
        item {
            HeroHeader()
        }

        // Quick Action Grid
        item {
            QuickActionsGrid(
                onNewProject = { showNewProjectDialog = true },
                onOpenEditor = { viewModel.setPane(IdePane.EDITOR) },
                onOpenAi = { viewModel.setPane(IdePane.AI_ASSISTANT) },
                onOpenServer = { viewModel.setPane(IdePane.LOCAL_SERVER) },
                onOpenTerminal = { viewModel.setPane(IdePane.TERMINAL) },
                onOpenBuild = { viewModel.setPane(IdePane.GITHUB_BUILD) },
                onOpenSettings = { viewModel.setPane(IdePane.SETTINGS) }
            )
        }

        // Recent Projects Section Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Proyek Terbaru (Recent Projects)",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                TextButton(
                    onClick = { showNewProjectDialog = true },
                    modifier = Modifier.testTag("new_project_header_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Baru")
                }
            }
        }

        // Project Items
        if (projects.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Belum ada proyek. Ketuk 'Proyek Baru' untuk mulai!",
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                }
            }
        } else {
            items(projects) { proj ->
                ProjectCard(
                    project = proj,
                    isActive = activeProject?.id == proj.id,
                    onOpen = {
                        viewModel.openProject(proj)
                        viewModel.setPane(IdePane.EDITOR)
                    }
                )
            }
        }
    }

    if (showNewProjectDialog) {
        NewProjectDialog(
            onDismiss = { showNewProjectDialog = false },
            onCreate = { name, template ->
                viewModel.createNewProject(name, template)
                showNewProjectDialog = false
            }
        )
    }
}

@Composable
private fun HeroHeader() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = VsCodeSidebarBg)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .background(Color(0xFF0F172A), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Code,
                    contentDescription = null,
                    tint = CyanAccent,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "DroidStudio AI",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        color = BlueAccent,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "IDE & XAMPP",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Mobile-First Fullstack Dev, Local Server & Cloud Builder",
                    fontSize = 12.sp,
                    color = Color(0xFF94A3B8)
                )
            }
        }
    }
}

@Composable
private fun QuickActionsGrid(
    onNewProject: () -> Unit,
    onOpenEditor: () -> Unit,
    onOpenAi: () -> Unit,
    onOpenServer: () -> Unit,
    onOpenTerminal: () -> Unit,
    onOpenBuild: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ActionTile(
                title = "Proyek Baru",
                subtitle = "Buat Web / API",
                icon = Icons.Default.Add,
                color = BlueAccent,
                modifier = Modifier.weight(1f),
                onClick = onNewProject,
                testTag = "home_new_project_tile"
            )

            ActionTile(
                title = "Code Editor",
                subtitle = "Multi-tab Editor",
                icon = Icons.Default.Code,
                color = CyanAccent,
                modifier = Modifier.weight(1f),
                onClick = onOpenEditor,
                testTag = "home_editor_tile"
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ActionTile(
                title = "AI Assistant",
                subtitle = "Gemini Coding",
                icon = Icons.Default.AutoAwesome,
                color = PurpleAccent,
                modifier = Modifier.weight(1f),
                onClick = onOpenAi,
                testTag = "home_ai_tile"
            )

            ActionTile(
                title = "Local Server",
                subtitle = "PHP & Static",
                icon = Icons.Default.Dns,
                color = EmeraldGreen,
                modifier = Modifier.weight(1f),
                onClick = onOpenServer,
                testTag = "home_server_tile"
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ActionTile(
                title = "Terminal",
                subtitle = "Bash & Git",
                icon = Icons.Default.Terminal,
                color = Color(0xFFE2E8F0),
                modifier = Modifier.weight(1f),
                onClick = onOpenTerminal,
                testTag = "home_terminal_tile"
            )

            ActionTile(
                title = "Build APK",
                subtitle = "GitHub Actions",
                icon = Icons.Default.CloudUpload,
                color = Color(0xFFF59E0B),
                modifier = Modifier.weight(1f),
                onClick = onOpenBuild,
                testTag = "home_build_tile"
            )
        }
    }
}

@Composable
private fun ActionTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    testTag: String
) {
    Surface(
        onClick = onClick,
        modifier = modifier.testTag(testTag),
        shape = RoundedCornerShape(12.dp),
        color = VsCodeSidebarBg,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(color.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = Color(0xFF94A3B8)
                )
            }
        }
    }
}

@Composable
private fun ProjectCard(
    project: Project,
    isActive: Boolean,
    onOpen: () -> Unit
) {
    val dateStr = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(project.lastModified))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpen() }
            .testTag("project_card_${project.name}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) Color(0xFF1E293B) else VsCodeSidebarBg
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = null,
                    tint = if (isActive) CyanAccent else Color(0xFFF59E0B),
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = project.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color.White
                        )
                        if (isActive) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = EmeraldGreen,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "Aktif",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${project.template} • ${project.fileCount} file • $dateStr",
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
            }

            IconButton(onClick = onOpen) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Buka Proyek",
                    tint = BlueAccent
                )
            }
        }
    }
}

@Composable
private fun NewProjectDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, template: String) -> Unit
) {
    var projectName by remember { mutableStateOf("MyAwesomeApp") }
    var selectedTemplate by remember { mutableStateOf("Ecommerce") }

    val templates = listOf(
        "Ecommerce" to "Toko Online Fullstack (HTML/CSS/JS + PHP + SQLite)",
        "Portfolio" to "Developer Portfolio & Blog",
        "RestApi" to "REST API Server (PHP & SQLite)",
        "Blank" to "Proyek Kosong (Blank Project)"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "Buat Proyek Baru", fontWeight = FontWeight.Bold, color = Color.White)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = projectName,
                    onValueChange = { projectName = it },
                    label = { Text("Nama Proyek") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("new_project_name_input")
                )

                Text(
                    text = "Pilih Template Starter:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF94A3B8)
                )

                templates.forEach { (key, desc) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedTemplate = key }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedTemplate == key,
                            onClick = { selectedTemplate = key }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(text = key, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                            Text(text = desc, fontSize = 11.sp, color = Color.Gray)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (projectName.isNotBlank()) {
                        onCreate(projectName, selectedTemplate)
                    }
                },
                modifier = Modifier.testTag("confirm_create_project_button")
            ) {
                Text("Buat Proyek")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        },
        containerColor = VsCodeSidebarBg
    )
}

package com.example.ui.screens

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ProjectFile
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.OrangeAccent
import com.example.ui.theme.PurpleAccent
import com.example.ui.theme.RedAccent
import com.example.ui.theme.VsCodeBg
import com.example.ui.theme.VsCodeHeaderBg
import com.example.ui.theme.VsCodeSidebarBg
import com.example.viewmodel.MainViewModel

@Composable
fun ExplorerPane(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val activeProject by viewModel.activeProject.collectAsState()
    val fileTree by viewModel.fileTree.collectAsState()

    var showNewFileDialog by remember { mutableStateOf(false) }
    var isCreatingFolder by remember { mutableStateOf(false) }
    var selectedParentPath by remember { mutableStateOf<String?>(null) }

    var fileToRename by remember { mutableStateOf<ProjectFile?>(null) }
    var fileToDelete by remember { mutableStateOf<ProjectFile?>(null) }

    // Map to keep track of expanded folders
    val expandedFolders = remember { mutableStateMapOf<String, Boolean>() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(VsCodeSidebarBg)
    ) {
        // Explorer Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .background(VsCodeHeaderBg)
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "EXPLORER: ${activeProject?.name?.uppercase() ?: "NO PROJECT"}",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFBBBBBB)
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = {
                        selectedParentPath = activeProject?.path
                        isCreatingFolder = false
                        showNewFileDialog = true
                    },
                    modifier = Modifier.size(32.dp).testTag("explorer_new_file_btn")
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "New File",
                        tint = Color(0xFFCCCCCC),
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = {
                        selectedParentPath = activeProject?.path
                        isCreatingFolder = true
                        showNewFileDialog = true
                    },
                    modifier = Modifier.size(32.dp).testTag("explorer_new_folder_btn")
                ) {
                    Icon(
                        Icons.Default.CreateNewFolder,
                        contentDescription = "New Folder",
                        tint = Color(0xFFCCCCCC),
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = { viewModel.refreshFileTree() },
                    modifier = Modifier.size(32.dp).testTag("explorer_refresh_btn")
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = Color(0xFFCCCCCC),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // File List Tree
        if (fileTree.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Proyek kosong.\nKetuk '+' untuk membuat file baru.",
                    color = Color.Gray,
                    fontSize = 13.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 4.dp)
            ) {
                items(fileTree) { item ->
                    FileTreeItemRow(
                        file = item,
                        depth = 0,
                        expandedFolders = expandedFolders,
                        onFileClick = { file ->
                            if (file.isDirectory) {
                                val current = expandedFolders[file.path] ?: false
                                expandedFolders[file.path] = !current
                            } else {
                                viewModel.openFile(file.path)
                            }
                        },
                        onNewFileInside = { folder ->
                            selectedParentPath = folder.path
                            isCreatingFolder = false
                            showNewFileDialog = true
                        },
                        onRename = { file -> fileToRename = file },
                        onDelete = { file -> fileToDelete = file }
                    )
                }
            }
        }
    }

    // New File / Folder Dialog
    if (showNewFileDialog) {
        var inputName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showNewFileDialog = false },
            title = {
                Text(
                    text = if (isCreatingFolder) "Buat Folder Baru" else "Buat File Baru",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            },
            text = {
                Column {
                    Text(
                        text = "Lokasi: ${selectedParentPath ?: activeProject?.path}",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    OutlinedTextField(
                        value = inputName,
                        onValueChange = { inputName = it },
                        label = { Text(if (isCreatingFolder) "Nama Folder" else "Nama File (cth: index.html)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("new_file_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parent = selectedParentPath ?: activeProject?.path
                        if (parent != null && inputName.isNotBlank()) {
                            viewModel.createNewFileOrFolder(parent, inputName, isCreatingFolder)
                            showNewFileDialog = false
                        }
                    },
                    modifier = Modifier.testTag("confirm_create_file_btn")
                ) {
                    Text("Buat")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewFileDialog = false }) {
                    Text("Batal")
                }
            },
            containerColor = VsCodeSidebarBg
        )
    }

    // Rename Dialog
    fileToRename?.let { file ->
        var renameInput by remember { mutableStateOf(file.name) }
        AlertDialog(
            onDismissRequest = { fileToRename = null },
            title = { Text("Ganti Nama", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = renameInput,
                    onValueChange = { renameInput = it },
                    label = { Text("Nama Baru") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (renameInput.isNotBlank()) {
                        viewModel.renameFile(file.path, renameInput)
                        fileToRename = null
                    }
                }) {
                    Text("Simpan")
                }
            },
            dismissButton = {
                TextButton(onClick = { fileToRename = null }) {
                    Text("Batal")
                }
            },
            containerColor = VsCodeSidebarBg
        )
    }

    // Delete Confirmation
    fileToDelete?.let { file ->
        AlertDialog(
            onDismissRequest = { fileToDelete = null },
            title = { Text("Konfirmasi Hapus", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "Apakah Anda yakin ingin menghapus '${file.name}'? Perubahan tidak dapat dibatalkan.",
                    color = Color.LightGray
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteFileOrFolder(file.path)
                        fileToDelete = null
                    },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = RedAccent)
                ) {
                    Text("Hapus")
                }
            },
            dismissButton = {
                TextButton(onClick = { fileToDelete = null }) {
                    Text("Batal")
                }
            },
            containerColor = VsCodeSidebarBg
        )
    }
}

@Composable
fun FileTreeItemRow(
    file: ProjectFile,
    depth: Int,
    expandedFolders: Map<String, Boolean>,
    onFileClick: (ProjectFile) -> Unit,
    onNewFileInside: (ProjectFile) -> Unit,
    onRename: (ProjectFile) -> Unit,
    onDelete: (ProjectFile) -> Unit
) {
    val isExpanded = expandedFolders[file.path] ?: false
    var showMenu by remember { mutableStateOf(false) }

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onFileClick(file) }
                .padding(vertical = 5.dp)
                .padding(start = (depth * 14 + 10).dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            if (file.isDirectory) {
                Icon(
                    imageVector = if (isExpanded) Icons.Default.FolderOpen else Icons.Default.Folder,
                    contentDescription = null,
                    tint = Color(0xFFF59E0B),
                    modifier = Modifier.size(16.dp)
                )
            } else {
                FileExtensionIcon(extension = file.extension)
            }

            Spacer(modifier = Modifier.width(8.dp))

            // File Name
            Text(
                text = file.name,
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace,
                color = if (file.isDirectory) Color(0xFFE2E8F0) else Color(0xFFCCCCCC),
                fontWeight = if (file.isDirectory) FontWeight.SemiBold else FontWeight.Normal,
                modifier = Modifier.weight(1f)
            )

            // Context Menu Button
            Box {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint = Color.Gray,
                        modifier = Modifier.size(14.dp)
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    modifier = Modifier.background(VsCodeHeaderBg)
                ) {
                    if (file.isDirectory) {
                        DropdownMenuItem(
                            text = { Text("File Baru di Sini", color = Color.White) },
                            leadingIcon = { Icon(Icons.Default.Add, contentDescription = null, tint = CyanAccent) },
                            onClick = {
                                showMenu = false
                                onNewFileInside(file)
                            }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Ganti Nama", color = Color.White) },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = Color.LightGray) },
                        onClick = {
                            showMenu = false
                            onRename(file)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Hapus", color = RedAccent) },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = RedAccent) },
                        onClick = {
                            showMenu = false
                            onDelete(file)
                        }
                    )
                }
            }
        }

        // Render children if directory and expanded
        if (file.isDirectory && isExpanded) {
            file.children.forEach { child ->
                FileTreeItemRow(
                    file = child,
                    depth = depth + 1,
                    expandedFolders = expandedFolders,
                    onFileClick = onFileClick,
                    onNewFileInside = onNewFileInside,
                    onRename = onRename,
                    onDelete = onDelete
                )
            }
        }
    }
}

@Composable
fun FileExtensionIcon(extension: String) {
    val (label, color) = when (extension.lowercase()) {
        "html", "htm" -> "H5" to OrangeAccent
        "css" -> "CSS" to CyanAccent
        "js", "jsx" -> "JS" to Color(0xFFF7DF1E)
        "ts", "tsx" -> "TS" to Color(0xFF3178C6)
        "php" -> "PHP" to PurpleAccent
        "py" -> "PY" to Color(0xFF3776AB)
        "json" -> "{}" to EmeraldGreen
        "sql" -> "SQL" to Color(0xFF00758F)
        "xml" -> "XML" to Color(0xFFE34F26)
        "kt", "kts" -> "KT" to Color(0xFF7F52FF)
        "md" -> "MD" to Color(0xFF94A3B8)
        else -> "" to Color(0xFF94A3B8)
    }

    if (label.isNotEmpty()) {
        Surface(
            color = color.copy(alpha = 0.2f),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(3.dp),
            modifier = Modifier.size(16.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = label,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }
        }
    } else {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.InsertDriveFile,
            contentDescription = null,
            tint = Color(0xFF94A3B8),
            modifier = Modifier.size(16.dp)
        )
    }
}

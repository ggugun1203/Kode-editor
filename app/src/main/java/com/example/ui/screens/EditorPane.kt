package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FindReplace
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.editor.SyntaxHighlighter
import com.example.ui.components.QuickSymbolBar
import com.example.ui.theme.BlueAccent
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.OrangeAccent
import com.example.ui.theme.PurpleAccent
import com.example.ui.theme.VsCodeBg
import com.example.ui.theme.VsCodeHeaderBg
import com.example.ui.theme.VsCodeLineNumber
import com.example.ui.theme.VsCodeSidebarBg
import com.example.ui.theme.VsCodeTabActive
import com.example.ui.theme.VsCodeTabInactive
import com.example.viewmodel.IdePane
import com.example.viewmodel.MainViewModel

@Composable
fun EditorPane(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val openTabs by viewModel.openTabs.collectAsState()
    val activeTabIndex by viewModel.activeTabIndex.collectAsState()
    val fontSize by viewModel.settingsManager.fontSize.collectAsState()
    val isServerRunning by viewModel.localWebServer.isRunning.collectAsState()

    val currentTab = viewModel.currentTab

    // Find & Replace state
    var showFindReplace by remember { mutableStateOf(false) }
    var findQuery by remember { mutableStateOf("") }
    var replaceQuery by remember { mutableStateOf("") }

    // Quick AI Actions Menu
    var showAiMenu by remember { mutableStateOf(false) }

    // Text field state mapped to current tab content
    var textFieldValue by remember(currentTab?.filePath) {
        mutableStateOf(TextFieldValue(currentTab?.content ?: "", TextRange(0)))
    }

    // Keep synced when content changes from AI or file switch
    LaunchedEffect(currentTab?.content) {
        if (currentTab != null && currentTab.content != textFieldValue.text) {
            textFieldValue = textFieldValue.copy(text = currentTab.content)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(VsCodeBg)
    ) {
        // Tab Bar
        if (openTabs.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp)
                    .background(VsCodeHeaderBg)
                    .horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically
            ) {
                openTabs.forEachIndexed { idx, tab ->
                    val isActive = idx == activeTabIndex
                    Surface(
                        modifier = Modifier
                            .fillMaxHeight()
                            .clickable { viewModel.selectTab(idx) }
                            .testTag("tab_${tab.fileName}"),
                        color = if (isActive) VsCodeTabActive else VsCodeTabInactive
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxHeight()
                                .padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FileExtensionIcon(extension = tab.language)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = tab.fileName,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                color = if (isActive) Color.White else Color(0xFF94A3B8)
                            )
                            if (tab.isDirty) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(CyanAccent, CircleShape)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            IconButton(
                                onClick = { viewModel.closeTab(idx) },
                                modifier = Modifier.size(18.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close Tab",
                                    tint = Color.Gray,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Sub-header Toolbar: Run, Find, AI Actions
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
                .background(VsCodeSidebarBg)
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = currentTab?.fileName ?: "No file open",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color.LightGray
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Find & Replace Toggle
                IconButton(
                    onClick = { showFindReplace = !showFindReplace },
                    modifier = Modifier.size(28.dp).testTag("editor_find_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Find & Replace",
                        tint = if (showFindReplace) CyanAccent else Color.LightGray,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // AI Assist Action Button
                Box {
                    IconButton(
                        onClick = { showAiMenu = true },
                        modifier = Modifier.size(28.dp).testTag("editor_ai_menu_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "AI Actions",
                            tint = PurpleAccent,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showAiMenu,
                        onDismissRequest = { showAiMenu = false },
                        modifier = Modifier.background(VsCodeHeaderBg)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Jelaskan Kode Ini", color = Color.White) },
                            leadingIcon = { Icon(Icons.Default.AutoAwesome, null, tint = CyanAccent) },
                            onClick = {
                                showAiMenu = false
                                viewModel.sendAiPrompt("Jelaskan arsitektur dan alur fungsi dari kode file ${currentTab?.fileName} ini secara ringkas dan padat.")
                                viewModel.setPane(IdePane.AI_ASSISTANT)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Perbaiki Error & Bug", color = Color.White) },
                            leadingIcon = { Icon(Icons.Default.AutoAwesome, null, tint = OrangeAccent) },
                            onClick = {
                                showAiMenu = false
                                viewModel.sendAiPrompt("Periksa apakah ada syntax error, security vulnerability, atau bug pada file ${currentTab?.fileName} ini dan berikan solusinya.")
                                viewModel.setPane(IdePane.AI_ASSISTANT)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Refactor & Rapikan", color = Color.White) },
                            leadingIcon = { Icon(Icons.Default.AutoAwesome, null, tint = EmeraldGreen) },
                            onClick = {
                                showAiMenu = false
                                viewModel.sendAiPrompt("Refactor kode ${currentTab?.fileName} ini agar lebih clean, modular, dan performa tinggi.")
                                viewModel.setPane(IdePane.AI_ASSISTANT)
                            }
                        )
                    }
                }

                // Run Live Preview
                IconButton(
                    onClick = {
                        if (!isServerRunning) {
                            viewModel.toggleLocalServer()
                        }
                        viewModel.setPane(IdePane.LIVE_PREVIEW)
                    },
                    modifier = Modifier.size(28.dp).testTag("editor_run_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Run in Live Preview",
                        tint = EmeraldGreen,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Expandable Find & Replace Bar
        if (showFindReplace) {
            FindReplaceBar(
                findText = findQuery,
                onFindChange = { findQuery = it },
                replaceText = replaceQuery,
                onReplaceChange = { replaceQuery = it },
                onReplaceAll = {
                    if (findQuery.isNotEmpty() && currentTab != null) {
                        val replaced = textFieldValue.text.replace(findQuery, replaceQuery)
                        textFieldValue = textFieldValue.copy(text = replaced)
                        viewModel.updateCurrentContent(replaced)
                    }
                },
                onClose = { showFindReplace = false }
            )
        }

        // Editor Body
        if (currentTab == null) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = Color.DarkGray,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Pilih file di Explorer atau minta AI membuat kode",
                        color = Color.Gray,
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            // Main Code Area with Line Numbers Gutter
            val lines = remember(textFieldValue.text) {
                textFieldValue.text.split("\n")
            }
            val lineCount = lines.size.coerceAtLeast(1)
            val lineNumbersText = remember(lineCount) {
                (1..lineCount).joinToString("\n")
            }

            val highlightedCode = remember(textFieldValue.text, currentTab.language) {
                SyntaxHighlighter.highlight(textFieldValue.text, currentTab.language)
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .horizontalScroll(rememberScrollState())
                ) {
                    // Line numbers gutter
                    Text(
                        text = lineNumbersText,
                        fontFamily = FontFamily.Monospace,
                        fontSize = fontSize.sp,
                        color = VsCodeLineNumber,
                        lineHeight = (fontSize * 1.4f).sp,
                        modifier = Modifier
                            .background(VsCodeSidebarBg)
                            .padding(horizontal = 8.dp, vertical = 8.dp)
                    )

                    // Code Editor Input
                    BasicTextField(
                        value = textFieldValue,
                        onValueChange = { newValue ->
                            textFieldValue = newValue
                            viewModel.updateCurrentContent(newValue.text)
                        },
                        textStyle = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = fontSize.sp,
                            color = Color(0xFFE2E8F0),
                            lineHeight = (fontSize * 1.4f).sp
                        ),
                        cursorBrush = SolidColor(CyanAccent),
                        visualTransformation = {
                            androidx.compose.ui.text.input.TransformedText(
                                highlightedCode,
                                androidx.compose.ui.text.input.OffsetMapping.Identity
                            )
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp)
                            .testTag("code_editor_textfield")
                    )
                }
            }
        }

        // Quick Symbol Bar for Phone Keyboards
        QuickSymbolBar(
            onInsertSymbol = { sym ->
                val currentText = textFieldValue.text
                val selection = textFieldValue.selection
                val newText = currentText.substring(0, selection.min) + sym + currentText.substring(selection.max)
                val newCursor = selection.min + sym.length
                textFieldValue = TextFieldValue(newText, TextRange(newCursor))
                viewModel.updateCurrentContent(newText)
            },
            onUndo = { viewModel.undo() },
            onRedo = { viewModel.redo() },
            onSave = { viewModel.saveCurrentTab() }
        )
    }
}

@Composable
private fun FindReplaceBar(
    findText: String,
    onFindChange: (String) -> Unit,
    replaceText: String,
    onReplaceChange: (String) -> Unit,
    onReplaceAll: () -> Unit,
    onClose: () -> Unit
) {
    Surface(
        color = VsCodeHeaderBg,
        tonalElevation = 4.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = findText,
                    onValueChange = onFindChange,
                    placeholder = { Text("Cari...", fontSize = 12.sp) },
                    singleLine = true,
                    textStyle = TextStyle(fontSize = 12.sp, fontFamily = FontFamily.Monospace),
                    modifier = Modifier.weight(1f).height(46.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanAccent,
                        unfocusedBorderColor = Color.Gray
                    )
                )
                Spacer(modifier = Modifier.width(6.dp))
                IconButton(onClick = onClose, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = replaceText,
                    onValueChange = onReplaceChange,
                    placeholder = { Text("Ganti dengan...", fontSize = 12.sp) },
                    singleLine = true,
                    textStyle = TextStyle(fontSize = 12.sp, fontFamily = FontFamily.Monospace),
                    modifier = Modifier.weight(1f).height(46.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanAccent,
                        unfocusedBorderColor = Color.Gray
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(onClick = onReplaceAll) {
                    Text("Ganti Semua", fontSize = 11.sp, color = CyanAccent)
                }
            }
        }
    }
}

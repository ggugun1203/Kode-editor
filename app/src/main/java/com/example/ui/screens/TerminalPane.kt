package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.TerminalLine
import com.example.model.TerminalOutputType
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.OrangeAccent
import com.example.ui.theme.RedAccent
import com.example.ui.theme.VsCodeBg
import com.example.ui.theme.VsCodeHeaderBg
import com.example.ui.theme.VsCodeSidebarBg
import com.example.viewmodel.MainViewModel
import kotlinx.coroutines.launch

@Composable
fun TerminalPane(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val terminalLines by viewModel.terminalEngine.lines.collectAsState()
    val activeProject by viewModel.activeProject.collectAsState()

    var inputCommand by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // Auto scroll to bottom
    LaunchedEffect(terminalLines.size) {
        if (terminalLines.isNotEmpty()) {
            listState.animateScrollToItem(terminalLines.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0C0C0C)) // Pure dark terminal aesthetic
    ) {
        // Terminal Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp)
                .background(VsCodeHeaderBg)
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Terminal,
                    contentDescription = null,
                    tint = EmeraldGreen,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "TERMINAL: 1. bash (ARM64)",
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            IconButton(
                onClick = { viewModel.terminalEngine.clear() },
                modifier = Modifier.size(28.dp).testTag("terminal_clear_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Clear",
                    tint = Color.Gray,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // Quick Commands Bar
        QuickCommandsRow(
            onRunCommand = { cmd ->
                scope.launch {
                    viewModel.terminalEngine.executeCommand(cmd)
                }
            }
        )

        // Terminal Output Screen
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 10.dp),
            contentPadding = PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            items(terminalLines) { line ->
                val textColor = when (line.type) {
                    TerminalOutputType.CMD -> CyanAccent
                    TerminalOutputType.SUCCESS -> EmeraldGreen
                    TerminalOutputType.STDERR -> RedAccent
                    TerminalOutputType.INFO -> Color(0xFF94A3B8)
                    TerminalOutputType.STDOUT -> Color(0xFFE2E8F0)
                }

                Text(
                    text = line.text,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = textColor,
                    lineHeight = 16.sp
                )
            }
        }

        // Input Command Prompt
        Surface(
            color = Color(0xFF171717),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$ ",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = EmeraldGreen
                )

                OutlinedTextField(
                    value = inputCommand,
                    onValueChange = { inputCommand = it },
                    placeholder = { Text("Ketik perintah (cth: php -S 0.0.0.0:8080, ls, git status)...", fontSize = 11.sp) },
                    singleLine = true,
                    textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = Color.White),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = {
                        if (inputCommand.isNotBlank()) {
                            val cmd = inputCommand
                            inputCommand = ""
                            scope.launch {
                                viewModel.terminalEngine.executeCommand(cmd)
                            }
                        }
                    }),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldGreen,
                        unfocusedBorderColor = Color.DarkGray
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("terminal_input_field")
                )

                Spacer(modifier = Modifier.width(6.dp))

                IconButton(
                    onClick = {
                        if (inputCommand.isNotBlank()) {
                            val cmd = inputCommand
                            inputCommand = ""
                            scope.launch {
                                viewModel.terminalEngine.executeCommand(cmd)
                            }
                        }
                    },
                    modifier = Modifier.testTag("terminal_send_btn")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Execute",
                        tint = EmeraldGreen
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickCommandsRow(onRunCommand: (String) -> Unit) {
    val quickPills = listOf(
        "ls -la",
        "pwd",
        "php -S 0.0.0.0:8080",
        "npm run dev",
        "git status",
        "git log",
        "sqlite3 database/app.db \"SELECT * FROM products;\"",
        "help",
        "clear"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF141414))
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        quickPills.forEach { cmd ->
            Surface(
                onClick = { onRunCommand(cmd) },
                shape = RoundedCornerShape(4.dp),
                color = Color(0xFF262626),
                modifier = Modifier.padding(vertical = 1.dp)
            ) {
                Text(
                    text = cmd,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = CyanAccent,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

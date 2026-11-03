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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AiMessage
import com.example.model.DiffStatus
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

@Composable
fun AiAssistantPane(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val messages by viewModel.aiMessages.collectAsState()
    val isLoading by viewModel.isAiLoading.collectAsState()
    val geminiKey by viewModel.settingsManager.geminiApiKey.collectAsState()
    val geminiModel by viewModel.settingsManager.geminiModel.collectAsState()
    val activeProject by viewModel.activeProject.collectAsState()
    val currentTab = viewModel.currentTab

    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Scroll to bottom on new message
    LaunchedEffect(messages.size, isLoading) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(VsCodeBg)
    ) {
        // AI Header Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .background(VsCodeHeaderBg)
                .padding(horizontal = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = PurpleAccent,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "AI Coding Assistant",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Model: $geminiModel • Workspace: ${activeProject?.name ?: "None"}",
                        fontSize = 10.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
            }

            IconButton(
                onClick = { viewModel.setPane(IdePane.SETTINGS) },
                modifier = Modifier.testTag("ai_settings_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "AI Settings",
                    tint = Color.LightGray,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Warning banner if Gemini key not set
        if (geminiKey.isBlank()) {
            Surface(
                color = Color(0xFF451A03),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Key, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Gemini API Key belum dimasukkan. Atur di menu Pengaturan.",
                            fontSize = 11.sp,
                            color = Color(0xFFFDE68A)
                        )
                    }
                    TextButton(onClick = { viewModel.setPane(IdePane.SETTINGS) }) {
                        Text("Atur Kunci", fontSize = 11.sp, color = CyanAccent)
                    }
                }
            }
        }

        // Quick Prompt Pills
        QuickPromptsRow(
            onSelectPrompt = { prompt ->
                inputText = prompt
            }
        )

        // Messages List
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(messages) { msg ->
                AiMessageItem(
                    message = msg,
                    onReviewDiff = { proposal ->
                        viewModel.showDiffProposal(proposal)
                    },
                    onApplyDirect = { proposal ->
                        viewModel.applyDiffProposal(proposal)
                    }
                )
            }

            if (isLoading) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = PurpleAccent,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "AI sedang menganalisis proyek & menyiapkan kode...",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }
            }
        }

        // Chat Input Bar
        Surface(
            color = VsCodeHeaderBg,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = {
                        Text(
                            text = if (currentTab != null) "Tanya AI tentang ${currentTab.fileName}..." else "Instruksi kode / generate project...",
                            fontSize = 12.sp
                        )
                    },
                    maxLines = 4,
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp, color = Color.White),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PurpleAccent,
                        unfocusedBorderColor = Color.DarkGray
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("ai_chat_input")
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = {
                        if (inputText.isNotBlank() && !isLoading) {
                            val text = inputText
                            inputText = ""
                            viewModel.sendAiPrompt(text)
                        }
                    },
                    enabled = inputText.isNotBlank() && !isLoading,
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            if (inputText.isNotBlank()) PurpleAccent else Color(0xFF333333),
                            CircleShape
                        )
                        .testTag("ai_send_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send to AI",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickPromptsRow(onSelectPrompt: (String) -> Unit) {
    val samplePrompts = listOf(
        "Buatkan website toko online dengan katalog, keranjang belanja, dan checkout",
        "Tambahkan fitur Dark Mode toggle pada index.html dan style.css",
        "Buat REST API PHP untuk produk dan hubungkan ke SQLite",
        "Periksa apakah ada bug pada file ini dan perbaiki",
        "Refactor kode agar lebih clean dan modular",
        "Buat halaman admin dashboard dengan tabel statistik"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(VsCodeSidebarBg)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        samplePrompts.forEach { prompt ->
            Surface(
                onClick = { onSelectPrompt(prompt) },
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF1E293B),
                modifier = Modifier.padding(vertical = 2.dp)
            ) {
                Text(
                    text = prompt.take(38) + if (prompt.length > 38) "..." else "",
                    fontSize = 11.sp,
                    color = CyanAccent,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }
    }
}

@Composable
private fun AiMessageItem(
    message: AiMessage,
    onReviewDiff: (com.example.model.FileDiffProposal) -> Unit,
    onApplyDirect: (com.example.model.FileDiffProposal) -> Unit
) {
    val isUser = message.role == "user"

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(if (isUser) 0.85f else 0.95f),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = when {
                    message.isError -> Color(0xFF450A0A)
                    isUser -> Color(0xFF0F3E68)
                    else -> VsCodeSidebarBg
                }
            )
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isUser) Icons.Default.Code else Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = if (isUser) CyanAccent else PurpleAccent,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isUser) "Anda" else "DroidStudio AI",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = Color.LightGray
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = message.content,
                    fontSize = 13.sp,
                    color = Color(0xFFF1F5F9),
                    lineHeight = 18.sp
                )

                // Render File Diff Proposal Card if AI generated code
                message.diffProposal?.let { proposal ->
                    Spacer(modifier = Modifier.height(10.dp))
                    DiffProposalCard(
                        proposal = proposal,
                        onReview = { onReviewDiff(proposal) },
                        onApply = { onApplyDirect(proposal) }
                    )
                }
            }
        }
    }
}

@Composable
private fun DiffProposalCard(
    proposal: com.example.model.FileDiffProposal,
    onReview: () -> Unit,
    onApply: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Code, null, tint = CyanAccent, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = proposal.targetFilePath,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = Color(0xFF38BDF8)
                    )
                }

                Surface(
                    color = when (proposal.status) {
                        DiffStatus.APPLIED -> EmeraldGreen
                        DiffStatus.REJECTED -> RedAccent
                        DiffStatus.PENDING -> BlueAccent
                    },
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = when (proposal.status) {
                            DiffStatus.APPLIED -> "Diterapkan"
                            DiffStatus.REJECTED -> "Ditolak"
                            DiffStatus.PENDING -> "Menunggu Persetujuan"
                        },
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            if (proposal.summary.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = proposal.summary,
                    fontSize = 11.sp,
                    color = Color(0xFF94A3B8)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (proposal.status == DiffStatus.PENDING) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onReview,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Lihat Diff", fontSize = 11.sp)
                    }

                    Button(
                        onClick = onApply,
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Check, null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Terapkan", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

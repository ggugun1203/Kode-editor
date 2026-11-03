package com.example.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.FileDiffProposal
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.RedAccent
import com.example.ui.theme.VsCodeBg
import com.example.ui.theme.VsCodeHeaderBg
import com.example.ui.theme.VsCodeSidebarBg

@Composable
fun DiffDialog(
    proposal: FileDiffProposal,
    onApply: () -> Unit,
    onReject: () -> Unit,
    onUndo: () -> Unit,
    onDismiss: () -> Unit
) {
    var selectedDiffTab by remember { mutableIntStateOf(0) } // 0: Sesudah (Proposed), 1: Sebelum (Original), 2: Perbandingan (Split)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f)
                .padding(12.dp)
                .testTag("diff_dialog"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = VsCodeSidebarBg)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Persetujuan Perubahan AI",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = proposal.targetFilePath,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF38BDF8)
                        )
                    }
                }

                // Summary banner
                SurfaceSummary(summary = proposal.summary)

                Spacer(modifier = Modifier.height(8.dp))

                // Diff view mode tabs
                TabRow(
                    selectedTabIndex = selectedDiffTab,
                    containerColor = VsCodeHeaderBg,
                    contentColor = Color.White
                ) {
                    Tab(
                        selected = selectedDiffTab == 0,
                        onClick = { selectedDiffTab = 0 },
                        text = { Text("Kode Baru (+)") }
                    )
                    Tab(
                        selected = selectedDiffTab == 1,
                        onClick = { selectedDiffTab = 1 },
                        text = { Text("Kode Lama (-)") }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Code Diff Content
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(VsCodeBg, RoundedCornerShape(8.dp))
                        .padding(10.dp)
                        .verticalScroll(rememberScrollState())
                        .horizontalScroll(rememberScrollState())
                ) {
                    val codeToShow = if (selectedDiffTab == 0) {
                        proposal.proposedContent
                    } else {
                        if (proposal.originalContent.isBlank()) "(File baru - tidak ada kode sebelumnya)" else proposal.originalContent
                    }

                    Text(
                        text = codeToShow,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = if (selectedDiffTab == 0) Color(0xFFE2E8F0) else Color(0xFF94A3B8)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onReject,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = RedAccent),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("reject_diff_button")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Tolak")
                    }

                    OutlinedButton(
                        onClick = onUndo,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("undo_diff_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Undo")
                    }

                    Button(
                        onClick = onApply,
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                        modifier = Modifier
                            .weight(1.2f)
                            .testTag("apply_diff_button")
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Terapkan")
                    }
                }
            }
        }
    }
}

@Composable
private fun SurfaceSummary(summary: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .background(Color(0xFF1E293B), RoundedCornerShape(8.dp))
            .padding(10.dp)
    ) {
        Column {
            Text(
                text = "Ringkasan:",
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = Color(0xFF94A3B8)
            )
            Text(
                text = summary.ifBlank { "Modifikasi konten file sesuai instruksi." },
                fontSize = 13.sp,
                color = Color.White
            )
        }
    }
}

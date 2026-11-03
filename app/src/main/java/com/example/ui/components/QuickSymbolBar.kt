package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.VsCodeHeaderBg
import com.example.ui.theme.VsCodeSidebarBg

@Composable
fun QuickSymbolBar(
    onInsertSymbol: (String) -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier
) {
    val symbols = listOf(
        "{", "}", "(", ")", "[", "]", "<", ">",
        ";", ":", "=", "\"", "'", "/", "\\", "$",
        "!", "?", "&", "|", "+", "-", "*", "Tab"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(42.dp)
            .background(VsCodeHeaderBg)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Quick Action Buttons
        IconButton(
            onClick = onUndo,
            modifier = Modifier.testTag("undo_button")
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Undo,
                contentDescription = "Undo",
                tint = Color.White,
                modifier = Modifier.padding(4.dp)
            )
        }

        IconButton(
            onClick = onRedo,
            modifier = Modifier.testTag("redo_button")
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Redo,
                contentDescription = "Redo",
                tint = Color.White,
                modifier = Modifier.padding(4.dp)
            )
        }

        IconButton(
            onClick = onSave,
            modifier = Modifier.testTag("save_button")
        ) {
            Icon(
                imageVector = Icons.Default.Save,
                contentDescription = "Save File",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(4.dp)
            )
        }

        // Horizontal scrollable symbols
        Row(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            symbols.forEach { sym ->
                Surface(
                    onClick = {
                        if (sym == "Tab") {
                            onInsertSymbol("  ")
                        } else {
                            onInsertSymbol(sym)
                        }
                    },
                    shape = RoundedCornerShape(4.dp),
                    color = VsCodeSidebarBg,
                    modifier = Modifier.testTag("symbol_$sym")
                ) {
                    Text(
                        text = sym,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

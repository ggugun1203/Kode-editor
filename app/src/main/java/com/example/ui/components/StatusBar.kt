package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.RedAccent
import com.example.ui.theme.VsCodeStatusBarBg

@Composable
fun StatusBar(
    isServerRunning: Boolean,
    serverPort: Int,
    currentLanguage: String,
    geminiModel: String,
    gitBranch: String,
    onServerClick: () -> Unit,
    onAiClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(26.dp)
            .background(if (isServerRunning) Color(0xFF005FB8) else VsCodeStatusBarBg)
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left side: Git branch & Server status
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = " $gitBranch",
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.width(12.dp))

            Row(
                modifier = Modifier.clickable { onServerClick() },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(if (isServerRunning) EmeraldGreen else RedAccent, CircleShape)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (isServerRunning) "Port: $serverPort (Live)" else "Server: Off",
                    color = Color.White,
                    fontSize = 11.sp
                )
            }
        }

        // Right side: Gemini model & Language & Encoding
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(
                modifier = Modifier.clickable { onAiClick() },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = Color(0xFF70F7D7),
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = geminiModel.replace("gemini-", ""),
                    color = Color.White,
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Text(
                text = currentLanguage.uppercase(),
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "UTF-8",
                color = Color(0xFFD1D5DB),
                fontSize = 11.sp
            )
        }
    }
}

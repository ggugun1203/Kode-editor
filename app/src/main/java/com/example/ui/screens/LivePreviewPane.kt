package com.example.ui.screens

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.model.ConsoleLogItem
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.OrangeAccent
import com.example.ui.theme.RedAccent
import com.example.ui.theme.VsCodeBg
import com.example.ui.theme.VsCodeHeaderBg
import com.example.ui.theme.VsCodeSidebarBg
import com.example.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun LivePreviewPane(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val previewUrl by viewModel.previewUrl.collectAsState()
    val isDesktop by viewModel.isDesktopPreview.collectAsState()
    val consoleLogs by viewModel.consoleLogs.collectAsState()
    val reloadTrigger by viewModel.previewReloadTrigger.collectAsState()

    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var currentUrlInput by remember(previewUrl) { mutableStateOf(previewUrl) }
    var loadProgress by remember { mutableFloatStateOf(1f) }
    var showConsoleLogs by remember { mutableStateOf(false) }

    // Auto reload when files change
    LaunchedEffect(reloadTrigger) {
        if (reloadTrigger > 0) {
            webViewInstance?.reload()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(VsCodeBg)
    ) {
        // Browser Toolbar
        Surface(
            color = VsCodeHeaderBg,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { if (webViewInstance?.canGoBack() == true) webViewInstance?.goBack() },
                        modifier = Modifier.size(32.dp).testTag("preview_back_btn")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.LightGray, modifier = Modifier.size(18.dp))
                    }

                    IconButton(
                        onClick = { if (webViewInstance?.canGoForward() == true) webViewInstance?.goForward() },
                        modifier = Modifier.size(32.dp).testTag("preview_forward_btn")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Forward", tint = Color.LightGray, modifier = Modifier.size(18.dp))
                    }

                    IconButton(
                        onClick = { webViewInstance?.reload() },
                        modifier = Modifier.size(32.dp).testTag("preview_refresh_btn")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reload", tint = CyanAccent, modifier = Modifier.size(18.dp))
                    }

                    // URL Address Bar
                    OutlinedTextField(
                        value = currentUrlInput,
                        onValueChange = { currentUrlInput = it },
                        singleLine = true,
                        textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Color.White),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanAccent,
                            unfocusedBorderColor = Color.DarkGray
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .padding(horizontal = 4.dp)
                            .testTag("preview_url_input")
                    )

                    // Desktop vs Mobile toggle
                    IconButton(
                        onClick = { viewModel.toggleDesktopPreview() },
                        modifier = Modifier.size(32.dp).testTag("preview_viewport_toggle")
                    ) {
                        Icon(
                            imageVector = if (isDesktop) Icons.Default.DesktopWindows else Icons.Default.PhoneAndroid,
                            contentDescription = "Viewport Mode",
                            tint = if (isDesktop) CyanAccent else Color.LightGray,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Console logs badge
                    IconButton(
                        onClick = { showConsoleLogs = !showConsoleLogs },
                        modifier = Modifier.size(32.dp).testTag("preview_console_toggle")
                    ) {
                        BadgedBox(
                            badge = {
                                if (consoleLogs.isNotEmpty()) {
                                    Badge(containerColor = RedAccent) {
                                        Text("${consoleLogs.size.coerceAtMost(99)}", fontSize = 8.sp)
                                    }
                                }
                            }
                        ) {
                            Icon(
                                Icons.Default.BugReport,
                                contentDescription = "Console Logs",
                                tint = if (showConsoleLogs) Color(0xFFF59E0B) else Color.LightGray,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                if (loadProgress < 1f) {
                    LinearProgressIndicator(
                        progress = { loadProgress },
                        modifier = Modifier.fillMaxWidth().height(2.dp),
                        color = CyanAccent,
                        trackColor = Color.Transparent
                    )
                }
            }
        }

        // WebView Container
        Box(
            modifier = Modifier
                .weight(if (showConsoleLogs) 0.6f else 1f)
                .fillMaxWidth()
                .background(Color.White)
        ) {
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.allowFileAccess = true
                        settings.allowContentAccess = true
                        settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        settings.useWideViewPort = true
                        settings.loadWithOverviewMode = true

                        if (isDesktop) {
                            settings.userAgentString =
                                "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
                        }

                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                loadProgress = newProgress / 100f
                            }

                            override fun onConsoleMessage(message: ConsoleMessage?): Boolean {
                                message?.let {
                                    viewModel.addConsoleLog(it.messageLevel().name, it.message())
                                }
                                return true
                            }
                        }

                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                if (url != null) currentUrlInput = url
                            }

                            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                return false // Load internally
                            }
                        }

                        loadUrl(previewUrl)
                        webViewInstance = this
                    }
                },
                update = { webView ->
                    webView.settings.userAgentString = if (isDesktop) {
                        "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
                    } else null

                    if (webView.url != previewUrl && !previewUrl.isBlank()) {
                        webView.loadUrl(previewUrl)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Console Logs Inspector Drawer
        if (showConsoleLogs) {
            ConsoleLogsDrawer(
                logs = consoleLogs,
                onClear = { viewModel.clearConsoleLogs() },
                onClose = { showConsoleLogs = false },
                modifier = Modifier
                    .weight(0.4f)
                    .fillMaxWidth()
            )
        }
    }
}

@Composable
private fun ConsoleLogsDrawer(
    logs: List<ConsoleLogItem>,
    onClear: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
        colors = CardDefaults.cardColors(containerColor = VsCodeSidebarBg)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(34.dp)
                    .background(VsCodeHeaderBg)
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "CONSOLE INSPECTOR (${logs.size})",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color.LightGray
                )

                Row {
                    IconButton(onClick = onClear, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Clear", tint = Color.Gray, modifier = Modifier.size(14.dp))
                    }
                    IconButton(onClick = onClose, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray, modifier = Modifier.size(14.dp))
                    }
                }
            }

            if (logs.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Belum ada log console (console.log / error).", color = Color.Gray, fontSize = 11.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(logs) { log ->
                        val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(log.timestamp))
                        val (textColor, bgTint) = when (log.level) {
                            "ERROR" -> RedAccent to RedAccent.copy(alpha = 0.1f)
                            "WARNING", "WARN" -> OrangeAccent to OrangeAccent.copy(alpha = 0.1f)
                            else -> Color(0xFFE2E8F0) to Color.Transparent
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(bgTint, RoundedCornerShape(4.dp))
                                .padding(4.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                text = "[$time] ${log.level}: ",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = textColor
                            )
                            Text(
                                text = log.message,
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

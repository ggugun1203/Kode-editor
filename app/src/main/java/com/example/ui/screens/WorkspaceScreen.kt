package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.DiffDialog
import com.example.ui.components.StatusBar
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.PurpleAccent
import com.example.ui.theme.VsCodeActivityBarBg
import com.example.ui.theme.VsCodeBg
import com.example.ui.theme.VsCodeHeaderBg
import com.example.ui.theme.VsCodeSidebarBg
import com.example.viewmodel.IdePane
import com.example.viewmodel.MainViewModel

data class NavItem(
    val pane: IdePane,
    val title: String,
    val icon: ImageVector,
    val testTag: String
)

@Composable
fun WorkspaceScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val currentPane by viewModel.currentPane.collectAsState()
    val isServerRunning by viewModel.localWebServer.isRunning.collectAsState()
    val serverPort by viewModel.settingsManager.serverPort.collectAsState()
    val geminiModel by viewModel.settingsManager.geminiModel.collectAsState()
    val gitBranch by viewModel.settingsManager.githubBranch.collectAsState()
    val activeDiffProposal by viewModel.activeDiffProposal.collectAsState()
    val currentTab = viewModel.currentTab

    val navItems = listOf(
        NavItem(IdePane.HOME, "Home", Icons.Default.Home, "nav_home"),
        NavItem(IdePane.EXPLORER, "Files", Icons.Default.Folder, "nav_explorer"),
        NavItem(IdePane.EDITOR, "Editor", Icons.Default.Code, "nav_editor"),
        NavItem(IdePane.AI_ASSISTANT, "AI", Icons.Default.AutoAwesome, "nav_ai"),
        NavItem(IdePane.LOCAL_SERVER, "Server", Icons.Default.Dns, "nav_server"),
        NavItem(IdePane.LIVE_PREVIEW, "Preview", Icons.Default.PlayArrow, "nav_preview"),
        NavItem(IdePane.TERMINAL, "Terminal", Icons.Default.Terminal, "nav_terminal"),
        NavItem(IdePane.GITHUB_BUILD, "Build", Icons.Default.CloudUpload, "nav_build")
    )

    // Handle Android system back button smoothly
    BackHandler(enabled = currentPane != IdePane.HOME) {
        if (currentPane != IdePane.EDITOR && viewModel.openTabs.value.isNotEmpty()) {
            viewModel.setPane(IdePane.EDITOR)
        } else {
            viewModel.setPane(IdePane.HOME)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            Column {
                // Bottom VS Code Status Bar
                StatusBar(
                    isServerRunning = isServerRunning,
                    serverPort = serverPort,
                    currentLanguage = currentTab?.language ?: "plaintext",
                    geminiModel = geminiModel,
                    gitBranch = gitBranch,
                    onServerClick = { viewModel.setPane(IdePane.LOCAL_SERVER) },
                    onAiClick = { viewModel.setPane(IdePane.AI_ASSISTANT) }
                )

                // Mobile Navigation Bar
                NavigationBar(
                    containerColor = VsCodeActivityBarBg,
                    tonalElevation = 6.dp,
                    modifier = Modifier.height(58.dp)
                ) {
                    navItems.forEach { item ->
                        val selected = currentPane == item.pane
                        NavigationBarItem(
                            selected = selected,
                            onClick = { viewModel.setPane(item.pane) },
                            icon = {
                                if (item.pane == IdePane.LOCAL_SERVER && isServerRunning) {
                                    BadgedBox(badge = { Badge(containerColor = EmeraldGreen) }) {
                                        Icon(item.icon, contentDescription = item.title, modifier = Modifier.size(20.dp))
                                    }
                                } else {
                                    Icon(item.icon, contentDescription = item.title, modifier = Modifier.size(20.dp))
                                }
                            },
                            label = { Text(item.title, fontSize = 9.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = CyanAccent,
                                selectedTextColor = CyanAccent,
                                unselectedIconColor = Color(0xFF858585),
                                unselectedTextColor = Color(0xFF858585),
                                indicatorColor = Color(0xFF264F78)
                            ),
                            modifier = Modifier.testTag(item.testTag)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(VsCodeBg)
        ) {
            when (currentPane) {
                IdePane.HOME -> HomeScreen(viewModel = viewModel)
                IdePane.EXPLORER -> ExplorerPane(viewModel = viewModel)
                IdePane.EDITOR -> EditorPane(viewModel = viewModel)
                IdePane.AI_ASSISTANT -> AiAssistantPane(viewModel = viewModel)
                IdePane.LOCAL_SERVER -> LocalServerPane(viewModel = viewModel)
                IdePane.LIVE_PREVIEW -> LivePreviewPane(viewModel = viewModel)
                IdePane.TERMINAL -> TerminalPane(viewModel = viewModel)
                IdePane.GITHUB_BUILD -> GitHubBuildPane(viewModel = viewModel)
                IdePane.SETTINGS -> SettingsScreen(viewModel = viewModel)
                IdePane.SEARCH -> ExplorerPane(viewModel = viewModel)
            }

            // Diff Approval Dialog Overlay
            activeDiffProposal?.let { proposal ->
                DiffDialog(
                    proposal = proposal,
                    onApply = { viewModel.applyDiffProposal(proposal) },
                    onReject = { viewModel.rejectDiffProposal(proposal) },
                    onUndo = { viewModel.undoDiffProposal(proposal) },
                    onDismiss = { viewModel.dismissDiffProposal() }
                )
            }
        }
    }
}

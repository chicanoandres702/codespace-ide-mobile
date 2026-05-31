package com.codespace.ide.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.codespace.ide.data.SecureTokenStore
import com.codespace.ide.ui.panes.AiAssistantPane
import com.codespace.ide.ui.panes.EditorPane
import com.codespace.ide.ui.panes.ExplorerPane
import com.codespace.ide.ui.panes.SourceControlPane
import com.codespace.ide.ui.panes.TerminalPane

private enum class Pane(val label: String, val icon: ImageVector) {
    EXPLORER("Explorer", Icons.Default.Folder),
    EDITOR("Editor", Icons.Default.Edit),
    TERMINAL("Terminal", Icons.Default.Terminal),
    SCM("Git", Icons.Default.AccountTree),
    AI("AI", Icons.Default.AutoAwesome),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectShellScreen(
    projectId: String,
    isDark: Boolean,
    onToggleTheme: () -> Unit,
    onBack: () -> Unit,
    tokenStore: SecureTokenStore,
) {
    var current by remember { mutableStateOf(Pane.EDITOR) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("project • $projectId") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onToggleTheme) {
                        Icon(
                            if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Toggle theme",
                        )
                    }
                },
            )
        },
        bottomBar = {
            NavigationBar {
                Pane.entries.forEach { pane ->
                    NavigationBarItem(
                        selected = current == pane,
                        onClick = { current = pane },
                        icon = { Icon(pane.icon, contentDescription = pane.label) },
                        label = { Text(pane.label) },
                    )
                }
            }
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (current) {
                Pane.EXPLORER -> ExplorerPane(onOpenFile = { current = Pane.EDITOR })
                Pane.EDITOR -> EditorPane()
                Pane.TERMINAL -> TerminalPane()
                Pane.SCM -> SourceControlPane()
                Pane.AI -> AiAssistantPane(tokenStore = tokenStore)
            }
        }
    }
}

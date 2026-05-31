package com.codespace.ide.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codespace.ide.data.SecureTokenStore
import com.codespace.ide.ui.panes.AiAssistantPane
import com.codespace.ide.ui.panes.EditorPane
import com.codespace.ide.ui.panes.ExplorerPane
import com.codespace.ide.ui.panes.SourceControlPane
import com.codespace.ide.ui.panes.TerminalPane

private val VsCodeBg = Color(0xFF1E1E1E)
private val VsCodeSidebar = Color(0xFF252526)
private val VsCodeActivityBar = Color(0xFF333333)
private val VsCodeStatusBar = Color(0xFF007ACC)
private val VsCodeSelected = Color(0xFF37373D)
private val VsCodeText = Color(0xFFCCCCCC)
private val VsCodeIcon = Color(0xFF858585)
private val VsCodeIconActive = Color(0xFFFFFFFF)

private enum class Panel {
    EXPLORER, SEARCH, GIT, TERMINAL, AI, NONE
}

private data class ActivityItem(
    val panel: Panel,
    val icon: ImageVector,
    val label: String,
)

@Composable
fun ProjectShellScreen(
    projectId: String,
    isDark: Boolean,
    onToggleTheme: () -> Unit,
    onBack: () -> Unit,
    tokenStore: SecureTokenStore,
) {
    var activePanel by remember { mutableStateOf(Panel.EXPLORER) }
    var showPanel by remember { mutableStateOf(true) }

    val activityItems = listOf(
        ActivityItem(Panel.EXPLORER, Icons.Default.Folder, "Explorer"),
        ActivityItem(Panel.SEARCH, Icons.Default.Search, "Search"),
        ActivityItem(Panel.GIT, Icons.Default.AccountTree, "Source Control"),
        ActivityItem(Panel.TERMINAL, Icons.Default.Terminal, "Terminal"),
        ActivityItem(Panel.AI, Icons.Default.AutoAwesome, "AI Assistant"),
    )

    Column(Modifier.fillMaxSize().background(VsCodeBg)) {
        // Main area
        Row(Modifier.weight(1f).fillMaxWidth()) {

            // Activity Bar (left icon strip)
            Column(
                Modifier
                    .width(48.dp)
                    .fillMaxHeight()
                    .background(VsCodeActivityBar),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                activityItems.forEach { item ->
                    val isActive = activePanel == item.panel
                    Box(
                        Modifier
                            .size(48.dp)
                            .background(if (isActive) VsCodeSelected else Color.Transparent)
                            .clickable {
                                if (activePanel == item.panel) {
                                    showPanel = !showPanel
                                } else {
                                    activePanel = item.panel
                                    showPanel = true
                                }
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            item.icon,
                            contentDescription = item.label,
                            tint = if (isActive) VsCodeIconActive else VsCodeIcon,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }

                // Spacer
                Box(Modifier.weight(1f))

                // Settings at bottom
                Box(
                    Modifier
                        .size(48.dp)
                        .clickable { onToggleTheme() },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = "Toggle theme",
                        tint = VsCodeIcon,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }

            VerticalDivider(color = Color(0xFF444444))

            // Side Panel
            if (showPanel && activePanel != Panel.NONE) {
                Column(
                    Modifier
                        .width(260.dp)
                        .fillMaxHeight()
                        .background(VsCodeSidebar)
                ) {
                    // Panel title
                    Text(
                        activityItems.first { it.panel == activePanel }.label.uppercase(),
                        color = Color(0xFFBBBBBB),
                        fontSize = 11.sp,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                    HorizontalDivider(color = Color(0xFF444444))

                    when (activePanel) {
                        Panel.EXPLORER -> ExplorerPane(onOpenFile = {})
                        Panel.GIT -> SourceControlPane()
                        Panel.TERMINAL -> TerminalPane()
                        Panel.AI -> AiAssistantPane(tokenStore = tokenStore)
                        else -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Coming soon", color = VsCodeText)
                        }
                    }
                }
                VerticalDivider(color = Color(0xFF444444))
            }

            // Editor area
            Column(Modifier.weight(1f).fillMaxHeight()) {
                EditorPane()
            }
        }

        HorizontalDivider(color = Color(0xFF444444))

        // Status bar
        Row(
            Modifier
                .fillMaxWidth()
                .background(VsCodeStatusBar)
                .padding(horizontal = 8.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "⎇  main",
                color = Color.White,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
            )
            Text(
                "  •  $projectId",
                color = Color.White,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
            )
            Box(Modifier.weight(1f))
            Text(
                if (isDark) "Dark" else "Light",
                color = Color.White,
                fontSize = 12.sp,
            )
        }
    }
}

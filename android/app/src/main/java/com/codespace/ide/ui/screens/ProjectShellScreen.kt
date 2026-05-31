package com.codespace.ide.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
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
private val VsCodeTabBar = Color(0xFF252526)
private val VsCodeTabActive = Color(0xFF1E1E1E)
private val VsCodeTabInactive = Color(0xFF2D2D2D)
private val VsCodeBottomPanel = Color(0xFF1E1E1E)
private val VsCodeDivider = Color(0xFF444444)

private enum class SidePanel { EXPLORER, SEARCH, GIT, RUN, EXTENSIONS, AI, NONE }
private enum class BottomTab { PROBLEMS, OUTPUT, TERMINAL }

private data class ActivityItem(val panel: SidePanel, val icon: ImageVector, val label: String)

@Composable
fun ProjectShellScreen(
    projectId: String,
    isDark: Boolean,
    onToggleTheme: () -> Unit,
    onBack: () -> Unit,
    tokenStore: SecureTokenStore,
) {
    var activePanel by remember { mutableStateOf(SidePanel.EXPLORER) }
    var showSidePanel by remember { mutableStateOf(true) }
    var activeBottomTab by remember { mutableStateOf(BottomTab.TERMINAL) }
    var showBottomPanel by remember { mutableStateOf(true) }

    // Draggable widths
    var totalWidth by remember { mutableFloatStateOf(1080f) }
    var sidePanelWidth by remember { mutableFloatStateOf(260f) }
    var bottomPanelHeight by remember { mutableFloatStateOf(250f) }
    var totalHeight by remember { mutableFloatStateOf(1920f) }
    val density = LocalDensity.current

    val activityItems = listOf(
        ActivityItem(SidePanel.EXPLORER, Icons.Default.Folder, "Explorer"),
        ActivityItem(SidePanel.SEARCH, Icons.Default.Search, "Search"),
        ActivityItem(SidePanel.GIT, Icons.Default.AccountTree, "Source Control"),
        ActivityItem(SidePanel.RUN, Icons.Default.BugReport, "Run & Debug"),
        ActivityItem(SidePanel.EXTENSIONS, Icons.Default.Extension, "Extensions"),
        ActivityItem(SidePanel.AI, Icons.Default.AutoAwesome, "AI Assistant"),
    )

    Column(
        Modifier
            .fillMaxSize()
            .background(VsCodeBg)
            .onGloballyPositioned {
                totalWidth = it.size.width.toFloat()
                totalHeight = it.size.height.toFloat()
            }
    ) {
        // Main area (activity bar + side panel + editor)
        Row(Modifier.weight(1f).fillMaxWidth()) {

            // Activity Bar
            Column(
                Modifier
                    .width(48.dp)
                    .fillMaxHeight()
                    .background(VsCodeActivityBar),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                activityItems.forEach { item ->
                    val isActive = activePanel == item.panel && showSidePanel
                    Box(
                        Modifier
                            .size(48.dp)
                            .background(if (isActive) VsCodeSelected else Color.Transparent)
                            .clickable {
                                if (activePanel == item.panel) {
                                    showSidePanel = !showSidePanel
                                } else {
                                    activePanel = item.panel
                                    showSidePanel = true
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
                Box(Modifier.weight(1f))
                // Account icon
                Box(
                    Modifier.size(48.dp).clickable { },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.Person, contentDescription = "Account", tint = VsCodeIcon, modifier = Modifier.size(24.dp))
                }
                // Settings icon
                Box(
                    Modifier.size(48.dp).clickable { onToggleTheme() },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings", tint = VsCodeIcon, modifier = Modifier.size(24.dp))
                }
            }

            VerticalDivider(color = VsCodeDivider, modifier = Modifier.fillMaxHeight())

            // Side Panel with draggable divider
            if (showSidePanel) {
                val sidePanelWidthDp = with(density) { sidePanelWidth.toDp() }
                Column(
                    Modifier
                        .width(sidePanelWidthDp.coerceIn(150.dp, 500.dp))
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
                    HorizontalDivider(color = VsCodeDivider)
                    Box(Modifier.fillMaxSize()) {
                        when (activePanel) {
                            SidePanel.EXPLORER -> ExplorerPane(onOpenFile = {})
                            SidePanel.GIT -> SourceControlPane()
                            SidePanel.AI -> AiAssistantPane(tokenStore = tokenStore)
                            else -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("Coming soon", color = VsCodeText, fontSize = 13.sp)
                            }
                        }
                    }
                }

                // Draggable vertical divider
                Box(
                    Modifier
                        .width(4.dp)
                        .fillMaxHeight()
                        .background(VsCodeDivider)
                        .pointerInput(Unit) {
                            detectDragGestures { _, dragAmount ->
                                sidePanelWidth = (sidePanelWidth + dragAmount.x)
                                    .coerceIn(150f, totalWidth - 200f)
                            }
                        }
                )
            }

            // Editor + Bottom Panel
            Column(Modifier.weight(1f).fillMaxHeight()) {
                // Editor tabs
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(35.dp)
                        .background(VsCodeTabBar)
                        .horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Sample tabs
                    listOf("index.ts", "main.py").forEachIndexed { i, name ->
                        val isActive = i == 0
                        Row(
                            Modifier
                                .background(if (isActive) VsCodeTabActive else VsCodeTabInactive)
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                name,
                                color = if (isActive) VsCodeIconActive else VsCodeIcon,
                                fontSize = 13.sp,
                                fontWeight = if (isActive) FontWeight.Normal else FontWeight.Light,
                            )
                            Text(
                                "  ×",
                                color = VsCodeIcon,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(start = 4.dp),
                            )
                        }
                        VerticalDivider(color = VsCodeDivider)
                    }
                }
                HorizontalDivider(color = VsCodeDivider)

                // Editor area
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    EditorPane()
                }

                // Draggable horizontal divider for bottom panel
                if (showBottomPanel) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .background(VsCodeDivider)
                            .pointerInput(Unit) {
                                detectDragGestures { _, dragAmount ->
                                    bottomPanelHeight = (bottomPanelHeight - dragAmount.y)
                                        .coerceIn(100f, totalHeight - 300f)
                                }
                            }
                    )

                    // Bottom panel
                    val bottomPanelHeightDp = with(density) { bottomPanelHeight.toDp() }
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .height(bottomPanelHeightDp.coerceIn(80.dp, 500.dp))
                            .background(VsCodeBottomPanel)
                    ) {
                        // Bottom tabs
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .background(VsCodeTabBar),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            BottomTab.entries.forEach { tab ->
                                val isActive = activeBottomTab == tab
                                Column(
                                    Modifier.clickable { activeBottomTab = tab }
                                ) {
                                    Text(
                                        tab.name,
                                        color = if (isActive) VsCodeIconActive else VsCodeIcon,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    )
                                    if (isActive) {
                                        Box(
                                            Modifier
                                                .fillMaxWidth()
                                                .height(1.dp)
                                                .background(VsCodeStatusBar)
                                        )
                                    }
                                }
                            }
                        }
                        HorizontalDivider(color = VsCodeDivider)

                        // Bottom panel content
                        Box(Modifier.fillMaxSize()) {
                            when (activeBottomTab) {
                                BottomTab.TERMINAL -> TerminalPane()
                                BottomTab.PROBLEMS -> Box(
                                    Modifier.fillMaxSize().padding(16.dp),
                                    contentAlignment = Alignment.TopStart,
                                ) {
                                    Text("No problems detected.", color = VsCodeText, fontSize = 13.sp)
                                }
                                BottomTab.OUTPUT -> Box(
                                    Modifier.fillMaxSize().padding(16.dp),
                                    contentAlignment = Alignment.TopStart,
                                ) {
                                    Text("No output.", color = VsCodeText, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        HorizontalDivider(color = VsCodeDivider)

        // Status bar
        Row(
            Modifier
                .fillMaxWidth()
                .height(22.dp)
                .background(VsCodeStatusBar)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("⎇  main", color = Color.White, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
            Text("  •  $projectId", color = Color.White, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
            Box(Modifier.weight(1f))
            Text(
                if (isDark) "Dark+" else "Light+",
                color = Color.White,
                fontSize = 12.sp,
                modifier = Modifier.clickable { onToggleTheme() },
            )
            Text("  Ln 1, Col 1", color = Color.White, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
        }
    }
}

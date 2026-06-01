package com.codespace.ide.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MoreHoriz
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
import androidx.compose.ui.text.style.TextAlign
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

    var totalWidth by remember { mutableFloatStateOf(1080f) }
    var totalHeight by remember { mutableFloatStateOf(1920f) }
    var sidePanelWidth by remember { mutableFloatStateOf(260f) }
    var bottomPanelHeight by remember { mutableFloatStateOf(250f) }
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
        // 1. VS CODE TOP WINDOW TITLE BAR
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(38.dp)
                .background(VsCodeSidebar),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(modifier = Modifier.width(12.dp))
            
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Back",
                tint = VsCodeIcon,
                modifier = Modifier
                    .size(16.dp)
                    .clickable { onBack() }
            )
            Spacer(modifier = Modifier.width(12.dp))
            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = "Forward",
                tint = VsCodeIcon.copy(alpha = 0.5f),
                modifier = Modifier.size(16.dp)
            )
            
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 24.dp, vertical = 5.dp)
                    .fillMaxHeight()
                    .background(Color(0xFF2D2D2D), shape = RoundedCornerShape(4.dp))
                    .border(1.dp, VsCodeDivider, shape = RoundedCornerShape(4.dp)),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                ) {
                    Icon(Icons.Default.Search, contentDescription = null, tint = VsCodeIcon, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "codespace-ide-mobile [Workspace]",
                        color = VsCodeText,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            
            Icon(Icons.Default.MoreHoriz, contentDescription = "Layout Options", tint = VsCodeIcon, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(12.dp))
        }
        
        HorizontalDivider(color = VsCodeDivider)

        // 2. MAIN CORE LAYOUT
        Row(Modifier.weight(1f).fillMaxWidth()) {

            // ACTIVITY BAR
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
                        if (isActive) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .width(2.dp)
                                    .background(VsCodeStatusBar)
                                    .align(Alignment.CenterStart)
                            )
                        }
                        Icon(
                            item.icon,
                            contentDescription = item.label,
                            tint = if (isActive) VsCodeIconActive else VsCodeIcon,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
                
                Box(Modifier.weight(1f))
                
                Box(Modifier.size(48.dp).clickable { }, contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Person, contentDescription = "Account", tint = VsCodeIcon, modifier = Modifier.size(22.dp))
                }
                
                Box(Modifier.size(48.dp).clickable { onToggleTheme() }, contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings", tint = VsCodeIcon, modifier = Modifier.size(22.dp))
                }
            }

            VerticalDivider(color = VsCodeDivider, modifier = Modifier.fillMaxHeight())

            // PRIMARY SIDE BAR ACCORDION
            if (showSidePanel) {
                val sidePanelWidthDp = with(density) { sidePanelWidth.toDp() }
                Column(
                    Modifier
                        .width(sidePanelWidthDp.coerceIn(160.dp, 450.dp))
                        .fillMaxHeight()
                        .background(VsCodeSidebar)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = activityItems.first { it.panel == activePanel }.label.uppercase(),
                            color = Color(0xFFBBBBBB),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            modifier = Modifier.weight(1f),
                        )
                        Icon(Icons.Default.MoreHoriz, contentDescription = null, tint = VsCodeIcon, modifier = Modifier.size(14.dp))
                    }
                    HorizontalDivider(color = VsCodeDivider)
                    
                    Box(Modifier.fillMaxSize()) {
                        when (activePanel) {
                            SidePanel.EXPLORER -> ExplorerPane(onOpenFile = {})
                            SidePanel.GIT -> SourceControlPane()
                            SidePanel.AI -> AiAssistantPane(tokenStore = tokenStore)
                            else -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("Feature panel staging", color = VsCodeIcon, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }
                }

                // Vertical split-pane line
                Box(
                    Modifier
                        .width(3.dp)
                        .fillMaxHeight()
                        .background(VsCodeDivider)
                        .pointerInput(Unit) {
                            detectDragGestures { _, dragAmount ->
                                sidePanelWidth = (sidePanelWidth + dragAmount.x)
                                    .coerceIn(160f, totalWidth - 200f)
                            }
                        }
                )
            }

            // RIGHT HAND BLOCK
            Column(Modifier.weight(1f).fillMaxHeight()) {
                
                // EDITOR WORKSPACE CONTAINER TABS
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(35.dp)
                        .background(VsCodeTabBar)
                        .horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    listOf("ProjectShellScreen.kt", "TerminalPane.kt").forEachIndexed { i, name ->
                        val isActive = i == 0
                        Row(
                            Modifier
                                .background(if (isActive) VsCodeTabActive else VsCodeTabInactive)
                                .fillMaxHeight()
                                .padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (isActive) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(2.dp)
                                        .background(VsCodeStatusBar)
                                        .align(Alignment.Top)
                                )
                            }
                            Text(
                                text = name,
                                color = if (isActive) VsCodeIconActive else VsCodeIcon,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = if (isActive) FontWeight.Normal else FontWeight.Light,
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = VsCodeIcon,
                                modifier = Modifier.size(10.dp)
                            )
                        }
                        // Added simple divider instead of one-sided border modifier
                        Box(Modifier.width(1.dp).fillMaxHeight().background(VsCodeDivider))
                    }
                }
                HorizontalDivider(color = VsCodeDivider)

                // MAIN EDITOR WINDOW
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    EditorPane()
                }

                // BOTTOM PANEL
                if (showBottomPanel) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .background(VsCodeDivider)
                            .pointerInput(Unit) {
                                detectDragGestures { _, dragAmount ->
                                    bottomPanelHeight = (bottomPanelHeight - dragAmount.y)
                                        .coerceIn(100f, totalHeight - 250f)
                                }
                            }
                    )

                    val bottomPanelHeightDp = with(density) { bottomPanelHeight.toDp() }
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .height(bottomPanelHeightDp.coerceIn(100.dp, 450.dp))
                            .background(VsCodeBottomPanel)
                    ) {
                        // BOTTOM TOOL PANEL HEADER
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .height(32.dp)
                                .background(VsCodeTabBar),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            BottomTab.entries.forEach { tab ->
                                val isActive = activeBottomTab == tab
                                Column(
                                    Modifier
                                        .fillMaxHeight()
                                        .clickable { activeBottomTab = tab }
                                        .padding(horizontal = 14.dp),
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = tab.name,
                                        color = if (isActive) VsCodeIconActive else VsCodeIcon,
                                        fontSize = 11.sp,
                                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                        fontFamily = FontFamily.Monospace,
                                        letterSpacing = 0.5.sp
                                    )
                                    if (isActive) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Box(
                                            Modifier
                                                .width(28.dp)
                                                .height(2.dp)
                                                .background(VsCodeStatusBar)
                                        )
                                    }
                                }
                            }
                        }
                        HorizontalDivider(color = VsCodeDivider)

                        Box(Modifier.fillMaxSize()) {
                            when (activeBottomTab) {
                                BottomTab.TERMINAL -> TerminalPane()
                                BottomTab.PROBLEMS -> Box(
                                    Modifier.fillMaxSize().padding(12.dp)
                                ) {
                                    Text("No diagnostics compiler alerts detected in directory.", color = VsCodeIcon, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                                }
                                BottomTab.OUTPUT -> Box(
                                    Modifier.fillMaxSize().padding(12.dp)
                                ) {
                                    Text("Initializing environment output streams... ready.", color = VsCodeIcon, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                                }
                            }
                        }
                    }
                }
            }
        }

        HorizontalDivider(color = VsCodeDivider)

        // 3. BLUE SYSTEM ENVIRONMENT STATUS BAR
        Row(
            Modifier
                .fillMaxWidth()
                .height(22.dp)
                .background(VsCodeStatusBar)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
            Text("⎇ main", color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            Text("  •  $projectId", color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            
            Box(Modifier.weight(1f))
            
            Text(
                text = if (isDark) "Dark Modern" else "Light Modern",
                color = Color.White,
                fontSize = 11.sp,
                modifier = Modifier.clickable { onToggleTheme() },
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text("Ln 1, Col 1", color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        }
    }
}

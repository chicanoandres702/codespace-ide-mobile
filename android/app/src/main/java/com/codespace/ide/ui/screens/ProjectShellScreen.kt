package com.codespace.ide.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codespace.ide.data.SecureTokenStore
import com.codespace.ide.ui.panes.*

private val BgColor = Color(0xFFFFFFFF)
private val SidePanelBg = Color(0xFFFFFFFF)
private val ActivityBarBg = Color(0xFFFFFFFF)
private val ActivityBarIcon = Color(0xFF424242)
private val ActivityBarIconActive = Color(0xFF007ACC)
private val TabBarBg = Color(0xFFECECEC)
private val TabActiveBg = Color(0xFFFFFFFF)
private val TabInactiveBg = Color(0xFFECECEC)
private val TabActiveIndicator = Color(0xFF007ACC)
private val TabText = Color(0xFF333333)
private val TabTextInactive = Color(0xFF717171)
private val DividerColor = Color(0xFFE0E0E0)
private val StatusBarBg = Color(0xFFFFFFFF)
private val PanelBg = Color(0xFFFFFFFF)
private val SectionHeaderText = Color(0xFF717171)
private val FloatingMenuBg = Color(0xFFFFFFFF)
private val FloatingMenuBorder = Color(0xFFD4D4D4)
private val FloatingMenuText = Color(0xFF333333)
private val CmdSelectedBg = Color(0xFF0060C0)
private val CmdSelectedText = Color(0xFFFFFFFF)
private val BlueBtn = Color(0xFF0060C0)

private enum class SidePanel { EXPLORER, SEARCH, GIT, RUN, EXTENSIONS, NONE }
private enum class BottomTab { PROBLEMS, OUTPUT, TERMINAL }

@Composable
fun ProjectShellScreen(
    projectId: String,
    isDark: Boolean,
    onToggleTheme: () -> Unit,
    onBack: () -> Unit,
    tokenStore: SecureTokenStore,
) {
    val density = LocalDensity.current
    var activePanel by remember { mutableStateOf<SidePanel?>(null) }
    var showAiPanel by remember { mutableStateOf(false) }
    var activeBottomTab by remember { mutableStateOf(BottomTab.TERMINAL) }
    var totalWidth by remember { mutableFloatStateOf(1080f) }
    var totalHeight by remember { mutableFloatStateOf(1920f) }
    var sidePanelWidth by remember { mutableFloatStateOf(280f) }
    var bottomPanelHeight by remember { mutableFloatStateOf(300f) }
    var aiPanelWidth by remember { mutableFloatStateOf(300f) }
    var showCommandPalette by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    var showPersonMenu by remember { mutableStateOf(false) }
    var showPersonSubMenu by remember { mutableStateOf(false) }
    var showGearMenu by remember { mutableStateOf(false) }
    var showGearThemeMenu by remember { mutableStateOf(false) }
    var showColorTheme by remember { mutableStateOf(false) }
    var showRunMenu by remember { mutableStateOf(false) }
    // showPanelMenu is the "..." menu in the bottom tab bar (context-aware per tab)
    var showPanelMenu by remember { mutableStateOf(false) }
    var showExplorerMore by remember { mutableStateOf(false) }
    var commandQuery by remember { mutableStateOf("") }
    var commandTab by remember { mutableStateOf("Commands") }
    val editorTabs = remember { mutableStateListOf<String>() }
    var activeEditorTab by remember { mutableStateOf<String?>(null) }

    Box(
        Modifier.fillMaxSize().background(BgColor)
            .onGloballyPositioned { totalWidth = it.size.width.toFloat(); totalHeight = it.size.height.toFloat() }
    ) {
        Column(Modifier.fillMaxSize().padding(top = 36.dp)) {
            Row(Modifier.weight(1f).fillMaxWidth()) {
                // Activity Bar
                Column(
                    Modifier.width(48.dp).fillMaxHeight().background(ActivityBarBg),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    listOf(
                        SidePanel.EXPLORER to Icons.Default.Folder,
                        SidePanel.SEARCH to Icons.Default.Search,
                        SidePanel.GIT to Icons.Default.AccountTree,
                        SidePanel.RUN to (if (activePanel == SidePanel.EXTENSIONS) Icons.Default.Extension else Icons.Default.BugReportOutlined),
                    ).forEach { (panel, icon) ->
                        val realPanel = if (panel == SidePanel.RUN && activePanel == SidePanel.EXTENSIONS) SidePanel.EXTENSIONS else panel
                        val isActive = activePanel == realPanel
                        Box(
                            Modifier.fillMaxWidth().height(48.dp).clickable {
                                activePanel = if (activePanel == realPanel) null else realPanel
                            },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (isActive) Box(Modifier.width(2.dp).height(24.dp).align(Alignment.CenterStart).background(Color(0xFF007ACC)))
                            Icon(icon, null, tint = if (isActive) ActivityBarIconActive else ActivityBarIcon, modifier = Modifier.size(24.dp))
                        }
                    }
                    Box(
                        Modifier.fillMaxWidth().height(48.dp).clickable { showMoreMenu = true },
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Default.MoreHoriz, null, tint = ActivityBarIcon, modifier = Modifier.size(24.dp)) }
                    Spacer(Modifier.weight(1f))
                    Box(Modifier.fillMaxWidth().height(48.dp).clickable { showPersonMenu = true }, contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Person, null, tint = ActivityBarIcon, modifier = Modifier.size(24.dp))
                    }
                    Box(Modifier.fillMaxWidth().height(48.dp).clickable { showGearMenu = true }, contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Settings, null, tint = ActivityBarIcon, modifier = Modifier.size(24.dp))
                    }
                    Spacer(Modifier.height(4.dp))
                }

                // Side Panel
                if (activePanel != null) {
                    val spWidth = with(density) { sidePanelWidth.toDp() }.coerceIn(150.dp, 600.dp)
                    Column(Modifier.width(spWidth).fillMaxHeight().background(SidePanelBg)) {
                        when (activePanel) {
                            SidePanel.EXPLORER -> ExplorerSidePanel(
                                onOpenFile = { name -> if (!editorTabs.contains(name)) editorTabs.add(name); activeEditorTab = name },
                                onMoreMenu = { showExplorerMore = true }
                            )
                            SidePanel.SEARCH -> SearchPanel()
                            SidePanel.GIT -> GitSidePanel()
                            SidePanel.RUN -> RunDebugPanel(onMoreMenu = { showRunMenu = true })
                            SidePanel.EXTENSIONS -> ExtensionsPanel()
                            else -> {}
                        }
                    }
                    Box(
                        Modifier.width(4.dp).fillMaxHeight().background(DividerColor)
                            .pointerInput(Unit) {
                                detectDragGestures { _, dragAmount ->
                                    val nw = sidePanelWidth + dragAmount.x
                                    if (nw < 80f) activePanel = null
                                    else sidePanelWidth = nw.coerceIn(80f, totalWidth * 0.7f)
                                }
                            }
                    )
                }

                // Editor + AI
                Row(Modifier.weight(1f).fillMaxHeight()) {
                    Column(Modifier.weight(1f).fillMaxHeight()) {
                        if (editorTabs.isNotEmpty()) {
                            Row(
                                Modifier.fillMaxWidth().height(35.dp).background(TabBarBg)
                                    .horizontalScroll(rememberScrollState()),
                                verticalAlignment = Alignment.Bottom,
                            ) {
                                editorTabs.forEach { tab ->
                                    val isActive = tab == activeEditorTab
                                    Column(Modifier.clickable { activeEditorTab = tab }.background(if (isActive) TabActiveBg else TabInactiveBg)) {
                                        Row(Modifier.padding(horizontal = 12.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Text(tab, fontSize = 13.sp, color = if (isActive) TabText else TabTextInactive, maxLines = 1)
                                            Spacer(Modifier.width(6.dp))
                                            Icon(Icons.Default.Close, null, tint = TabTextInactive,
                                                modifier = Modifier.size(14.dp).clickable {
                                                    editorTabs.remove(tab)
                                                    if (activeEditorTab == tab) activeEditorTab = editorTabs.lastOrNull()
                                                })
                                        }
                                        if (isActive) Box(Modifier.fillMaxWidth().height(1.dp).background(TabActiveIndicator))
                                        else Spacer(Modifier.height(1.dp))
                                    }
                                }
                            }
                        }

                        Box(Modifier.weight(1f).fillMaxWidth()) {
                            if (activeEditorTab != null) {
                                EditorPane(openFilePath = activeEditorTab)
                            } else {
                                Box(Modifier.fillMaxSize().background(BgColor), contentAlignment = Alignment.Center) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("CodeSpace IDE", fontSize = 32.sp, color = Color(0xFFDDDDDD),
                                            fontWeight = FontWeight.Light, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                    }
                                }
                            }
                        }

                        // ── BOTTOM PANEL ──────────────────────────────────────────────
                        // Drag handle to resize. Tab bar always visible.
                        // Active tab = blue outlined pill. Content switches on tap.
                        // All 3 panels stay composed — terminal session never dies.

                        // Resize drag handle
                        Box(
                            Modifier.fillMaxWidth().height(4.dp).background(DividerColor)
                                .pointerInput(Unit) {
                                    detectDragGestures { _, dragAmount ->
                                        bottomPanelHeight = (bottomPanelHeight - dragAmount.y)
                                            .coerceIn(80f, totalHeight * 0.75f)
                                    }
                                }
                        )

                        // Tab bar — always on screen
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF3F3F3))
                                .height(36.dp)
                                .padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            BottomTab.entries.forEach { tab ->
                                val isActive = tab == activeBottomTab
                                Box(
                                    Modifier
                                        .clickable { activeBottomTab = tab }
                                        .background(
                                            color = if (isActive) Color(0xFFDCEAFB) else Color.Transparent,
                                            shape = RoundedCornerShape(4.dp),
                                        )
                                        .border(
                                            width = if (isActive) 1.dp else 0.dp,
                                            color = if (isActive) Color(0xFF007ACC) else Color.Transparent,
                                            shape = RoundedCornerShape(4.dp),
                                        )
                                        .padding(horizontal = 10.dp, vertical = 4.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        tab.name,
                                        fontSize = 12.sp,
                                        color = if (isActive) Color(0xFF007ACC) else Color(0xFF717171),
                                        fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                                    )
                                }
                                Spacer(Modifier.width(4.dp))
                            }
                            Spacer(Modifier.weight(1f))
                            Icon(Icons.Default.MoreHoriz, null, tint = TabTextInactive,
                                modifier = Modifier.size(16.dp).clickable { showPanelMenu = true })
                            Spacer(Modifier.width(8.dp))
                            Icon(Icons.Default.Fullscreen, null, tint = TabTextInactive,
                                modifier = Modifier.size(16.dp).clickable {
                                    bottomPanelHeight = if (bottomPanelHeight > totalHeight * 0.5f) 260f else totalHeight * 0.75f
                                })
                            Spacer(Modifier.width(8.dp))
                        }

                        HorizontalDivider(color = DividerColor)

                        // Panel content — fixed height, all 3 always in tree
                        val bh = with(density) { bottomPanelHeight.toDp() }.coerceIn(80.dp, 600.dp)
                        Box(Modifier.fillMaxWidth().height(bh).background(PanelBg)) {
                            Box(
                                Modifier.fillMaxSize()
                                    .graphicsLayer { alpha = if (activeBottomTab == BottomTab.TERMINAL) 1f else 0f }
                                    .then(if (activeBottomTab == BottomTab.TERMINAL) Modifier else Modifier.pointerInput(Unit) {})
                            ) { TerminalPane() }
                            Box(
                                Modifier.fillMaxSize()
                                    .graphicsLayer { alpha = if (activeBottomTab == BottomTab.PROBLEMS) 1f else 0f }
                                    .then(if (activeBottomTab == BottomTab.PROBLEMS) Modifier else Modifier.pointerInput(Unit) {})
                            ) { ProblemsPanel() }
                            Box(
                                Modifier.fillMaxSize()
                                    .graphicsLayer { alpha = if (activeBottomTab == BottomTab.OUTPUT) 1f else 0f }
                                    .then(if (activeBottomTab == BottomTab.OUTPUT) Modifier else Modifier.pointerInput(Unit) {})
                            ) { OutputPanel() }
                        }


                    if (showAiPanel) {
                        val aw = with(density) { aiPanelWidth.toDp() }.coerceIn(200.dp, 500.dp)
                        Box(
                            Modifier.width(4.dp).fillMaxHeight().background(DividerColor)
                                .pointerInput(Unit) {
                                    detectDragGestures { _, dragAmount ->
                                        aiPanelWidth = (aiPanelWidth - dragAmount.x).coerceIn(150f, totalWidth * 0.5f)
                                    }
                                }
                        )
                        Column(Modifier.width(aw).fillMaxHeight().background(BgColor)) {
                            Row(Modifier.fillMaxWidth().background(TabBarBg).padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("CHAT", fontSize = 11.sp, color = SectionHeaderText, modifier = Modifier.weight(1f))
                                Icon(Icons.Default.Close, null, tint = TabTextInactive,
                                    modifier = Modifier.size(16.dp).clickable { showAiPanel = false })
                            }
                            HorizontalDivider(color = DividerColor)
                            AiAssistantPane(tokenStore = tokenStore)
                        }
                    }
                }
            }

            // Status Bar
            Row(
                Modifier.fillMaxWidth().height(22.dp).background(StatusBarBg).padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Toggle bottom panel visibility
                Icon(Icons.Default.CompareArrows, null, tint = Color(0xFF424242),
                    modifier = Modifier.size(14.dp).clickable { bottomPanelHeight = if (bottomPanelHeight > 80f) 0f else 260f })
                Spacer(Modifier.width(4.dp))
                Icon(Icons.Default.Close, null, tint = Color(0xFF424242), modifier = Modifier.size(12.dp))
                Text(" 0", color = Color(0xFF424242), fontSize = 11.sp)
                Spacer(Modifier.width(4.dp))
                Icon(Icons.Default.Warning, null, tint = Color(0xFF424242), modifier = Modifier.size(12.dp))
                Text(" 0", color = Color(0xFF424242), fontSize = 11.sp)
                Spacer(Modifier.weight(1f))
                Text("⎇  main", color = Color(0xFF424242), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                Spacer(Modifier.width(12.dp))
                Text("Layout: us", color = Color(0xFF424242), fontSize = 11.sp)
                Spacer(Modifier.width(8.dp))
                Icon(Icons.Default.Notifications, null, tint = Color(0xFF424242), modifier = Modifier.size(14.dp))
            }
        }

        // Top Bar
        Row(
            Modifier.fillMaxWidth().height(36.dp).background(Color(0xFFF8F8F8)).align(Alignment.TopCenter),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(Modifier.width(52.dp))
            Icon(Icons.Default.ArrowBack, null, tint = Color(0xFF717171), modifier = Modifier.size(20.dp).clickable { onBack() })
            Icon(Icons.Default.ArrowForward, null, tint = Color(0xFFCCCCCC), modifier = Modifier.size(20.dp))
            Box(
                Modifier.weight(1f).fillMaxHeight().clickable { showCommandPalette = true }.padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(projectId.ifBlank { "Workspace" }, fontSize = 13.sp, color = TabText, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Box(
                Modifier.size(36.dp).background(if (showAiPanel) Color(0xFF007ACC) else Color.Transparent).clickable { showAiPanel = !showAiPanel },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.Stars, null, tint = if (showAiPanel) Color.White else Color(0xFF717171), modifier = Modifier.size(18.dp))
            }
        }

        // Command Palette
        if (showCommandPalette) {
            val allCmds = listOf("Clear Terminal","Close All Editors","Color Theme","Command Palette",
                "Extensions: Install Extensions","File: New File","File: Open File","File: Open Folder",
                "Git: Clone","Git: Commit","Git: Push","Open Settings","Terminal: Create New Terminal",
                "View: Toggle Terminal","Workbench: Toggle Full Screen","Accounts: Manage Accounts",
                "Add Data Breakpoint at Address","Add Function Breakpoint")
            val filtered = allCmds.filter { it.contains(commandQuery, ignoreCase = true) }
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)).clickable { showCommandPalette = false; commandQuery = "" }, contentAlignment = Alignment.TopCenter) {
                Column(
                    Modifier.fillMaxWidth(0.92f).padding(top = 44.dp)
                        .background(FloatingMenuBg, RoundedCornerShape(8.dp))
                        .border(1.dp, FloatingMenuBorder, RoundedCornerShape(8.dp))
                        .clickable { }
                ) {
                    OutlinedTextField(value = commandQuery, onValueChange = { commandQuery = it },
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                        placeholder = { Text("Search commands...", fontSize = 14.sp) }, singleLine = true)
                    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)) {
                        listOf("Commands", "Files").forEach { tab ->
                            val isActive = tab == commandTab
                            Box(
                                Modifier.clickable { commandTab = tab }
                                    .background(if (isActive) BlueBtn else Color.Transparent, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) { Text(tab, fontSize = 13.sp, color = if (isActive) Color.White else TabTextInactive) }
                            Spacer(Modifier.width(4.dp))
                        }
                        Spacer(Modifier.weight(1f))
                        Box(Modifier.clickable { showAiPanel = true; showCommandPalette = false }
                            .background(BlueBtn, RoundedCornerShape(4.dp)).padding(horizontal = 12.dp, vertical = 6.dp)) {
                            Text("Open Chat", fontSize = 13.sp, color = Color.White)
                        }
                    }
                    HorizontalDivider(color = DividerColor)
                    LazyColumn(Modifier.heightIn(max = 320.dp)) {
                        items(filtered.take(12)) { cmd ->
                            val isFirst = cmd == filtered.firstOrNull()
                            Row(
                                Modifier.fillMaxWidth()
                                    .background(if (isFirst) CmdSelectedBg else Color.Transparent)
                                    .clickable { showCommandPalette = false; commandQuery = "" }
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(cmd, fontSize = 13.sp, color = if (isFirst) CmdSelectedText else FloatingMenuText, modifier = Modifier.weight(1f))
                                if (isFirst) Icon(Icons.Default.Settings, null, tint = Color.White, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }
            }
        }

        // Color Theme Picker
        if (showColorTheme) {
            val themes = listOf("Light 2026" to false,"Light (Visual Studio)" to false,"Light Modern" to false,
                "Light+" to false,"Quiet Light" to false,"Dark (Visual Studio)" to true,
                "Dark Modern" to true,"Dark+" to true,"Monokai" to true,"One Dark Pro" to true)
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)).clickable { showColorTheme = false }, contentAlignment = Alignment.TopCenter) {
                Column(
                    Modifier.fillMaxWidth(0.92f).padding(top = 44.dp)
                        .background(FloatingMenuBg, RoundedCornerShape(8.dp))
                        .border(1.dp, FloatingMenuBorder, RoundedCornerShape(8.dp))
                        .clickable { }
                ) {
                    OutlinedTextField(value = "", onValueChange = {},
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                        placeholder = { Text("Select Color Theme", fontSize = 13.sp) }, singleLine = true)
                    HorizontalDivider(color = DividerColor)
                    Column(Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState())) {
                        themes.forEach { (name, dark) ->
                            val isSelected = dark == isDark
                            Row(
                                Modifier.fillMaxWidth()
                                    .background(if (isSelected && name.contains(if (isDark) "Dark" else "Light")) CmdSelectedBg else Color.Transparent)
                                    .clickable { if (dark != isDark) onToggleTheme(); showColorTheme = false }
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(name, fontSize = 13.sp, color = FloatingMenuText, modifier = Modifier.weight(1f))
                                Text(if (dark) "dark themes" else "light themes", fontSize = 11.sp, color = TabTextInactive)
                            }
                        }
                    }
                }
            }
        }

        // More menu (··· in activity bar)
        if (showMoreMenu) {
            Box(Modifier.fillMaxSize().clickable { showMoreMenu = false }) {
                Card(
                    Modifier.align(Alignment.BottomStart).padding(start = 52.dp, bottom = 108.dp).width(200.dp).clickable { },
                    elevation = CardDefaults.cardElevation(8.dp),
                    colors = CardDefaults.cardColors(containerColor = FloatingMenuBg),
                    shape = RoundedCornerShape(4.dp),
                ) {
                    Column(Modifier.padding(vertical = 4.dp)) {
                        listOf("Remote Explorer", "Extensions").forEach { item ->
                            Row(Modifier.fillMaxWidth().clickable {
                                showMoreMenu = false
                                if (item == "Extensions") activePanel = SidePanel.EXTENSIONS
                            }.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                Text(item, fontSize = 13.sp, color = FloatingMenuText)
                            }
                        }
                    }
                }
            }
        }

        // Person menu
        if (showPersonMenu) {
            Box(Modifier.fillMaxSize().clickable { showPersonMenu = false; showPersonSubMenu = false }) {
                Card(
                    Modifier.align(Alignment.BottomStart).padding(start = 52.dp, bottom = 60.dp).width(220.dp).clickable { },
                    elevation = CardDefaults.cardElevation(8.dp),
                    colors = CardDefaults.cardColors(containerColor = FloatingMenuBg),
                    shape = RoundedCornerShape(4.dp),
                ) {
                    Column(Modifier.padding(vertical = 4.dp)) {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Accounts", fontSize = 13.sp, color = FloatingMenuText, fontWeight = FontWeight.Medium)
                        }
                        HorizontalDivider(color = DividerColor)
                        Row(Modifier.fillMaxWidth().clickable { showPersonSubMenu = !showPersonSubMenu }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Turn on Settings Sync…", fontSize = 13.sp, color = FloatingMenuText)
                            Icon(Icons.Default.ArrowForward, null, tint = TabTextInactive, modifier = Modifier.size(16.dp))
                        }
                        Row(Modifier.fillMaxWidth().clickable { showPersonMenu = false }
                            .padding(horizontal = 12.dp, vertical = 8.dp)) {
                            Text("Sign in with GitHub", fontSize = 13.sp, color = FloatingMenuText)
                        }
                        Row(Modifier.fillMaxWidth().clickable { showPersonMenu = false }
                            .padding(horizontal = 12.dp, vertical = 8.dp)) {
                            Text("Sign in with Microsoft", fontSize = 13.sp, color = FloatingMenuText)
                        }
                    }
                }
            }
        }

        // Gear menu
        if (showGearMenu) {
            Box(Modifier.fillMaxSize().clickable { showGearMenu = false; showGearThemeMenu = false }) {
                Card(
                    Modifier.align(Alignment.BottomStart).padding(start = 52.dp, bottom = 26.dp).width(220.dp).clickable { },
                    elevation = CardDefaults.cardElevation(8.dp),
                    colors = CardDefaults.cardColors(containerColor = FloatingMenuBg),
                    shape = RoundedCornerShape(4.dp),
                ) {
                    Column(Modifier.padding(vertical = 4.dp)) {
                        listOf("Settings", "Extensions", "Keyboard Shortcuts", "User Snippets", "User Tasks").forEach { item ->
                            Row(Modifier.fillMaxWidth().clickable { showGearMenu = false }
                                .padding(horizontal = 12.dp, vertical = 8.dp)) {
                                Text(item, fontSize = 13.sp, color = FloatingMenuText)
                            }
                        }
                        HorizontalDivider(color = DividerColor)
                        Row(Modifier.fillMaxWidth().clickable { showGearThemeMenu = !showGearThemeMenu }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Themes", fontSize = 13.sp, color = FloatingMenuText)
                            Icon(Icons.Default.ArrowForward, null, tint = TabTextInactive, modifier = Modifier.size(16.dp))
                        }
                        if (showGearThemeMenu) {
                            HorizontalDivider(color = DividerColor)
                            Row(Modifier.fillMaxWidth().clickable { showColorTheme = true; showGearMenu = false }
                                .padding(horizontal = 24.dp, vertical = 8.dp)) {
                                Text("Color Theme", fontSize = 13.sp, color = FloatingMenuText)
                            }
                        }
                    }
                }
            }
        }

        // Run/Debug panel menu
        if (showRunMenu) {
            Box(Modifier.fillMaxSize().clickable { showRunMenu = false }) {
                Card(
                    Modifier.align(Alignment.TopStart).padding(start = 52.dp, top = 152.dp).width(200.dp).clickable { },
                    elevation = CardDefaults.cardElevation(8.dp),
                    colors = CardDefaults.cardColors(containerColor = FloatingMenuBg),
                    shape = RoundedCornerShape(4.dp),
                ) {
                    Column(Modifier.padding(vertical = 4.dp)) {
                        listOf("Add Configuration…","Open launch.json").forEach { item ->
                            Row(Modifier.fillMaxWidth().clickable { showRunMenu = false }
                                .padding(horizontal = 12.dp, vertical = 8.dp)) {
                                Text(item, fontSize = 13.sp, color = FloatingMenuText)
                            }
                        }
                    }
                }
            }
        }

        // ── PANEL "..." MENU ─────────────────────────────────────────────────────
        // Rendered at the ROOT Box level so it always appears ABOVE the panel.
        // Context-aware: shows different options depending on the active tab,
        // matching VS Code behaviour.
        if (showPanelMenu) {
            Box(Modifier.fillMaxSize().clickable { showPanelMenu = false }) {
                Card(
                    Modifier.align(Alignment.BottomEnd).padding(end = 8.dp, bottom = 60.dp).width(260.dp).clickable { },
                    elevation = CardDefaults.cardElevation(8.dp),
                    colors = CardDefaults.cardColors(containerColor = FloatingMenuBg),
                    shape = RoundedCornerShape(4.dp),
                ) {
                    Column(Modifier.padding(vertical = 4.dp)) {
                        when (activeBottomTab) {
                            BottomTab.TERMINAL -> {
                                listOf(
                                    "New Terminal",
                                    "Split Terminal",
                                    "Clear Terminal",
                                    "Scroll to Previous Command",
                                    "Scroll to Next Command",
                                    "Run Active File",
                                    "Run Selected Text",
                                    "Start Dictation",
                                ).forEach { item ->
                                    val dimmed = item in listOf("Scroll to Previous Command", "Scroll to Next Command")
                                    Row(Modifier.fillMaxWidth().clickable { showPanelMenu = false }
                                        .padding(horizontal = 12.dp, vertical = 8.dp)) {
                                        Text(item, fontSize = 13.sp, color = if (dimmed) TabTextInactive else FloatingMenuText)
                                    }
                                }
                            }
                            BottomTab.OUTPUT -> {
                                listOf(
                                    "Clear Output",
                                    "Open Output in Editor",
                                    "Toggle Auto Scroll",
                                    "Show Output Channels",
                                ).forEach { item ->
                                    Row(Modifier.fillMaxWidth().clickable { showPanelMenu = false }
                                        .padding(horizontal = 12.dp, vertical = 8.dp)) {
                                        Text(item, fontSize = 13.sp, color = FloatingMenuText)
                                    }
                                }
                            }
                            BottomTab.PROBLEMS -> {
                                listOf(
                                    "Copy All Problems",
                                    "Filter by Type",
                                    "Collapse All",
                                    "Show Errors Only",
                                    "Show Warnings Only",
                                ).forEach { item ->
                                    Row(Modifier.fillMaxWidth().clickable { showPanelMenu = false }
                                        .padding(horizontal = 12.dp, vertical = 8.dp)) {
                                        Text(item, fontSize = 13.sp, color = FloatingMenuText)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Explorer ··· menu
        if (showExplorerMore) {
            Box(Modifier.fillMaxSize().clickable { showExplorerMore = false }) {
                Card(
                    Modifier.align(Alignment.TopStart).padding(start = 200.dp, top = 80.dp).width(200.dp).clickable { },
                    elevation = CardDefaults.cardElevation(8.dp),
                    colors = CardDefaults.cardColors(containerColor = FloatingMenuBg),
                    shape = RoundedCornerShape(4.dp),
                ) {
                    Column(Modifier.padding(vertical = 4.dp)) {
                        listOf("Open Editors","No Folder Opened","Outline","Timeline").forEach { item ->
                            Row(Modifier.fillMaxWidth().clickable { showExplorerMore = false }
                                .padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Check, null, tint = FloatingMenuText, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(item, fontSize = 13.sp, color = FloatingMenuText)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExplorerSidePanel(onOpenFile: (String) -> Unit, onMoreMenu: () -> Unit) {
    var noFolderExpanded by remember { mutableStateOf(true) }
    var openEditorsExpanded by remember { mutableStateOf(false) }
    var outlineExpanded by remember { mutableStateOf(false) }
    var timelineExpanded by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("EXPLORER", fontSize = 11.sp, color = SectionHeaderText, modifier = Modifier.weight(1f))
            Icon(Icons.Default.MoreHoriz, null, tint = TabTextInactive, modifier = Modifier.size(16.dp).clickable { onMoreMenu() })
        }
        HorizontalDivider(color = DividerColor)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            SectionRow("OPEN EDITORS", openEditorsExpanded) { openEditorsExpanded = !openEditorsExpanded }
            SectionRow("NO FOLDER OPENED", noFolderExpanded) { noFolderExpanded = !noFolderExpanded }
            if (noFolderExpanded) {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text("You have not yet opened a folder.", fontSize = 12.sp, color = TabTextInactive)
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = {}, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = BlueBtn)) { Text("Open Folder", fontSize = 12.sp) }
                    Spacer(Modifier.height(4.dp))
                    Button(onClick = {}, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = BlueBtn)) { Text("Open Recent", fontSize = 12.sp) }
                }
            }
            SectionRow("OUTLINE", outlineExpanded) { outlineExpanded = !outlineExpanded }
            SectionRow("TIMELINE", timelineExpanded) { timelineExpanded = !timelineExpanded }
        }
    }
}

@Composable
private fun SectionRow(title: String, expanded: Boolean, onToggle: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { onToggle() }.padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(if (expanded) Icons.Default.ExpandMore else Icons.Default.ArrowForward, null,
            tint = TabTextInactive, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(4.dp))
        Text(title, fontSize = 11.sp, color = SectionHeaderText, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun SearchPanel() {
    Column(Modifier.fillMaxSize().padding(8.dp)) {
        Text("SEARCH", fontSize = 11.sp, color = SectionHeaderText, modifier = Modifier.padding(bottom = 8.dp))
        OutlinedTextField(value = "", onValueChange = {}, modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search", fontSize = 13.sp) }, singleLine = true)
        Spacer(Modifier.height(8.dp))
        Text("No results", fontSize = 12.sp, color = TabTextInactive)
    }
}

@Composable
private fun GitSidePanel() {
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("SOURCE CONTROL", fontSize = 11.sp, color = SectionHeaderText, modifier = Modifier.weight(1f))
            Icon(Icons.Default.MoreHoriz, null, tint = TabTextInactive, modifier = Modifier.size(16.dp))
        }
        HorizontalDivider(color = DividerColor)
        Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.TopStart) {
            Text("No source control providers registered.", fontSize = 12.sp, color = TabTextInactive)
        }
    }
}

@Composable
private fun RunDebugPanel(onMoreMenu: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("RUN AND DEBUG", fontSize = 11.sp, color = SectionHeaderText, modifier = Modifier.weight(1f))
            Icon(Icons.Default.MoreHoriz, null, tint = TabTextInactive, modifier = Modifier.size(16.dp).clickable { onMoreMenu() })
        }
        HorizontalDivider(color = DividerColor)
        Column(Modifier.padding(16.dp)) {
            Text("No launch configuration found.", fontSize = 12.sp, color = TabTextInactive)
            Spacer(Modifier.height(8.dp))
            Button(onClick = {}, colors = ButtonDefaults.buttonColors(containerColor = BlueBtn), modifier = Modifier.fillMaxWidth()) {
                Text("Add Configuration…", fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun ExtensionsPanel() {
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("EXTENSIONS", fontSize = 11.sp, color = SectionHeaderText, modifier = Modifier.weight(1f))
        }
        HorizontalDivider(color = DividerColor)
        Column(Modifier.fillMaxSize().padding(8.dp)) {
            OutlinedTextField(value = "", onValueChange = {}, modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search Extensions…", fontSize = 13.sp) }, singleLine = true)
            Spacer(Modifier.height(8.dp))
            Text("INSTALLED", fontSize = 11.sp, color = SectionHeaderText)
            Spacer(Modifier.height(4.dp))
            Text("No extensions installed.", fontSize = 12.sp, color = TabTextInactive)
        }
    }
}

@Composable
private fun ProblemsPanel() {
    Column(Modifier.fillMaxSize().background(Color(0xFFFFFFFF))) {
        Row(
            Modifier.fillMaxWidth().background(Color(0xFFF3F3F3)).padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            androidx.compose.foundation.text.BasicTextField(
                value = "",
                onValueChange = {},
                modifier = Modifier
                    .weight(1f)
                    .background(Color(0xFFFFFFFF), RoundedCornerShape(3.dp))
                    .border(1.dp, Color(0xFFD0D0D0), RoundedCornerShape(3.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, color = Color(0xFF333333)),
                decorationBox = { inner ->
                    Text("Filter (e.g. text, **/\\*.ts, !**/node_modules/**...)", fontSize = 11.sp, color = Color(0xFFAAAAAA))
                    inner()
                },
                singleLine = true,
            )
        }
        HorizontalDivider(color = Color(0xFFD0D0D0))
        Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.TopStart) {
            Text("No problems have been detected in the workspace.", fontSize = 13.sp, color = Color(0xFF717171))
        }
    }
}

@Composable
private fun OutputPanel() {
    var filterText by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().background(Color(0xFFFFFFFF))) {
        Row(
            Modifier.fillMaxWidth().background(Color(0xFFF3F3F3)).padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            androidx.compose.foundation.text.BasicTextField(
                value = filterText,
                onValueChange = { filterText = it },
                modifier = Modifier
                    .weight(1f)
                    .background(Color(0xFFFFFFFF), RoundedCornerShape(3.dp))
                    .border(1.dp, Color(0xFFD0D0D0), RoundedCornerShape(3.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, color = Color(0xFF333333)),
                decorationBox = { inner ->
                    if (filterText.isEmpty()) Text("Filter (e.g. text, !excludeText, t...)", fontSize = 11.sp, color = Color(0xFFAAAAAA))
                    inner()
                },
                singleLine = true,
            )
            Row(
                Modifier.background(Color(0xFFFFFFFF), RoundedCornerShape(3.dp))
                    .border(1.dp, Color(0xFFD0D0D0), RoundedCornerShape(3.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Tasks", fontSize = 12.sp, color = Color(0xFF333333))
                Spacer(Modifier.width(4.dp))
                Icon(Icons.Default.ArrowDropDown, null, tint = Color(0xFF333333), modifier = Modifier.size(16.dp))
            }
        }
        HorizontalDivider(color = Color(0xFFD0D0D0))
        Box(Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp), contentAlignment = Alignment.TopStart) {
            Text("", fontSize = 12.sp, color = Color(0xFF1E1E1E),
                fontFamily = FontFamily.Monospace)
        }
    }
}

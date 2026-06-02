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
import androidx.compose.foundation.shape.CircleShape
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
    var showBottomPanel by remember { mutableStateOf(true) }
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
    var showTerminalMenu by remember { mutableStateOf(false) }
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
                        SidePanel.EXPLORER to Icons.Default.Description,
                        SidePanel.SEARCH to Icons.Default.Search,
                        SidePanel.GIT to Icons.Default.AccountTree,
                        SidePanel.RUN to (if (activePanel == SidePanel.EXTENSIONS) Icons.Default.Extension else Icons.Default.BugReport),
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
                                    }
                                    Box(Modifier.width(1.dp).height(35.dp).background(DividerColor))
                                }
                            }
                            HorizontalDivider(color = DividerColor)
                        }

                        Box(Modifier.weight(1f).fillMaxWidth().background(BgColor)) {
                            if (activeEditorTab != null) {
                                EditorPane()
                            } else {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text("</>\nCodeSpace IDE", fontSize = 48.sp, color = Color(0xFFE0E0E0),
                                        fontWeight = FontWeight.Light, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                }
                            }
                        }

                        if (showBottomPanel) {
                            Box(
                                Modifier.fillMaxWidth().height(4.dp).background(DividerColor)
                                    .pointerInput(Unit) {
                                        detectDragGestures { _, dragAmount ->
                                            val nh = bottomPanelHeight - dragAmount.y
                                            if (nh < 60f) showBottomPanel = false
                                            else bottomPanelHeight = nh.coerceIn(60f, totalHeight * 0.6f)
                                        }
                                    }
                            )
                            val bh = with(density) { bottomPanelHeight.toDp() }.coerceIn(60.dp, 500.dp)
                            Column(Modifier.fillMaxWidth().height(bh).background(PanelBg)) {
                                Row(
                                    Modifier.fillMaxWidth().background(TabBarBg).height(35.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    // PROBLEMS | OUTPUT | TERMINAL tabs
                                    BottomTab.entries.forEach { tab ->
                                        val isActive = tab == activeBottomTab
                                        Column(
                                            Modifier.clickable { activeBottomTab = tab }.fillMaxHeight(),
                                            verticalArrangement = Arrangement.Bottom,
                                        ) {
                                            Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                                Text(
                                                    tab.name,
                                                    fontSize = 12.sp,
                                                    color = if (isActive) TabText else TabTextInactive,
                                                    fontWeight = if (isActive) FontWeight.Medium else FontWeight.Normal,
                                                )
                                            }
                                            if (isActive) Box(Modifier.fillMaxWidth().height(1.dp).background(TabActiveIndicator))
                                            else Spacer(Modifier.height(1.dp))
                                        }
                                    }
                                    Spacer(Modifier.weight(1f))
                                    // Context icons per tab
                                    when (activeBottomTab) {
                                        BottomTab.PROBLEMS -> {
                                            Icon(Icons.Default.FilterList, null, tint = TabTextInactive, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(6.dp))
                                            Icon(Icons.Default.UnfoldLess, null, tint = TabTextInactive, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(6.dp))
                                        }
                                        BottomTab.OUTPUT -> {
                                            Icon(Icons.Default.FilterList, null, tint = TabTextInactive, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(6.dp))
                                            Icon(Icons.Default.Lock, null, tint = TabTextInactive, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(6.dp))
                                        }
                                        BottomTab.TERMINAL -> { /* no extra icons */ }
                                    }
                                    // Always-visible: ... | expand | close
                                    Icon(Icons.Default.MoreHoriz, null, tint = TabTextInactive,
                                        modifier = Modifier.size(16.dp).clickable { showTerminalMenu = true })
                                    Spacer(Modifier.width(6.dp))
                                    Box(Modifier.width(1.dp).height(16.dp).background(DividerColor))
                                    Spacer(Modifier.width(6.dp))
                                    Icon(Icons.Default.OpenInFull, null, tint = TabTextInactive,
                                        modifier = Modifier.size(16.dp).clickable {
                                            bottomPanelHeight = if (bottomPanelHeight > totalHeight * 0.7f) 300f else totalHeight * 0.85f
                                        })
                                    Spacer(Modifier.width(6.dp))
                                    Icon(Icons.Default.Close, null, tint = TabTextInactive,
                                        modifier = Modifier.size(16.dp).clickable { showBottomPanel = false })
                                    Spacer(Modifier.width(8.dp))
                                }
                                HorizontalDivider(color = DividerColor)
                                Box(Modifier.fillMaxSize()) {
                                    // Terminal always composed to preserve session state
                                    Box(Modifier.fillMaxSize().then(if (activeBottomTab == BottomTab.TERMINAL) Modifier else Modifier.size(0.dp))) {
                                        TerminalPane()
                                    }
                                    if (activeBottomTab == BottomTab.PROBLEMS) {
                                        ProblemsPanel()
                                    }
                                    if (activeBottomTab == BottomTab.OUTPUT) {
                                        OutputPanel()
                                    }
                                }
                            }
                        }
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
                Icon(Icons.Default.SwapHoriz, null, tint = Color(0xFF424242), modifier = Modifier.size(14.dp).clickable { showBottomPanel = !showBottomPanel })
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
            Icon(Icons.Default.ChevronLeft, null, tint = Color(0xFF717171), modifier = Modifier.size(20.dp).clickable { onBack() })
            Icon(Icons.Default.ChevronRight, null, tint = Color(0xFFCCCCCC), modifier = Modifier.size(20.dp))
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
                Icon(Icons.Default.AutoAwesome, null, tint = if (showAiPanel) Color.White else Color(0xFF717171), modifier = Modifier.size(18.dp))
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

        // More menu (···)
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
                    Modifier.align(Alignment.BottomStart).padding(start = 52.dp, bottom = 56.dp).width(280.dp).clickable { },
                    elevation = CardDefaults.cardElevation(8.dp),
                    colors = CardDefaults.cardColors(containerColor = FloatingMenuBg),
                    shape = RoundedCornerShape(4.dp),
                ) {
                    Column(Modifier.padding(vertical = 4.dp)) {
                        Row(
                            Modifier.fillMaxWidth().clickable { showPersonSubMenu = !showPersonSubMenu }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("Add Account", fontSize = 13.sp, color = FloatingMenuText)
                            Icon(Icons.Default.ChevronRight, null, tint = TabTextInactive, modifier = Modifier.size(16.dp))
                        }
                        HorizontalDivider(color = DividerColor)
                        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Check, null, tint = FloatingMenuText, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Settings Sync is Off", fontSize = 13.sp, color = FloatingMenuText)
                        }
                        listOf("Turn on Cloud Changes...", "Manage Extension Account Preferences...").forEach { item ->
                            Row(Modifier.fillMaxWidth().clickable { showPersonMenu = false }.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                Text(item, fontSize = 13.sp, color = FloatingMenuText)
                            }
                        }
                    }
                }
                if (showPersonSubMenu) {
                    Card(
                        Modifier.align(Alignment.BottomStart).padding(start = 284.dp, bottom = 120.dp).width(220.dp).clickable { },
                        elevation = CardDefaults.cardElevation(8.dp),
                        colors = CardDefaults.cardColors(containerColor = FloatingMenuBg),
                        shape = RoundedCornerShape(4.dp),
                    ) {
                        Column(Modifier.padding(vertical = 4.dp)) {
                            listOf("Manage Trusted Extensions", "Manage Trusted MCP Servers", "Sign Out").forEach { item ->
                                Row(Modifier.fillMaxWidth().clickable { showPersonMenu = false; showPersonSubMenu = false }.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                    Text(item, fontSize = 13.sp, color = FloatingMenuText)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Gear menu
        if (showGearMenu) {
            Box(Modifier.fillMaxSize().clickable { showGearMenu = false; showGearThemeMenu = false }) {
                Card(
                    Modifier.align(Alignment.BottomStart).padding(start = 52.dp, bottom = 28.dp).width(280.dp).clickable { },
                    elevation = CardDefaults.cardElevation(8.dp),
                    colors = CardDefaults.cardColors(containerColor = FloatingMenuBg),
                    shape = RoundedCornerShape(4.dp),
                ) {
                    Column(Modifier.padding(vertical = 4.dp)) {
                        Row(Modifier.fillMaxWidth().clickable { showGearMenu = false; showCommandPalette = true }.padding(horizontal = 12.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Command Palette...", fontSize = 13.sp, color = FloatingMenuText)
                            Text("Ctrl+Shift+P", fontSize = 11.sp, color = TabTextInactive)
                        }
                        listOf("Profiles", "Settings", "Keyboard Shortcuts", "Snippets", "Tasks").forEach { item ->
                            Row(Modifier.fillMaxWidth().clickable { showGearMenu = false }.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                Text(item, fontSize = 13.sp, color = FloatingMenuText)
                            }
                        }
                        Row(
                            Modifier.fillMaxWidth().background(if (showGearThemeMenu) Color(0xFFE8E8E8) else Color.Transparent)
                                .clickable { showGearThemeMenu = !showGearThemeMenu }.padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("Themes", fontSize = 13.sp, color = FloatingMenuText)
                            Icon(Icons.Default.ChevronRight, null, tint = TabTextInactive, modifier = Modifier.size(16.dp))
                        }
                        HorizontalDivider(color = DividerColor)
                        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Check, null, tint = FloatingMenuText, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Settings Sync is On", fontSize = 13.sp, color = FloatingMenuText)
                        }
                        Row(Modifier.fillMaxWidth().clickable { showGearMenu = false }.padding(horizontal = 12.dp, vertical = 8.dp)) {
                            Text("Switch to Insiders Version...", fontSize = 13.sp, color = FloatingMenuText)
                        }
                    }
                }
                if (showGearThemeMenu) {
                    Card(
                        Modifier.align(Alignment.BottomStart).padding(start = 284.dp, bottom = 180.dp).width(220.dp).clickable { },
                        elevation = CardDefaults.cardElevation(8.dp),
                        colors = CardDefaults.cardColors(containerColor = FloatingMenuBg),
                        shape = RoundedCornerShape(4.dp),
                    ) {
                        Column(Modifier.padding(vertical = 4.dp)) {
                            Row(Modifier.fillMaxWidth().clickable { showGearMenu = false; showGearThemeMenu = false; showColorTheme = true }.padding(horizontal = 12.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Color Theme", fontSize = 13.sp, color = FloatingMenuText)
                                Text("Ctrl+K Ctrl+T", fontSize = 11.sp, color = TabTextInactive)
                            }
                            listOf("File Icon Theme", "Product Icon Theme").forEach { item ->
                                Row(Modifier.fillMaxWidth().clickable { showGearMenu = false }.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                    Text(item, fontSize = 13.sp, color = FloatingMenuText)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Run/Debug menu
        if (showRunMenu) {
            Box(Modifier.fillMaxSize().clickable { showRunMenu = false }) {
                Card(
                    Modifier.align(Alignment.TopStart).padding(start = 200.dp, top = 100.dp).width(180.dp).clickable { },
                    elevation = CardDefaults.cardElevation(8.dp),
                    colors = CardDefaults.cardColors(containerColor = FloatingMenuBg),
                    shape = RoundedCornerShape(4.dp),
                ) {
                    Column(Modifier.padding(vertical = 4.dp)) {
                        Row(Modifier.fillMaxWidth().clickable { showRunMenu = false }.padding(horizontal = 12.dp, vertical = 8.dp)) {
                            Text("Debug Console", fontSize = 13.sp, color = FloatingMenuText)
                        }
                        Row(Modifier.fillMaxWidth().clickable { showRunMenu = false }.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Check, null, tint = FloatingMenuText, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Run", fontSize = 13.sp, color = TabTextInactive)
                        }
                    }
                }
            }
        }

        // Terminal context menu
        if (showTerminalMenu) {
            Box(Modifier.fillMaxSize().clickable { showTerminalMenu = false }) {
                Card(
                    Modifier.align(Alignment.BottomEnd).padding(end = 8.dp, bottom = 60.dp).width(240.dp).clickable { },
                    elevation = CardDefaults.cardElevation(8.dp),
                    colors = CardDefaults.cardColors(containerColor = FloatingMenuBg),
                    shape = RoundedCornerShape(4.dp),
                ) {
                    Column(Modifier.padding(vertical = 4.dp)) {
                        listOf("Scroll to Previous Command","Scroll to Next Command","Clear Terminal","Run Active File","Run Selected Text","Start Dictation").forEach { item ->
                            Row(Modifier.fillMaxWidth().clickable { showTerminalMenu = false }.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                Text(item, fontSize = 13.sp, color = if (item in listOf("Scroll to Previous Command","Scroll to Next Command")) TabTextInactive else FloatingMenuText)
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
                            Row(Modifier.fillMaxWidth().clickable { showExplorerMore = false }.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
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
private fun SectionRow(title: String, expanded: Boolean, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(if (expanded) Icons.Default.KeyboardArrowDown else Icons.Default.ChevronRight, null, tint = SectionHeaderText, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(4.dp))
        Text(title, fontSize = 11.sp, color = SectionHeaderText, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SearchPanel() {
    Column(Modifier.fillMaxSize().padding(8.dp)) {
        Text("SEARCH", fontSize = 11.sp, color = SectionHeaderText)
        HorizontalDivider(color = DividerColor, modifier = Modifier.padding(vertical = 8.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.ChevronRight, null, tint = TabTextInactive, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            OutlinedTextField(value = "", onValueChange = {}, modifier = Modifier.weight(1f).height(40.dp),
                placeholder = { Text("Search", fontSize = 13.sp) }, singleLine = true)
            Spacer(Modifier.width(4.dp))
            Text("Aa", fontSize = 11.sp, color = TabTextInactive, modifier = Modifier.border(1.dp, DividerColor, RoundedCornerShape(3.dp)).padding(3.dp))
            Spacer(Modifier.width(2.dp))
            Text("ab", fontSize = 11.sp, color = Color(0xFF007ACC), modifier = Modifier.border(1.dp, Color(0xFF007ACC), RoundedCornerShape(3.dp)).background(Color(0xFFDCEDFD)).padding(3.dp))
            Spacer(Modifier.width(2.dp))
            Text(".*", fontSize = 11.sp, color = TabTextInactive, modifier = Modifier.border(1.dp, DividerColor, RoundedCornerShape(3.dp)).padding(3.dp))
        }
    }
}

@Composable
private fun GitSidePanel() {
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(8.dp)) {
            Text("SOURCE CONTROL", fontSize = 11.sp, color = SectionHeaderText)
            HorizontalDivider(color = DividerColor, modifier = Modifier.padding(vertical = 8.dp))
            Text("You can open a remote repository or pull request without cloning.", fontSize = 12.sp, color = TabTextInactive)
            Spacer(Modifier.height(12.dp))
            Button(onClick = {}, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = BlueBtn)) {
                Text("Open Remote Repository", fontSize = 12.sp)
            }
        }
        HorizontalDivider(color = DividerColor)
        Box(Modifier.fillMaxSize()) { SourceControlPane() }
    }
}

@Composable
private fun RunDebugPanel(onMoreMenu: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(8.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("RUN AND DEBUG: RUN", fontSize = 11.sp, color = SectionHeaderText, modifier = Modifier.weight(1f))
            Icon(Icons.Default.MoreHoriz, null, tint = TabTextInactive, modifier = Modifier.size(16.dp).clickable { onMoreMenu() })
        }
        HorizontalDivider(color = DividerColor, modifier = Modifier.padding(vertical = 8.dp))
        Text("All debug extensions are disabled. Enable a debug extension or install a new one from the Marketplace.", fontSize = 12.sp, color = TabTextInactive)
        Spacer(Modifier.height(8.dp))
        Text("Run and Debug are not available in the web editor. Continue in an environment that can run code, like a codespace or VS Code Desktop.", fontSize = 12.sp, color = TabTextInactive)
    }
}

@Composable
private fun ExtensionsPanel() {
    var query by remember { mutableStateOf("") }
    var installedExp by remember { mutableStateOf(false) }
    var popularExp by remember { mutableStateOf(true) }
    var recommendedExp by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("EXTENSIONS", fontSize = 11.sp, color = SectionHeaderText, modifier = Modifier.weight(1f))
            Icon(Icons.Default.Refresh, null, tint = TabTextInactive, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Icon(Icons.Default.MoreHoriz, null, tint = TabTextInactive, modifier = Modifier.size(16.dp))
        }
        OutlinedTextField(value = query, onValueChange = { query = it }, modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp).height(36.dp),
            placeholder = { Text("Search Extensions in Marketplace", fontSize = 12.sp) }, singleLine = true)
        HorizontalDivider(color = DividerColor, modifier = Modifier.padding(vertical = 4.dp))
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            ExtSection("INSTALLED", installedExp, 0) { installedExp = !installedExp }
            ExtSection("POPULAR", popularExp, -1) { popularExp = !popularExp }
            if (popularExp) {
                listOf("Python" to "Microsoft","Pylance" to "Microsoft","Prettier" to "Prettier","GitLens" to "GitKraken","Docker" to "Microsoft").forEach { (n, a) ->
                    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(36.dp).background(BlueBtn, RoundedCornerShape(4.dp)), contentAlignment = Alignment.Center) {
                            Text(n.first().toString(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text(n, fontSize = 13.sp, color = TabText, fontWeight = FontWeight.Medium)
                            Text(a, fontSize = 11.sp, color = TabTextInactive)
                        }
                        Button(onClick = {}, modifier = Modifier.height(24.dp), contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BlueBtn)) { Text("Install", fontSize = 11.sp) }
                    }
                }
            }
            ExtSection("RECOMMENDED", recommendedExp, 5) { recommendedExp = !recommendedExp }
        }
    }
}

@Composable
private fun ExtSection(title: String, expanded: Boolean, count: Int, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(if (expanded) Icons.Default.KeyboardArrowDown else Icons.Default.ChevronRight, null, tint = SectionHeaderText, modifier = Modifier.size(16.dp))
        Text(title, fontSize = 11.sp, color = SectionHeaderText, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f).padding(start = 4.dp))
        if (count >= 0) {
            Box(Modifier.size(18.dp).clip(CircleShape).background(Color(0xFF007ACC)), contentAlignment = Alignment.Center) {
                Text("$count", fontSize = 10.sp, color = Color.White)
            }
        }
    }
}

@Composable
private fun ProblemsPanel() {
    Column(Modifier.fillMaxSize().background(Color(0xFFFFFFFF))) {
        // Filter bar
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
    val outputLog = remember { mutableStateListOf<String>() }

    Column(Modifier.fillMaxSize().background(Color(0xFFFFFFFF))) {
        // Filter + Tasks bar
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
                    if (filterText.isEmpty()) {
                        Text("Filter (e.g. text, !excludeText, t...)", fontSize = 11.sp, color = Color(0xFFAAAAAA))
                    }
                    inner()
                },
                singleLine = true,
            )
            Row(
                Modifier
                    .background(Color(0xFFFFFFFF), RoundedCornerShape(3.dp))
                    .border(1.dp, Color(0xFFD0D0D0), RoundedCornerShape(3.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text("Tasks", fontSize = 12.sp, color = Color(0xFF333333))
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = Color(0xFF717171), modifier = Modifier.size(14.dp))
            }
        }
        HorizontalDivider(color = Color(0xFFD0D0D0))
        LazyColumn(Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp)) {
            if (outputLog.isEmpty()) {
                item { Text("", fontSize = 13.sp, color = Color(0xFF717171)) }
            } else {
                items(outputLog) { line ->
                    Text(line, fontSize = 13.sp, fontFamily = FontFamily.Monospace, color = Color(0xFF333333),
                        modifier = Modifier.padding(vertical = 1.dp))
                }
            }
        }
    }
}

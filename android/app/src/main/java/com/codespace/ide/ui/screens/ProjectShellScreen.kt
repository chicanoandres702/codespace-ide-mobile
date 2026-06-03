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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codespace.ide.data.SecureTokenStore
import com.codespace.ide.ui.panes.*

// ── Colour tokens ────────────────────────────────────────────────────────────
private val BgColor               = Color(0xFFFFFFFF)
private val SidePanelBg           = Color(0xFFFFFFFF)
private val ActivityBarBg         = Color(0xFFFFFFFF)
private val ActivityBarIcon       = Color(0xFF616161)
private val ActivityBarIconActive = Color(0xFF007ACC)
private val TabBarBg              = Color(0xFFECECEC)
private val TabActiveBg           = Color(0xFFFFFFFF)
private val TabInactiveBg         = Color(0xFFECECEC)
private val TabActiveIndicator    = Color(0xFF007ACC)
private val TabText               = Color(0xFF333333)
private val TabTextInactive       = Color(0xFF717171)
private val DividerColor          = Color(0xFFE0E0E0)
private val StatusBarBg           = Color(0xFFFFFFFF)
private val PanelBg               = Color(0xFFFFFFFF)
private val SectionHeaderText     = Color(0xFF717171)
private val FloatingMenuBg        = Color(0xFFFFFFFF)
private val FloatingMenuBorder    = Color(0xFFD4D4D4)
private val FloatingMenuText      = Color(0xFF333333)
private val CmdSelectedBg         = Color(0xFF0060C0)
private val CmdSelectedText       = Color(0xFFFFFFFF)
private val BlueBtn               = Color(0xFF0060C0)
private val KeyboardToolbarBg     = Color(0xFFF0F0F0)

private enum class SidePanel { EXPLORER, SEARCH, GIT, RUN, EXTENSIONS }
private enum class BottomTab  { PROBLEMS, OUTPUT, TERMINAL }

private val SPECIAL_KEYS = listOf(
    "{", "}", "[", "]", "(", ")", "<", ">",
    "=", "+", "-", "*", "/", ":", ";", "'",
    "\"", "|", "&", "!", "?", "@", "#", "\$",
    "%", "^", "~", "\\", ",", ".", "_",
    "Tab", "Esc",
)

@Composable
fun ProjectShellScreen(
    projectId: String,
    isDark: Boolean,
    onToggleTheme: () -> Unit,
    onBack: () -> Unit,
    tokenStore: SecureTokenStore,
) {
    val density = LocalDensity.current

    var activePanel       by remember { mutableStateOf<SidePanel?>(null) }
    var showBottomPanel   by remember { mutableStateOf(true) }
    var showAiPanel       by remember { mutableStateOf(false) }
    var activeBottomTab   by remember { mutableStateOf(BottomTab.TERMINAL) }
    var totalWidth        by remember { mutableFloatStateOf(1080f) }
    var totalHeight       by remember { mutableFloatStateOf(1920f) }
    var sidePanelWidth    by remember { mutableFloatStateOf(280f) }
    var bottomPanelHeight by remember { mutableFloatStateOf(300f) }
    var aiPanelWidth      by remember { mutableFloatStateOf(300f) }
    var showCommandPalette  by remember { mutableStateOf(false) }
    var showMoreMenu        by remember { mutableStateOf(false) }
    var showPersonMenu      by remember { mutableStateOf(false) }
    var showGearMenu        by remember { mutableStateOf(false) }
    var showColorTheme      by remember { mutableStateOf(false) }
    var showRunMenu         by remember { mutableStateOf(false) }
    var showPanelMenu       by remember { mutableStateOf(false) }
    var showExplorerMore    by remember { mutableStateOf(false) }
    var commandQuery        by remember { mutableStateOf("") }
    var commandTab          by remember { mutableStateOf("Commands") }
    val editorTabs          = remember { mutableStateListOf<String>() }
    var activeEditorTab     by remember { mutableStateOf<String?>(null) }
    var keyboardInsert      by remember { mutableStateOf<((String) -> Unit)?>(null) }

    Box(
        Modifier.fillMaxSize().background(BgColor)
            .onGloballyPositioned {
                totalWidth  = it.size.width.toFloat()
                totalHeight = it.size.height.toFloat()
            }
    ) {
        Column(Modifier.fillMaxSize().padding(top = 36.dp)) {

            Row(Modifier.weight(1f).fillMaxWidth()) {

                // ── Activity Bar ──────────────────────────────────────────
                Column(
                    Modifier
                        .width(48.dp).fillMaxHeight().background(ActivityBarBg)
                        .border(width = 1.dp, color = DividerColor,
                            shape = RoundedCornerShape(0.dp))
                        .padding(end = 1.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    listOf(
                        SidePanel.EXPLORER   to Icons.Default.Folder,
                        SidePanel.SEARCH     to Icons.Default.Search,
                        SidePanel.GIT        to Icons.Default.AccountTree,
                        SidePanel.RUN        to Icons.Default.BugReport,
                        SidePanel.EXTENSIONS to Icons.Default.Extension,
                    ).forEach { (panel, icon) ->
                        val isActive = activePanel == panel
                        Box(
                            Modifier.fillMaxWidth().height(48.dp)
                                .clickable { activePanel = if (activePanel == panel) null else panel },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (isActive) Box(
                                Modifier.width(2.dp).height(24.dp)
                                    .align(Alignment.CenterStart)
                                    .background(Color(0xFF007ACC))
                            )
                            Icon(icon, null,
                                tint     = if (isActive) ActivityBarIconActive else ActivityBarIcon,
                                modifier = Modifier.size(24.dp))
                        }
                    }
                    Box(
                        Modifier.fillMaxWidth().height(48.dp).clickable { showMoreMenu = true },
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Default.MoreHoriz, null, tint = ActivityBarIcon, modifier = Modifier.size(24.dp)) }
                    Spacer(Modifier.weight(1f))
                    Box(
                        Modifier.fillMaxWidth().height(48.dp).clickable { showAiPanel = !showAiPanel },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Default.AutoAwesome, null,
                            tint     = if (showAiPanel) ActivityBarIconActive else ActivityBarIcon,
                            modifier = Modifier.size(24.dp))
                    }
                    Box(
                        Modifier.fillMaxWidth().height(48.dp).clickable { showPersonMenu = true },
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Default.Person, null, tint = ActivityBarIcon, modifier = Modifier.size(24.dp)) }
                    Box(
                        Modifier.fillMaxWidth().height(48.dp).clickable { showGearMenu = true },
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Default.Settings, null, tint = ActivityBarIcon, modifier = Modifier.size(24.dp)) }
                    Spacer(Modifier.height(4.dp))
                }

                // ── Side Panel ────────────────────────────────────────────
                if (activePanel != null) {
                    val spWidth = with(density) { sidePanelWidth.toDp() }.coerceIn(150.dp, 600.dp)
                    Column(Modifier.width(spWidth).fillMaxHeight().background(SidePanelBg)) {
                        when (activePanel) {
                            SidePanel.EXPLORER   -> ExplorerSidePanel(
                                onOpenFile = { path ->
                                    if (!editorTabs.contains(path)) editorTabs.add(path)
                                    activeEditorTab = path
                                },
                                onMoreMenu = { showExplorerMore = true },
                            )
                            SidePanel.SEARCH     -> SearchPanel()
                            SidePanel.GIT        -> GitSidePanel()
                            SidePanel.RUN        -> RunDebugPanel(onMoreMenu = { showRunMenu = true })
                            SidePanel.EXTENSIONS -> ExtensionsPanel()
                            else                 -> {}
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

                // ── Editor column ─────────────────────────────────────────
                Column(Modifier.weight(1f).fillMaxHeight()) {

                    // Editor tab bar
                    if (editorTabs.isNotEmpty()) {
                        Row(
                            Modifier.fillMaxWidth().height(35.dp).background(TabBarBg)
                                .horizontalScroll(rememberScrollState()),
                            verticalAlignment = Alignment.Bottom,
                        ) {
                            editorTabs.forEach { tab ->
                                val isActive = tab == activeEditorTab
                                Column(
                                    Modifier.clickable { activeEditorTab = tab }
                                        .background(if (isActive) TabActiveBg else TabInactiveBg)
                                ) {
                                    Row(
                                        Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            tab.substringAfterLast("/"),
                                            fontSize = 13.sp,
                                            color    = if (isActive) TabText else TabTextInactive,
                                            maxLines = 1,
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Icon(Icons.Default.Close, null, tint = TabTextInactive,
                                            modifier = Modifier.size(14.dp).clickable {
                                                editorTabs.remove(tab)
                                                if (activeEditorTab == tab)
                                                    activeEditorTab = editorTabs.lastOrNull()
                                            })
                                    }
                                    if (isActive) Box(Modifier.fillMaxWidth().height(1.dp).background(TabActiveIndicator))
                                    else Spacer(Modifier.height(1.dp))
                                }
                                Box(Modifier.width(1.dp).height(35.dp).background(DividerColor))
                            }
                        }
                    }

                    // Breadcrumb bar
                    if (activeEditorTab != null) {
                        Row(
                            Modifier.fillMaxWidth().height(22.dp).background(BgColor)
                                .padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            val parts = activeEditorTab!!.removePrefix("/storage/emulated/0/").split("/")
                            parts.forEachIndexed { idx, part ->
                                Text(
                                    part,
                                    fontSize = 12.sp,
                                    color    = if (idx == parts.lastIndex) TabText else TabTextInactive,
                                    maxLines = 1,
                                )
                                if (idx < parts.lastIndex) {
                                    Icon(Icons.Default.ChevronRight, null,
                                        tint = TabTextInactive, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                        HorizontalDivider(color = DividerColor)
                    }

                    // Editor
                    Box(Modifier.weight(1f).fillMaxWidth()) {
                        if (activeEditorTab != null) {
                            EditorPane(
                                openFilePath    = activeEditorTab,
                                onInsertRequest = { fn -> keyboardInsert = fn },
                            )
                        } else {
                            Box(Modifier.fillMaxSize().background(BgColor), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("CodeSpace IDE", fontSize = 32.sp, color = Color(0xFFDDDDDD),
                                        fontWeight = FontWeight.Light, textAlign = TextAlign.Center)
                                    Spacer(Modifier.height(8.dp))
                                    Text("Open a file from the Explorer →",
                                        fontSize = 14.sp, color = Color(0xFFCCCCCC))
                                }
                            }
                        }
                    }

                    // ── Coding toolbar above keyboard ─────────────────────
                    if (activeEditorTab != null) {
                        Row(
                            Modifier.fillMaxWidth().height(40.dp).background(KeyboardToolbarBg)
                                .horizontalScroll(rememberScrollState()),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Spacer(Modifier.width(4.dp))
                            SPECIAL_KEYS.forEach { key ->
                                Box(
                                    Modifier
                                        .height(32.dp).defaultMinSize(minWidth = 36.dp)
                                        .background(Color.White, RoundedCornerShape(4.dp))
                                        .border(1.dp, DividerColor, RoundedCornerShape(4.dp))
                                        .clickable { keyboardInsert?.invoke(key) }
                                        .padding(horizontal = 8.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(key, fontSize = 13.sp, color = TabText,
                                        fontFamily = FontFamily.Monospace)
                                }
                                Spacer(Modifier.width(4.dp))
                            }
                        }
                        HorizontalDivider(color = DividerColor)
                    }

                    // ── Bottom Panel ──────────────────────────────────────
                    if (showBottomPanel) {
                        // Drag handle
                        Box(
                            Modifier.fillMaxWidth().height(4.dp).background(DividerColor)
                                .pointerInput(Unit) {
                                    detectDragGestures { _, dragAmount ->
                                        val nh = bottomPanelHeight - dragAmount.y
                                        if (nh < 60f) showBottomPanel = false
                                        else bottomPanelHeight = nh.coerceIn(60f, totalHeight * 0.75f)
                                    }
                                }
                        )

                        // ── Tab bar — ALWAYS shows all 3 tabs ────────────
                        Row(
                            Modifier.fillMaxWidth().background(Color(0xFFF3F3F3))
                                .height(36.dp).padding(horizontal = 8.dp),
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
                                        fontSize   = 12.sp,
                                        color      = if (isActive) Color(0xFF007ACC) else Color(0xFF717171),
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
                                    bottomPanelHeight =
                                        if (bottomPanelHeight > totalHeight * 0.5f) 260f
                                        else totalHeight * 0.75f
                                })
                            Spacer(Modifier.width(8.dp))
                            Icon(Icons.Default.Close, null, tint = TabTextInactive,
                                modifier = Modifier.size(16.dp).clickable { showBottomPanel = false })
                            Spacer(Modifier.width(4.dp))
                        }

                        HorizontalDivider(color = DividerColor)

                        // ── Panel content — all 3 always in composition ───
                        val bh = with(density) { bottomPanelHeight.toDp() }.coerceIn(60.dp, 600.dp)
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
                    } // end showBottomPanel

                } // end editor Column

                // ── AI Panel ─────────────────────────────────────────────
                if (showAiPanel) {
                    val aw = with(density) { aiPanelWidth.toDp() }.coerceIn(200.dp, 500.dp)
                    Box(
                        Modifier.width(4.dp).fillMaxHeight().background(DividerColor)
                            .pointerInput(Unit) {
                                detectDragGestures { _, dragAmount ->
                                    aiPanelWidth =
                                        (aiPanelWidth - dragAmount.x).coerceIn(150f, totalWidth * 0.5f)
                                }
                            }
                    )
                    Column(Modifier.width(aw).fillMaxHeight().background(BgColor)) {
                        Row(
                            Modifier.fillMaxWidth().background(TabBarBg)
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("AI CHAT", fontSize = 11.sp, color = SectionHeaderText,
                                modifier = Modifier.weight(1f))
                            Icon(Icons.Default.Close, null, tint = TabTextInactive,
                                modifier = Modifier.size(16.dp).clickable { showAiPanel = false })
                        }
                        HorizontalDivider(color = DividerColor)
                        AiAssistantPane(tokenStore = tokenStore)
                    }
                }

            } // end main Row

            // ── Status Bar ────────────────────────────────────────────────
            HorizontalDivider(color = DividerColor)
            Row(
                Modifier.fillMaxWidth().height(22.dp).background(StatusBarBg).padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Default.CompareArrows, null, tint = Color(0xFF424242),
                    modifier = Modifier.size(14.dp).clickable { showBottomPanel = !showBottomPanel })
                Spacer(Modifier.width(4.dp))
                Icon(Icons.Default.Close, null, tint = Color(0xFF424242), modifier = Modifier.size(12.dp))
                Text(" 0", color = Color(0xFF424242), fontSize = 11.sp)
                Spacer(Modifier.width(4.dp))
                Icon(Icons.Default.Warning, null, tint = Color(0xFF424242), modifier = Modifier.size(12.dp))
                Text(" 0", color = Color(0xFF424242), fontSize = 11.sp)
                Spacer(Modifier.weight(1f))
                Text("⎇  main", color = Color(0xFF424242), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                Spacer(Modifier.width(12.dp))
                Text(activeEditorTab?.substringAfterLast(".")?.uppercase() ?: "Plain Text",
                    color = Color(0xFF424242), fontSize = 11.sp)
                Spacer(Modifier.width(8.dp))
                Text("UTF-8", color = Color(0xFF424242), fontSize = 11.sp)
                Spacer(Modifier.width(8.dp))
                Icon(Icons.Default.Notifications, null, tint = Color(0xFF424242), modifier = Modifier.size(14.dp))
            }

        } // end outer Column

        // ── Top Bar ───────────────────────────────────────────────────────
        Row(
            Modifier.fillMaxWidth().height(36.dp).background(Color(0xFFF8F8F8))
                .border(width = 1.dp, color = DividerColor, shape = RoundedCornerShape(0.dp))
                .align(Alignment.TopCenter),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(Modifier.width(8.dp))
            Icon(Icons.Default.ArrowBack, null, tint = TabTextInactive,
                modifier = Modifier.size(20.dp).clickable { onBack() })
            Spacer(Modifier.width(8.dp))
            Text(projectId.ifBlank { "Workspace" }, fontSize = 13.sp, color = TabText,
                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            Icon(Icons.Default.Search, null, tint = TabTextInactive,
                modifier = Modifier.size(20.dp).clickable { showCommandPalette = true })
            Spacer(Modifier.width(8.dp))
            Icon(Icons.Default.Terminal, null, tint = TabTextInactive,
                modifier = Modifier.size(20.dp).clickable {
                    showBottomPanel = true; activeBottomTab = BottomTab.TERMINAL })
            Spacer(Modifier.width(8.dp))
            Icon(Icons.Default.AutoAwesome, null,
                tint     = if (showAiPanel) ActivityBarIconActive else TabTextInactive,
                modifier = Modifier.size(20.dp).clickable { showAiPanel = !showAiPanel })
            Spacer(Modifier.width(8.dp))
        }

        // ── Overlays ──────────────────────────────────────────────────────

        if (showCommandPalette) {
            Box(
                Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f))
                    .clickable { showCommandPalette = false; commandQuery = "" },
                contentAlignment = Alignment.TopCenter,
            ) {
                Column(
                    Modifier.padding(top = 60.dp).fillMaxWidth(0.95f)
                        .background(FloatingMenuBg, RoundedCornerShape(8.dp))
                        .border(1.dp, FloatingMenuBorder, RoundedCornerShape(8.dp))
                        .clickable(enabled = false) {}
                ) {
                    Row(Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                        listOf("Commands", "Files", "Settings").forEach { tab ->
                            val isA = commandTab == tab
                            Box(
                                Modifier.clickable { commandTab = tab }
                                    .background(if (isA) BlueBtn else Color.Transparent, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                            ) { Text(tab, fontSize = 13.sp, color = if (isA) Color.White else TabTextInactive) }
                        }
                    }
                    OutlinedTextField(
                        value = commandQuery, onValueChange = { commandQuery = it },
                        placeholder = { Text("> Search commands…", fontSize = 14.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                    HorizontalDivider(color = DividerColor)
                    val commands = listOf(
                        "Open File", "Open Folder", "New Terminal", "Toggle Sidebar",
                        "Change Color Theme", "Format Document", "Git: Commit",
                        "Git: Push", "Git: Pull", "Toggle AI Panel",
                    ).filter { commandQuery.isBlank() || it.contains(commandQuery, ignoreCase = true) }
                    commands.take(8).forEachIndexed { idx, cmd ->
                        Row(
                            Modifier.fillMaxWidth()
                                .background(if (idx == 0) CmdSelectedBg else Color.Transparent)
                                .clickable {
                                    when (cmd) {
                                        "New Terminal"   -> { showBottomPanel = true; activeBottomTab = BottomTab.TERMINAL }
                                        "Toggle Sidebar" -> { activePanel = if (activePanel == null) SidePanel.EXPLORER else null }
                                        "Toggle AI Panel"-> showAiPanel = !showAiPanel
                                        "Change Color Theme" -> showColorTheme = true
                                    }
                                    showCommandPalette = false; commandQuery = ""
                                }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                        ) {
                            Text(cmd, fontSize = 13.sp,
                                color    = if (idx == 0) CmdSelectedText else FloatingMenuText,
                                modifier = Modifier.weight(1f))
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                }
            }
        }

        if (showColorTheme) {
            Box(
                Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f))
                    .clickable { showColorTheme = false },
                contentAlignment = Alignment.TopCenter,
            ) {
                Column(
                    Modifier.padding(top = 60.dp).fillMaxWidth(0.9f)
                        .background(FloatingMenuBg, RoundedCornerShape(8.dp))
                        .border(1.dp, FloatingMenuBorder, RoundedCornerShape(8.dp))
                        .clickable(enabled = false) {}
                ) {
                    Text("Color Theme", style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(16.dp))
                    HorizontalDivider(color = DividerColor)
                    listOf(
                        "Light (Default)" to false,
                        "Dark (Default)"  to true,
                        "GitHub Light"    to false,
                        "Dracula"         to true,
                        "AMOLED Black"    to true,
                    ).forEach { (name, dark) ->
                        Row(
                            Modifier.fillMaxWidth()
                                .clickable { if (dark != isDark) onToggleTheme(); showColorTheme = false }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                        ) {
                            Text(name, fontSize = 13.sp, color = FloatingMenuText,
                                modifier = Modifier.weight(1f))
                            Text(if (dark) "dark" else "light", fontSize = 11.sp, color = TabTextInactive)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }

        if (showMoreMenu) {
            Box(Modifier.fillMaxSize().clickable { showMoreMenu = false }) {
                Card(
                    Modifier.align(Alignment.TopStart).padding(top = 36.dp, start = 48.dp).width(200.dp),
                    colors = CardDefaults.cardColors(containerColor = FloatingMenuBg),
                    elevation = CardDefaults.cardElevation(8.dp),
                ) {
                    listOf("Run & Debug", "Extensions", "Remote Explorer", "Timeline").forEach { item ->
                        Row(
                            Modifier.fillMaxWidth().clickable {
                                when (item) {
                                    "Run & Debug" -> activePanel = SidePanel.RUN
                                    "Extensions"  -> activePanel = SidePanel.EXTENSIONS
                                }
                                showMoreMenu = false
                            }.padding(16.dp),
                        ) { Text(item, fontSize = 13.sp, color = FloatingMenuText) }
                    }
                }
            }
        }

        if (showPersonMenu) {
            Box(Modifier.fillMaxSize().clickable { showPersonMenu = false }) {
                Card(
                    Modifier.align(Alignment.BottomStart).padding(bottom = 96.dp, start = 4.dp).width(220.dp),
                    colors = CardDefaults.cardColors(containerColor = FloatingMenuBg),
                    elevation = CardDefaults.cardElevation(8.dp),
                ) {
                    listOf("Sign in with GitHub", "Sign in with Microsoft", "Manage Accounts").forEach { item ->
                        Row(Modifier.fillMaxWidth().clickable { showPersonMenu = false }.padding(16.dp)) {
                            Text(item, fontSize = 13.sp, color = FloatingMenuText)
                        }
                    }
                }
            }
        }

        if (showGearMenu) {
            Box(Modifier.fillMaxSize().clickable { showGearMenu = false }) {
                Card(
                    Modifier.align(Alignment.BottomStart).padding(bottom = 48.dp, start = 4.dp).width(220.dp),
                    colors = CardDefaults.cardColors(containerColor = FloatingMenuBg),
                    elevation = CardDefaults.cardElevation(8.dp),
                ) {
                    listOf("Settings", "Color Theme", "Keyboard Shortcuts", "Extensions").forEach { item ->
                        Row(Modifier.fillMaxWidth().clickable {
                            when (item) {
                                "Color Theme" -> { showColorTheme = true; showGearMenu = false }
                                else          -> showGearMenu = false
                            }
                        }.padding(16.dp)) { Text(item, fontSize = 13.sp, color = FloatingMenuText) }
                    }
                }
            }
        }

        if (showRunMenu) {
            Box(Modifier.fillMaxSize().clickable { showRunMenu = false }) {
                Card(
                    Modifier.align(Alignment.TopStart).padding(top = 36.dp, start = 48.dp).width(200.dp),
                    colors = CardDefaults.cardColors(containerColor = FloatingMenuBg),
                    elevation = CardDefaults.cardElevation(8.dp),
                ) {
                    listOf("Run Program", "Start Debugging", "Stop", "Restart").forEach { item ->
                        Row(Modifier.fillMaxWidth().clickable { showRunMenu = false }.padding(16.dp)) {
                            Text(item, fontSize = 13.sp, color = FloatingMenuText)
                        }
                    }
                }
            }
        }

        if (showPanelMenu) {
            Box(Modifier.fillMaxSize().clickable { showPanelMenu = false }) {
                Card(
                    Modifier.align(Alignment.BottomEnd).padding(bottom = 80.dp, end = 8.dp).width(200.dp),
                    colors = CardDefaults.cardColors(containerColor = FloatingMenuBg),
                    elevation = CardDefaults.cardElevation(8.dp),
                ) {
                    val items = when (activeBottomTab) {
                        BottomTab.TERMINAL -> listOf("New Terminal", "Split Terminal", "Kill Terminal", "Clear")
                        BottomTab.OUTPUT   -> listOf("Clear Output", "Copy All")
                        BottomTab.PROBLEMS -> listOf("Filter", "Collapse All", "Show Errors", "Show Warnings")
                    }
                    items.forEach { item ->
                        Row(Modifier.fillMaxWidth().clickable {
                            when (item) {
                                "New Terminal" -> { showBottomPanel = true; activeBottomTab = BottomTab.TERMINAL }
                            }
                            showPanelMenu = false
                        }.padding(16.dp)) { Text(item, fontSize = 13.sp, color = FloatingMenuText) }
                    }
                }
            }
        }

        if (showExplorerMore) {
            Box(Modifier.fillMaxSize().clickable { showExplorerMore = false }) {
                Card(
                    Modifier.align(Alignment.TopStart).padding(top = 36.dp, start = 48.dp).width(200.dp),
                    colors = CardDefaults.cardColors(containerColor = FloatingMenuBg),
                    elevation = CardDefaults.cardElevation(8.dp),
                ) {
                    listOf("New File", "New Folder", "Refresh", "Collapse All", "Open in Terminal").forEach { item ->
                        Row(Modifier.fillMaxWidth().clickable { showExplorerMore = false }.padding(16.dp)) {
                            Text(item, fontSize = 13.sp, color = FloatingMenuText)
                        }
                    }
                }
            }
        }

    } // end root Box
}

@Composable
private fun ProblemsPanel() {
    Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.TopStart) {
        Text("✓  No problems detected in the workspace.", fontSize = 13.sp, color = Color(0xFF717171))
    }
}

@Composable
private fun OutputPanel() {
    val logs = listOf(
        "[info]  Gradle build started",
        "[info]  Compiling Kotlin sources…",
        "[info]  Build finished successfully",
        "[info]  APK: app-prod-arm64-v8a-debug.apk",
    )
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().background(Color(0xFFF5F5F5)).padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Tasks", fontSize = 12.sp, color = Color(0xFF424242), modifier = Modifier.weight(1f))
            Icon(Icons.Default.Delete, null, tint = Color(0xFF717171), modifier = Modifier.size(16.dp))
        }
        HorizontalDivider(color = Color(0xFFE0E0E0))
        LazyColumn(Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp)) {
            items(logs) { log ->
                Text(log, fontSize = 12.sp, color = Color(0xFF424242),
                    fontFamily = FontFamily.Monospace, modifier = Modifier.padding(vertical = 2.dp))
            }
        }
    }
}

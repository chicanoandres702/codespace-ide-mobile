package com.codespace.ide.ui.panes

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.WbCloudy
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codespace.ide.terminal.TerminalSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

private const val TERMINAL_PREFS = "terminal_history"
private const val TERMUX_PREFIX  = "/data/data/com.termux/files/usr"
private const val TERMUX_HOME    = "/data/data/com.termux/files/home"
private const val TERMUX_BASH    = "/data/data/com.termux/files/usr/bin/bash"

enum class TerminalMode { LOCAL, CODESPACE }

// One live session per tab
private class LiveSession(val id: String, val name: String, mode: TerminalMode) {
    val lines   = mutableStateListOf<String>()
    val history = mutableListOf<String>()
    var workingDir = TERMUX_HOME
    var mode = mode
    var connected = false

    // Underlying shell
    private var session: TerminalSession? = null

    fun start(scope: kotlinx.coroutines.CoroutineScope) {
        if (connected) return
        val shell = if (File(TERMUX_BASH).exists()) TERMUX_BASH else "/system/bin/sh"
        val ts = TerminalSession(workingDir = workingDir, shell = shell)
        session = ts
        connected = true
        lines.add("🖥  Local • Termux bash")
        lines.add("📁 $workingDir")
        scope.launch(Dispatchers.IO) {
            ts.start().collect { chunk ->
                // Split chunk into lines and append
                chunk.split("\n").forEach { line ->
                    val cleaned = line
                        .replace(Regex("\u001B\\[[0-9;]*[mK]"), "") // strip ANSI colors
                        .replace(Regex("\u001B\\[[0-9;]*[A-Z]"), "")
                        .trimEnd()
                    if (cleaned.isNotEmpty()) lines.add(cleaned)
                }
            }
            connected = false
        }
    }

    fun send(input: String) {
        session?.send(input)
    }

    fun sendLine(cmd: String) {
        if (cmd.isNotBlank()) history.add(0, cmd)
        if (history.size > 200) history.removeLastOrNull()
        session?.send("$cmd\n")
    }

    fun stop() {
        session?.stop()
        connected = false
    }

    fun clear() = lines.clear()
}

// Global session store (survives recomposition)
private val liveSessions = mutableMapOf<String, LiveSession>()

@Composable
fun TerminalPane() {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current

    // Tab list
    val tabs = remember {
        mutableStateListOf<LiveSession>().also { list ->
            if (list.isEmpty()) {
                val s = LiveSession("1", "bash", TerminalMode.LOCAL)
                liveSessions["1"] = s
                list.add(s)
            }
        }
    }
    var activeId by remember { mutableStateOf(tabs.first().id) }
    var input    by remember { mutableStateOf("") }
    var histIdx  by remember { mutableStateOf(-1) }
    val listState = rememberLazyListState()
    val focusRequester = remember { FocusRequester() }
    var showMenu by remember { mutableStateOf(false) }
    var showCodespaceDialog by remember { mutableStateOf(false) }

    val active = tabs.firstOrNull { it.id == activeId } ?: tabs.firstOrNull()

    // Auto-start shell when tab becomes active
    LaunchedEffect(activeId) {
        val session = tabs.firstOrNull { it.id == activeId } ?: return@LaunchedEffect
        if (session.mode == TerminalMode.LOCAL && !session.connected) {
            session.start(scope)
        }
    }

    // Auto-scroll
    LaunchedEffect(active?.lines?.size) {
        val size = active?.lines?.size ?: 0
        if (size > 0) listState.animateScrollToItem(size - 1)
    }

    fun newTab(mode: TerminalMode = TerminalMode.LOCAL) {
        val id   = System.currentTimeMillis().toString()
        val num  = tabs.size + 1
        val name = if (mode == TerminalMode.LOCAL) "bash $num" else "cloud $num"
        val s    = LiveSession(id, name, mode)
        liveSessions[id] = s
        tabs.add(s)
        activeId = id
        if (mode == TerminalMode.CODESPACE) showCodespaceDialog = true
    }

    fun closeTab(id: String) {
        if (tabs.size <= 1) return
        val idx = tabs.indexOfFirst { it.id == id }
        tabs[idx].stop()
        liveSessions.remove(id)
        tabs.removeAt(idx)
        if (activeId == id) activeId = tabs.getOrNull(idx - 1)?.id ?: tabs.first().id
    }

    fun sendCmd(cmd: String) {
        val s = active ?: return
        val trimmed = cmd.trim()
        if (trimmed.isBlank()) { s.send("\n"); return }
        histIdx = -1
        s.lines.add("❯ $trimmed")
        when {
            trimmed == "clear" -> s.clear()
            trimmed.startsWith("cd ") -> {
                val path = trimmed.substring(3).trim().replace("~", TERMUX_HOME)
                val dir  = if (path.startsWith("/")) File(path) else File(s.workingDir, path)
                if (dir.exists() && dir.isDirectory) {
                    s.workingDir = dir.absolutePath
                    s.lines.add("📁 ${dir.absolutePath}")
                    s.send("cd ${dir.absolutePath}\n")
                } else {
                    s.lines.add("cd: $path: No such directory")
                }
            }
            else -> s.sendLine(trimmed)
        }
    }

    // Codespace info dialog
    if (showCodespaceDialog) {
        AlertDialog(
            onDismissRequest = { showCodespaceDialog = false },
            title = { Text("Codespace Terminal") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("GitHub Codespace SSH connection is coming soon.", fontSize = 13.sp)
                    Text("For now, use Local mode — it runs Termux bash with full access to node, python3, git, npm, and all your Termux packages.", fontSize = 13.sp)
                }
            },
            confirmButton = {
                TextButton(onClick = { showCodespaceDialog = false }) { Text("OK") }
            }
        )
    }

    Column(Modifier.fillMaxSize().background(Color(0xFF1E1E1E))) {

        // ── Tab bar ──────────────────────────────────────────────────────────
        Row(
            Modifier.fillMaxWidth().background(Color(0xFF252526)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(Modifier.weight(1f).horizontalScroll(rememberScrollState())) {
                tabs.forEach { tab ->
                    val isActive = tab.id == activeId
                    Row(
                        Modifier
                            .background(if (isActive) Color(0xFF1E1E1E) else Color(0xFF2D2D2D))
                            .clickable { activeId = tab.id }
                            .padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            if (tab.mode == TerminalMode.LOCAL) Icons.Default.Computer else Icons.Default.WbCloudy,
                            null,
                            tint = if (tab.mode == TerminalMode.LOCAL) Color(0xFF4EC994) else Color(0xFF89B4FA),
                            modifier = Modifier.size(11.dp),
                        )
                        Text(tab.name,
                            color  = if (isActive) Color.White else Color(0xFF969696),
                            fontSize = 12.sp,
                            fontWeight = if (isActive) FontWeight.Medium else FontWeight.Normal)
                        if (tabs.size > 1) {
                            Icon(Icons.Default.Close, null,
                                tint = Color(0xFF969696),
                                modifier = Modifier.size(10.dp).clickable { closeTab(tab.id) })
                        }
                    }
                }
            }

            // Local / Cloud toggle
            if (active != null) {
                Row(
                    Modifier.background(Color(0xFF2D2D2D)).padding(horizontal = 6.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    listOf(TerminalMode.LOCAL to "Local", TerminalMode.CODESPACE to "Cloud").forEach { (m, label) ->
                        val sel = active.mode == m
                        Box(
                            Modifier
                                .background(if (sel) Color(0xFF007ACC) else Color.Transparent,
                                    androidx.compose.foundation.shape.RoundedCornerShape(4.dp))
                                .clickable {
                                    active.mode = m
                                    if (m == TerminalMode.LOCAL && !active.connected) active.start(scope)
                                    if (m == TerminalMode.CODESPACE) showCodespaceDialog = true
                                }
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(label, fontSize = 11.sp, color = if (sel) Color.White else Color(0xFF969696))
                        }
                    }
                }
            }

            IconButton(onClick = { newTab() }, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Add, null, tint = Color(0xFF969696), modifier = Modifier.size(16.dp))
            }
            Box {
                IconButton(onClick = { showMenu = true }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.MoreVert, null, tint = Color(0xFF969696), modifier = Modifier.size(16.dp))
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false },
                    offset = DpOffset(0.dp, 4.dp), modifier = Modifier.background(Color(0xFF2D2D2D))) {
                    listOf(
                        "Clear" to { active?.clear() },
                        "New Local Terminal" to { newTab(TerminalMode.LOCAL) },
                        "New Cloud Terminal" to { newTab(TerminalMode.CODESPACE) },
                        "Kill Terminal" to { if (tabs.size > 1) closeTab(activeId) },
                    ).forEach { (label, action) ->
                        DropdownMenuItem(
                            text = { Text(label, color = Color(0xFFCCCCCC), fontSize = 13.sp) },
                            onClick = { showMenu = false; action() })
                    }
                }
            }
        }

        // ── Status banner ─────────────────────────────────────────────────────
        if (active != null) {
            Row(
                Modifier.fillMaxWidth()
                    .background(if (active.mode == TerminalMode.LOCAL) Color(0xFF0D1B0D) else Color(0xFF0D0D1B))
                    .padding(horizontal = 12.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    if (active.mode == TerminalMode.LOCAL) Icons.Default.Computer else Icons.Default.WbCloudy,
                    null,
                    tint = if (active.mode == TerminalMode.LOCAL) Color(0xFF4EC994) else Color(0xFF89B4FA),
                    modifier = Modifier.size(11.dp),
                )
                Text(
                    if (active.mode == TerminalMode.LOCAL)
                        "Local • ${active.workingDir.replace(TERMUX_HOME, "~")} • ${if (active.connected) "connected" else "disconnected"}"
                    else "Codespace • Not connected",
                    fontSize = 11.sp,
                    color = if (active.mode == TerminalMode.LOCAL) Color(0xFF4EC994) else Color(0xFF89B4FA),
                    fontFamily = FontFamily.Monospace,
                )
            }
        }

        // ── Output ────────────────────────────────────────────────────────────
        if (active != null) {
            Column(
                Modifier.fillMaxSize()
                    .clickable(indication = null, interactionSource = remember {
                        androidx.compose.foundation.interaction.MutableInteractionSource()
                    }) { focusRequester.requestFocus(); keyboardController?.show() }
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    items(active.lines) { line ->
                        Text(
                            line,
                            color = when {
                                line.startsWith("❯") -> Color(0xFF89B4FA)
                                line.startsWith("❌") || line.lowercase().contains("error") -> Color(0xFFF38BA8)
                                line.startsWith("📁") -> Color(0xFFA6E3A1)
                                line.startsWith("⚠") -> Color(0xFFF9E2AF)
                                line.startsWith("🖥") -> Color(0xFF89DCEB)
                                else -> Color(0xFFCDD6F4)
                            },
                            fontFamily = FontFamily.Monospace,
                            fontSize   = 13.sp,
                            modifier   = Modifier.padding(vertical = 1.dp),
                        )
                    }
                }

                // ── Input row ────────────────────────────────────────────────
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF252526))
                        .clickable(indication = null, interactionSource = remember {
                            androidx.compose.foundation.interaction.MutableInteractionSource()
                        }) { focusRequester.requestFocus(); keyboardController?.show() }
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(if (active.mode == TerminalMode.LOCAL) "❯ " else "☁ ",
                        color = if (active.mode == TerminalMode.LOCAL) Color(0xFF4EC994) else Color(0xFF89B4FA),
                        fontFamily = FontFamily.Monospace, fontSize = 14.sp)

                    BasicTextField(
                        value = input,
                        onValueChange = { v ->
                            if (v.endsWith("\n")) {
                                sendCmd(v.trimEnd('\n'))
                                input = ""
                                histIdx = -1
                            } else input = v
                        },
                        modifier = Modifier.weight(1f).focusRequester(focusRequester),
                        textStyle = TextStyle(color = Color(0xFFCDD6F4), fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp, lineHeight = 20.sp),
                        cursorBrush = SolidColor(Color(0xFF89B4FA)),
                        maxLines = 6,
                    )

                    // History arrows
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("▲", color = Color(0xFF969696), fontSize = 14.sp,
                            modifier = Modifier.clickable {
                                val h = active.history
                                if (h.isNotEmpty()) {
                                    histIdx = (histIdx + 1).coerceAtMost(h.size - 1)
                                    input = h[histIdx]
                                }
                            }.padding(horizontal = 6.dp, vertical = 2.dp))
                        Text("▼", color = Color(0xFF969696), fontSize = 14.sp,
                            modifier = Modifier.clickable {
                                histIdx--
                                input = if (histIdx < 0) { histIdx = -1; "" }
                                        else active.history.getOrElse(histIdx) { "" }
                            }.padding(horizontal = 6.dp, vertical = 2.dp))
                    }

                    // Special keys
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf("Tab" to "\t", "Ctrl+C" to "\u0003", "↑" to "\u001B[A", "↓" to "\u001B[B").forEach { (label, seq) ->
                            Box(
                                Modifier
                                    .background(Color(0xFF3A3A3A), androidx.compose.foundation.shape.RoundedCornerShape(3.dp))
                                    .clickable { active.send(seq) }
                                    .padding(horizontal = 6.dp, vertical = 4.dp)
                            ) {
                                Text(label, color = Color(0xFFCDD6F4), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }
                }
            }
        }
    }
}

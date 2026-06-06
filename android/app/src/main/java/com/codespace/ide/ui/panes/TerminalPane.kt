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
import androidx.compose.material.icons.filled.Cloud
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStreamWriter

private const val TERMINAL_PREFS = "terminal_history"
private const val TERMUX_PREFIX = "/data/data/com.termux/files/usr"
private const val TERMUX_HOME = "/data/data/com.termux/files/home"
private const val TERMUX_BASH = "/data/data/com.termux/files/usr/bin/bash"

enum class TerminalMode { LOCAL, CODESPACE }

data class TerminalSession(
    val id: String,
    val name: String,
    val lines: MutableList<String> = mutableListOf(),
    var workingDir: String = TERMUX_HOME,
    var mode: TerminalMode = TerminalMode.LOCAL,
    var commandHistory: MutableList<String> = mutableListOf(),
    var historyIndex: Int = -1,
)

// Persistent shell process per session
private val shellProcesses = mutableMapOf<String, Process>()
private val shellWriters = mutableMapOf<String, BufferedWriter>()

private fun getTermuxEnv(): Map<String, String> = mapOf(
    "PREFIX" to TERMUX_PREFIX,
    "HOME" to TERMUX_HOME,
    "TMPDIR" to "$TERMUX_PREFIX/tmp",
    "LANG" to "en_US.UTF-8",
    "TERM" to "xterm-256color",
    "PATH" to "$TERMUX_PREFIX/bin:$TERMUX_PREFIX/bin/applets:/system/bin:/system/xbin",
    "LD_LIBRARY_PATH" to "$TERMUX_PREFIX/lib",
    "SHELL" to TERMUX_BASH,
)

private fun startShell(sessionId: String, workingDir: String): Boolean {
    return try {
        shellProcesses[sessionId]?.destroy()
        val pb = ProcessBuilder(TERMUX_BASH, "--login", "-i")
            .directory(File(workingDir))
            .redirectErrorStream(true)
        pb.environment().putAll(getTermuxEnv())
        val process = pb.start()
        shellProcesses[sessionId] = process
        shellWriters[sessionId] = BufferedWriter(OutputStreamWriter(process.outputStream))
        true
    } catch (e: Exception) {
        false
    }
}

private fun killShell(sessionId: String) {
    shellWriters[sessionId]?.close()
    shellProcesses[sessionId]?.destroy()
    shellWriters.remove(sessionId)
    shellProcesses.remove(sessionId)
}

fun saveTerminals(context: Context, sessions: List<TerminalSession>, activeId: String) {
    val arr = JSONArray()
    sessions.forEach { s ->
        val linesArr = JSONArray()
        s.lines.takeLast(200).forEach { linesArr.put(it) }
        arr.put(JSONObject()
            .put("id", s.id)
            .put("name", s.name)
            .put("lines", linesArr)
            .put("workingDir", s.workingDir)
            .put("mode", s.mode.name))
    }
    context.getSharedPreferences(TERMINAL_PREFS, Context.MODE_PRIVATE)
        .edit()
        .putString("sessions", arr.toString())
        .putString("activeId", activeId)
        .apply()
}

fun loadTerminals(context: Context): Pair<List<TerminalSession>, String> {
    val prefs = context.getSharedPreferences(TERMINAL_PREFS, Context.MODE_PRIVATE)
    val str = prefs.getString("sessions", null)
    val activeId = prefs.getString("activeId", "1") ?: "1"
    if (str == null) return Pair(listOf(TerminalSession("1", "bash")), "1")
    return try {
        val arr = JSONArray(str)
        val sessions = (0 until arr.length()).map {
            val obj = arr.getJSONObject(it)
            val linesArr = obj.getJSONArray("lines")
            val lines = (0 until linesArr.length()).map { i -> linesArr.getString(i) }.toMutableList()
            TerminalSession(
                id = obj.getString("id"),
                name = obj.getString("name"),
                lines = lines,
                workingDir = obj.optString("workingDir", TERMUX_HOME),
                mode = try { TerminalMode.valueOf(obj.optString("mode", "LOCAL")) } catch (e: Exception) { TerminalMode.LOCAL },
            )
        }
        Pair(sessions, activeId)
    } catch (e: Exception) {
        Pair(listOf(TerminalSession("1", "bash")), "1")
    }
}

@Composable
fun TerminalPane() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current

    val (savedSessions, savedActiveId) = remember { loadTerminals(context) }
    val sessions = remember { mutableStateListOf(*savedSessions.toTypedArray()) }
    var activeId by remember { mutableStateOf(savedActiveId) }
    var input by remember { mutableStateOf("") }
    var historyIdx by remember { mutableStateOf(-1) }
    val listState = rememberLazyListState()
    val focusRequester = remember { FocusRequester() }
    var showTerminalMenu by remember { mutableStateOf(false) }
    var showCodespaceDialog by remember { mutableStateOf(false) }
    var csHost by remember { mutableStateOf("") }
    var csUser by remember { mutableStateOf("") }

    val activeSession = sessions.firstOrNull { it.id == activeId } ?: sessions.firstOrNull()
    val isLocalMode = activeSession?.mode == TerminalMode.LOCAL

    // Start shell for active session if not running
    LaunchedEffect(activeId) {
        val session = sessions.firstOrNull { it.id == activeId } ?: return@LaunchedEffect
        if (session.mode == TerminalMode.LOCAL && !shellProcesses.containsKey(activeId)) {
            withContext(Dispatchers.IO) {
                val started = startShell(activeId, session.workingDir)
                if (started) {
                    session.lines.add("🖥  Local Terminal — Termux bash")
                    session.lines.add("📁 ${session.workingDir}")
                    // Read initial shell output
                    kotlinx.coroutines.delay(500)
                    val reader = BufferedReader(InputStreamReader(shellProcesses[activeId]!!.inputStream))
                    // Drain any initial output
                }
            }
        }
    }

    LaunchedEffect(activeSession?.lines?.size) {
        val size = activeSession?.lines?.size ?: 0
        if (size > 0) listState.animateScrollToItem(size - 1)
    }

    fun saveAll() = saveTerminals(context, sessions, activeId)

    fun addTerminal(mode: TerminalMode = TerminalMode.LOCAL) {
        val newId = System.currentTimeMillis().toString()
        val num = sessions.size + 1
        val name = if (mode == TerminalMode.LOCAL) "bash $num" else "codespace $num"
        sessions.add(TerminalSession(newId, name, mode = mode))
        activeId = newId
        saveAll()
    }

    fun closeTerminal(id: String) {
        if (sessions.size <= 1) return
        val idx = sessions.indexOfFirst { it.id == id }
        sessions.removeAt(idx)
        killShell(id)
        if (activeId == id) activeId = sessions.getOrNull(idx - 1)?.id ?: sessions.first().id
        saveAll()
    }

    fun clearTerminal() {
        val session = sessions.firstOrNull { it.id == activeId } ?: return
        session.lines.clear()
        saveAll()
    }

    fun runLocalCommand(cmd: String) {
        val session = sessions.firstOrNull { it.id == activeId } ?: return
        val sessionIdx = sessions.indexOfFirst { it.id == activeId }
        val trimmed = cmd.trim()
        if (trimmed.isBlank()) return

        // Add to history
        session.commandHistory.add(0, trimmed)
        if (session.commandHistory.size > 100) session.commandHistory.removeLast()
        historyIdx = -1

        session.lines.add("❯ $trimmed")

        scope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    val workingDir = File(session.workingDir)
                    when {
                        trimmed == "clear" -> { session.lines.clear(); listOf() }
                        trimmed == "pwd" -> listOf(session.workingDir)
                        trimmed.startsWith("cd ") -> {
                            val newPath = trimmed.substring(3).trim()
                                .replace("~", TERMUX_HOME)
                            val newDir = if (newPath.startsWith("/")) File(newPath)
                                         else File(workingDir, newPath)
                            if (newDir.exists() && newDir.isDirectory) {
                                sessions[sessionIdx] = sessions[sessionIdx].copy(workingDir = newDir.absolutePath)
                                listOf("📁 ${newDir.absolutePath}")
                            } else listOf("cd: ${newPath}: No such directory")
                        }
                        else -> {
                            // Run via Termux bash
                            val pb = ProcessBuilder(TERMUX_BASH, "-c", trimmed)
                                .directory(File(session.workingDir))
                                .redirectErrorStream(true)
                            pb.environment().putAll(getTermuxEnv())
                            val process = pb.start()
                            val reader = BufferedReader(InputStreamReader(process.inputStream))
                            val output = mutableListOf<String>()
                            var line: String?
                            while (reader.readLine().also { line = it } != null) {
                                output.add(line!!)
                            }
                            val exitCode = process.waitFor()
                            if (output.isEmpty()) {
                                if (exitCode == 0) listOf() else listOf("Exit code: $exitCode")
                            } else output
                        }
                    }
                }
                session.lines.addAll(result)
            } catch (e: Exception) {
                session.lines.add("❌ Error: ${e.message}")
            }
            saveAll()
        }
    }

    fun runCodespaceCommand(cmd: String) {
        val session = sessions.firstOrNull { it.id == activeId } ?: return
        session.lines.add("❯ $cmd")
        session.lines.add("⚠ Codespace SSH not configured. Go to Settings → Codespace to add credentials.")
        saveAll()
    }

    // Codespace dialog
    if (showCodespaceDialog) {
        AlertDialog(
            onDismissRequest = { showCodespaceDialog = false },
            title = { Text("Connect to Codespace") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Enter your GitHub Codespace SSH details:", fontSize = 13.sp)
                    OutlinedTextField(
                        value = csHost,
                        onValueChange = { csHost = it },
                        label = { Text("Host (e.g. ssh.github.com)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = csUser,
                        onValueChange = { csUser = it },
                        label = { Text("Username") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text("Full SSH support coming soon. Use Local mode with Termux for now.", fontSize = 11.sp, color = Color(0xFF969696))
                }
            },
            confirmButton = {
                TextButton(onClick = { showCodespaceDialog = false }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showCodespaceDialog = false }) { Text("Cancel") }
            }
        )
    }

    Column(Modifier.fillMaxSize().background(Color(0xFF1E1E1E))) {

        // ── Tab bar ──────────────────────────────────────────────────────────
        Row(
            Modifier.fillMaxWidth().background(Color(0xFF252526)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                Modifier.weight(1f).horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                sessions.forEach { session ->
                    val isActive = session.id == activeId
                    Row(
                        Modifier
                            .background(if (isActive) Color(0xFF1E1E1E) else Color(0xFF2D2D2D))
                            .clickable { activeId = session.id; saveAll() }
                            .padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            if (session.mode == TerminalMode.LOCAL) Icons.Default.Computer else Icons.Default.Cloud,
                            null,
                            tint = if (session.mode == TerminalMode.LOCAL) Color(0xFF4EC994) else Color(0xFF89B4FA),
                            modifier = Modifier.size(12.dp),
                        )
                        Text(
                            session.name,
                            color = if (isActive) Color.White else Color(0xFF969696),
                            fontSize = 12.sp,
                            fontWeight = if (isActive) FontWeight.Medium else FontWeight.Normal,
                        )
                        if (sessions.size > 1) {
                            Icon(Icons.Default.Close, null,
                                tint = Color(0xFF969696),
                                modifier = Modifier.size(10.dp).clickable { closeTerminal(session.id) })
                        }
                    }
                }
            }

            // Mode toggle for active session
            if (activeSession != null) {
                Row(
                    Modifier.background(Color(0xFF2D2D2D)).padding(horizontal = 6.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    listOf(TerminalMode.LOCAL to "Local", TerminalMode.CODESPACE to "Cloud").forEach { (mode, label) ->
                        val isSelected = activeSession.mode == mode
                        Box(
                            Modifier
                                .background(
                                    if (isSelected) Color(0xFF007ACC) else Color.Transparent,
                                    androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
                                )
                                .clickable {
                                    val idx = sessions.indexOfFirst { it.id == activeId }
                                    if (idx >= 0) {
                                        sessions[idx] = sessions[idx].copy(mode = mode)
                                        if (mode == TerminalMode.CODESPACE) showCodespaceDialog = true
                                    }
                                    saveAll()
                                }
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                        ) {
                            Text(label, fontSize = 11.sp, color = if (isSelected) Color.White else Color(0xFF969696))
                        }
                    }
                }
            }

            IconButton(onClick = { addTerminal() }, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Add, null, tint = Color(0xFF969696), modifier = Modifier.size(16.dp))
            }
            Box {
                IconButton(onClick = { showTerminalMenu = true }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.MoreVert, null, tint = Color(0xFF969696), modifier = Modifier.size(16.dp))
                }
                DropdownMenu(
                    expanded = showTerminalMenu,
                    onDismissRequest = { showTerminalMenu = false },
                    offset = DpOffset(0.dp, 4.dp),
                    modifier = Modifier.background(Color(0xFF2D2D2D)),
                ) {
                    listOf(
                        "Clear" to { clearTerminal() },
                        "New Local Terminal" to { addTerminal(TerminalMode.LOCAL) },
                        "New Codespace Terminal" to { addTerminal(TerminalMode.CODESPACE) },
                        "Kill Terminal" to { if (sessions.size > 1) closeTerminal(activeId) },
                    ).forEach { (label, action) ->
                        DropdownMenuItem(
                            text = { Text(label, color = Color(0xFFCCCCCC), fontSize = 13.sp) },
                            onClick = { showTerminalMenu = false; action() },
                        )
                    }
                }
            }
        }

        // ── Mode banner ───────────────────────────────────────────────────────
        if (activeSession != null) {
            Row(
                Modifier.fillMaxWidth()
                    .background(if (isLocalMode) Color(0xFF0D1B0D) else Color(0xFF0D0D1B))
                    .padding(horizontal = 12.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    if (isLocalMode) Icons.Default.Computer else Icons.Default.Cloud,
                    null,
                    tint = if (isLocalMode) Color(0xFF4EC994) else Color(0xFF89B4FA),
                    modifier = Modifier.size(12.dp),
                )
                Text(
                    if (isLocalMode) "Local • Termux • ${activeSession.workingDir.replace(TERMUX_HOME, "~")}"
                    else "Codespace • Not connected",
                    fontSize = 11.sp,
                    color = if (isLocalMode) Color(0xFF4EC994) else Color(0xFF89B4FA),
                    fontFamily = FontFamily.Monospace,
                )
            }
        }

        // ── Output + Input ────────────────────────────────────────────────────
        if (activeSession != null) {
            Column(
                Modifier.fillMaxSize()
                    .clickable(indication = null, interactionSource = remember {
                        androidx.compose.foundation.interaction.MutableInteractionSource()
                    }) {
                        focusRequester.requestFocus()
                        keyboardController?.show()
                    }
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    items(activeSession.lines) { line ->
                        Text(
                            line,
                            color = when {
                                line.startsWith("❯") -> Color(0xFF89B4FA)
                                line.startsWith("❌") || line.startsWith("Error") -> Color(0xFFF38BA8)
                                line.startsWith("📁") || line.startsWith("->") -> Color(0xFFA6E3A1)
                                line.startsWith("⚠") -> Color(0xFFF9E2AF)
                                line.startsWith("🖥") || line.startsWith("☁") -> Color(0xFF89DCEB)
                                else -> Color(0xFFCDD6F4)
                            },
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(vertical = 1.dp),
                        )
                    }
                }

                // Input row
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF252526))
                        .clickable(indication = null, interactionSource = remember {
                            androidx.compose.foundation.interaction.MutableInteractionSource()
                        }) {
                            focusRequester.requestFocus()
                            keyboardController?.show()
                        }
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Prompt
                    Text(
                        if (isLocalMode) "❯ " else "☁ ",
                        color = if (isLocalMode) Color(0xFF4EC994) else Color(0xFF89B4FA),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp,
                    )
                    BasicTextField(
                        value = input,
                        onValueChange = { newValue ->
                            if (newValue.endsWith("\n")) {
                                val cmd = newValue.trimEnd('\n')
                                if (isLocalMode) runLocalCommand(cmd) else runCodespaceCommand(cmd)
                                input = ""
                                historyIdx = -1
                            } else {
                                input = newValue
                            }
                        },
                        modifier = Modifier.weight(1f).focusRequester(focusRequester),
                        textStyle = TextStyle(
                            color = Color(0xFFCDD6F4),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            lineHeight = 20.sp,
                        ),
                        cursorBrush = SolidColor(Color(0xFF89B4FA)),
                        maxLines = 6,
                    )
                    // History up/down
                    Column {
                        Text("▲", color = Color(0xFF969696), fontSize = 12.sp,
                            modifier = Modifier.clickable {
                                val hist = activeSession.commandHistory
                                if (hist.isNotEmpty()) {
                                    historyIdx = (historyIdx + 1).coerceAtMost(hist.size - 1)
                                    input = hist[historyIdx]
                                }
                            }.padding(4.dp))
                        Text("▼", color = Color(0xFF969696), fontSize = 12.sp,
                            modifier = Modifier.clickable {
                                historyIdx = (historyIdx - 1)
                                input = if (historyIdx < 0) { historyIdx = -1; "" }
                                        else activeSession.commandHistory.getOrElse(historyIdx) { "" }
                            }.padding(4.dp))
                    }
                }
            }
        }
    }
}

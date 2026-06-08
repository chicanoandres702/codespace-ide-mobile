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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

private const val TERMINAL_PREFS = "terminal_history"
private const val TERMUX_PREFIX = "/data/data/com.termux/files/usr"
private const val TERMUX_HOME = "/data/data/com.termux/files/home"
private const val TERMUX_BASH = "/data/data/com.termux/files/usr/bin/bash"

// Terminal colors
private val TermBg       = Color(0xFF1E1E1E)
private val TermText     = Color(0xFFCDD6F4)
private val TermGreen    = Color(0xFF4EC994)
private val TermBlue     = Color(0xFF89B4FA)
private val TermYellow   = Color(0xFFF9E2AF)
private val TermRed      = Color(0xFFF38BA8)
private val TermGray     = Color(0xFF6C7086)
private val TermCyan     = Color(0xFF89DCEB)
private val TermTabBg    = Color(0xFF252526)

data class TerminalSession(
    val id: String,
    val name: String,
    val lines: MutableList<TermLine> = mutableListOf(),
    var workingDir: String = TERMUX_HOME,
    val history: MutableList<String> = mutableListOf(),
)

data class TermLine(
    val text: String,
    val type: LineType = LineType.OUTPUT,
    val prompt: String = "",
)

enum class LineType { PROMPT, OUTPUT, ERROR, SUCCESS, INFO }

fun saveTerminals(context: Context, sessions: List<TerminalSession>, activeId: String) {
    val arr = JSONArray()
    sessions.forEach { s ->
        val linesArr = JSONArray()
        s.lines.takeLast(100).forEach { linesArr.put(it.text) }
        arr.put(JSONObject()
            .put("id", s.id)
            .put("name", s.name)
            .put("lines", linesArr)
            .put("workingDir", s.workingDir))
    }
    context.getSharedPreferences(TERMINAL_PREFS, Context.MODE_PRIVATE)
        .edit().putString("sessions", arr.toString())
        .putString("activeId", activeId).apply()
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
            val lines = (0 until linesArr.length()).map { i ->
                TermLine(linesArr.getString(i))
            }.toMutableList()
            TerminalSession(
                id = obj.getString("id"),
                name = obj.getString("name"),
                lines = lines,
                workingDir = obj.optString("workingDir", TERMUX_HOME),
            )
        }
        Pair(sessions, activeId)
    } catch (e: Exception) {
        Pair(listOf(TerminalSession("1", "bash")), "1")
    }
}

private fun getPromptText(workingDir: String): String {
    val home = TERMUX_HOME
    val displayPath = if (workingDir.startsWith(home))
        "~" + workingDir.removePrefix(home)
    else workingDir
    // Get git branch if available
    val branch = try {
        val pb = ProcessBuilder(
            if (File(TERMUX_BASH).exists()) TERMUX_BASH else "sh",
            "-c", "git -C "$workingDir" branch --show-current 2>/dev/null"
        ).also { p ->
            p.environment().apply {
                put("PATH", "$TERMUX_PREFIX/bin:/system/bin")
                put("HOME", home)
            }
        }
        pb.start().inputStream.bufferedReader().readLine()?.trim() ?: ""
    } catch (e: Exception) { "" }
    return if (branch.isNotEmpty()) "$displayPath ($branch)" else displayPath
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
    var histIdx by remember { mutableStateOf(-1) }
    val listState = rememberLazyListState()
    val focusRequester = remember { FocusRequester() }
    var showMenu by remember { mutableStateOf(false) }

    val activeSession = sessions.firstOrNull { it.id == activeId } ?: sessions.firstOrNull()

    LaunchedEffect(activeSession?.lines?.size) {
        val size = activeSession?.lines?.size ?: 0
        if (size > 0) listState.animateScrollToItem(size - 1)
    }

    fun saveAll() = saveTerminals(context, sessions, activeId)

    fun addTerminal() {
        val newId = System.currentTimeMillis().toString()
        val num = sessions.size + 1
        sessions.add(TerminalSession(newId, "bash $num"))
        activeId = newId
        saveAll()
    }

    fun closeTerminal(id: String) {
        if (sessions.size <= 1) return
        val idx = sessions.indexOfFirst { it.id == id }
        sessions.removeAt(idx)
        if (activeId == id) activeId = sessions.getOrNull(idx - 1)?.id ?: sessions.first().id
        saveAll()
    }

    fun runCommand(cmd: String) {
        val session = sessions.firstOrNull { it.id == activeId } ?: return
        val sessionIdx = sessions.indexOfFirst { it.id == activeId }
        val trimmed = cmd.trim()
        if (trimmed.isBlank()) return

        // Add to history
        if (session.history.isEmpty() || session.history.first() != trimmed) {
            session.history.add(0, trimmed)
            if (session.history.size > 100) session.history.removeLastOrNull()
        }
        histIdx = -1

        // Add prompt line showing the command
        val promptPath = session.workingDir.let {
            if (it.startsWith(TERMUX_HOME)) "~" + it.removePrefix(TERMUX_HOME) else it
        }
        session.lines.add(TermLine("$promptPath \$ $trimmed", LineType.PROMPT))

        scope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    val workingDir = File(session.workingDir)
                    when {
                        trimmed == "clear" -> {
                            session.lines.clear()
                            listOf<TermLine>()
                        }
                        trimmed == "pwd" -> listOf(TermLine(session.workingDir))
                        trimmed == "exit" -> {
                            if (sessions.size > 1) closeTerminal(activeId)
                            listOf<TermLine>()
                        }
                        trimmed.startsWith("cd ") -> {
                            val newPath = trimmed.substring(3).trim()
                                .replace("~", TERMUX_HOME)
                            val newDir = if (newPath.startsWith("/")) File(newPath)
                                         else File(workingDir, newPath)
                            if (newDir.exists() && newDir.isDirectory) {
                                sessions[sessionIdx] = sessions[sessionIdx].copy(
                                    workingDir = newDir.absolutePath)
                                listOf<TermLine>()
                            } else {
                                listOf(TermLine("cd: $newPath: No such file or directory", LineType.ERROR))
                            }
                        }
                        else -> {
                            val shell = if (File(TERMUX_BASH).exists()) TERMUX_BASH else "sh"
                            val pb = ProcessBuilder(shell, "-c", trimmed)
                                .directory(workingDir)
                                .redirectErrorStream(true)
                            pb.environment().apply {
                                put("PREFIX", TERMUX_PREFIX)
                                put("HOME", TERMUX_HOME)
                                put("TMPDIR", "$TERMUX_PREFIX/tmp")
                                put("LANG", "en_US.UTF-8")
                                put("TERM", "xterm-256color")
                                put("PATH", "$TERMUX_PREFIX/bin:$TERMUX_PREFIX/bin/applets:/system/bin:/system/xbin")
                                put("LD_LIBRARY_PATH", "$TERMUX_PREFIX/lib")
                            }
                            val process = pb.start()
                            val reader = BufferedReader(InputStreamReader(process.inputStream))
                            val output = mutableListOf<TermLine>()
                            var line: String?
                            while (reader.readLine().also { line = it } != null) {
                                val l = line!!
                                    .replace(Regex("\[[0-9;]*[mKHJABCDsuGr]"), "")
                                    .replace(Regex("\([AB]"), "")
                                output.add(TermLine(l))
                            }
                            val exit = process.waitFor()
                            if (output.isEmpty() && exit != 0) {
                                output.add(TermLine("Exit code: $exit", LineType.ERROR))
                            }
                            output
                        }
                    }
                }
                session.lines.addAll(result)
            } catch (e: Exception) {
                session.lines.add(TermLine("Error: ${e.message}", LineType.ERROR))
            }
            saveAll()
        }
    }

    Column(Modifier.fillMaxSize().background(TermBg)) {

        // ── Tab bar ──────────────────────────────────────────────────────
        Row(
            Modifier.fillMaxWidth().background(TermTabBg),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(Modifier.weight(1f).horizontalScroll(rememberScrollState())) {
                sessions.forEach { session ->
                    val isActive = session.id == activeId
                    Row(
                        Modifier
                            .background(if (isActive) TermBg else TermTabBg)
                            .clickable { activeId = session.id; saveAll() }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(session.name,
                            color = if (isActive) Color.White else TermGray,
                            fontSize = 12.sp,
                            fontWeight = if (isActive) FontWeight.Medium else FontWeight.Normal,
                            fontFamily = FontFamily.Monospace)
                        if (sessions.size > 1) {
                            Icon(Icons.Default.Close, null,
                                tint = TermGray,
                                modifier = Modifier.size(10.dp).clickable { closeTerminal(session.id) })
                        }
                    }
                }
            }
            IconButton(onClick = { addTerminal() }, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Add, null, tint = TermGray, modifier = Modifier.size(16.dp))
            }
            Box {
                IconButton(onClick = { showMenu = true }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.MoreVert, null, tint = TermGray, modifier = Modifier.size(16.dp))
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false },
                    offset = DpOffset(0.dp, 4.dp),
                    modifier = Modifier.background(Color(0xFF2D2D2D))) {
                    listOf(
                        "Clear" to { activeSession?.lines?.clear(); saveAll() },
                        "New Terminal" to { addTerminal() },
                        "Kill Terminal" to { if (sessions.size > 1) closeTerminal(activeId) },
                    ).forEach { (label, action) ->
                        DropdownMenuItem(
                            text = { Text(label, color = TermText, fontSize = 13.sp) },
                            onClick = { showMenu = false; action() })
                    }
                }
            }
        }

        // ── Output + Input ────────────────────────────────────────────────
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
                // Output lines
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f).fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                ) {
                    items(activeSession.lines) { line ->
                        when (line.type) {
                            LineType.PROMPT -> {
                                // Colored prompt line
                                val parts = line.text.split(" \$ ", limit = 2)
                                Row(Modifier.padding(vertical = 1.dp)) {
                                    Text(
                                        buildAnnotatedString {
                                            withStyle(SpanStyle(color = TermGreen, fontWeight = FontWeight.Bold)) {
                                                append(parts.getOrElse(0) { "" })
                                            }
                                            withStyle(SpanStyle(color = TermGray)) {
                                                append(" \$ ")
                                            }
                                            withStyle(SpanStyle(color = TermText)) {
                                                append(parts.getOrElse(1) { "" })
                                            }
                                        },
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 13.sp,
                                    )
                                }
                            }
                            LineType.ERROR -> Text(line.text, color = TermRed,
                                fontFamily = FontFamily.Monospace, fontSize = 13.sp,
                                modifier = Modifier.padding(vertical = 1.dp))
                            LineType.SUCCESS -> Text(line.text, color = TermGreen,
                                fontFamily = FontFamily.Monospace, fontSize = 13.sp,
                                modifier = Modifier.padding(vertical = 1.dp))
                            LineType.INFO -> Text(line.text, color = TermCyan,
                                fontFamily = FontFamily.Monospace, fontSize = 13.sp,
                                modifier = Modifier.padding(vertical = 1.dp))
                            else -> Text(line.text, color = TermText,
                                fontFamily = FontFamily.Monospace, fontSize = 13.sp,
                                modifier = Modifier.padding(vertical = 1.dp))
                        }
                    }
                }

                // ── Input row with colored prompt ─────────────────────────
                val promptPath = activeSession.workingDir.let {
                    if (it.startsWith(TERMUX_HOME)) "~" + it.removePrefix(TERMUX_HOME) else it
                }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(TermBg)
                        .clickable(indication = null, interactionSource = remember {
                            androidx.compose.foundation.interaction.MutableInteractionSource()
                        }) {
                            focusRequester.requestFocus()
                            keyboardController?.show()
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Colored prompt
                    Text(
                        buildAnnotatedString {
                            withStyle(SpanStyle(color = TermGreen, fontWeight = FontWeight.Bold)) {
                                append(promptPath)
                            }
                            withStyle(SpanStyle(color = TermGray)) {
                                append(" \$ ")
                            }
                        },
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                    )
                    // Input field
                    BasicTextField(
                        value = input,
                        onValueChange = { v ->
                            if (v.endsWith("
")) {
                                runCommand(v.trimEnd('\n'))
                                input = ""
                                histIdx = -1
                            } else {
                                input = v
                            }
                        },
                        modifier = Modifier.weight(1f).focusRequester(focusRequester),
                        textStyle = TextStyle(
                            color = TermText,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            lineHeight = 20.sp,
                        ),
                        cursorBrush = SolidColor(TermGreen),
                        maxLines = 5,
                    )
                    // History navigation
                    Column {
                        Text("▲", color = TermGray, fontSize = 14.sp,
                            modifier = Modifier
                                .clickable {
                                    val h = activeSession.history
                                    if (h.isNotEmpty()) {
                                        histIdx = (histIdx + 1).coerceAtMost(h.size - 1)
                                        input = h[histIdx]
                                    }
                                }
                                .padding(4.dp))
                        Text("▼", color = TermGray, fontSize = 14.sp,
                            modifier = Modifier
                                .clickable {
                                    histIdx--
                                    input = if (histIdx < 0) { histIdx = -1; "" }
                                            else activeSession.history.getOrElse(histIdx) { "" }
                                }
                                .padding(4.dp))
                    }
                }

                // ── Quick keys toolbar ────────────────────────────────────
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF252526))
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    listOf("Tab" to "	", "Ctrl+C" to "", "Ctrl+D" to "",
                           "←" to "", "→" to "", "↑" to "", "↓" to "").forEach { (label, _) ->
                        Box(
                            Modifier
                                .background(Color(0xFF3A3A3A), androidx.compose.foundation.shape.RoundedCornerShape(3.dp))
                                .clickable {
                                    when (label) {
                                        "Tab"    -> input += "	"
                                        "Ctrl+C" -> { input = ""; activeSession.lines.add(TermLine("^C", LineType.ERROR)) }
                                        "Ctrl+D" -> runCommand("exit")
                                        "↑"      -> {
                                            val h = activeSession.history
                                            if (h.isNotEmpty()) {
                                                histIdx = (histIdx + 1).coerceAtMost(h.size - 1)
                                                input = h[histIdx]
                                            }
                                        }
                                        "↓"      -> {
                                            histIdx--
                                            input = if (histIdx < 0) { histIdx = -1; "" }
                                                    else activeSession.history.getOrElse(histIdx) { "" }
                                        }
                                    }
                                }
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(label, color = TermText, fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        }
    }
}

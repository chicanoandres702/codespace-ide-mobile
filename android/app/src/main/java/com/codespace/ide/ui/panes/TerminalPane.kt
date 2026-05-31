package com.codespace.ide.ui.panes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

@Composable
fun TerminalPane() {
    var input by remember { mutableStateOf("") }
    val lines = remember { mutableStateListOf("CodeSpace Terminal — type commands below") }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    var workingDir by remember { mutableStateOf(File("/storage/emulated/0")) }

    LaunchedEffect(lines.size) {
        if (lines.isNotEmpty()) listState.animateScrollToItem(lines.size - 1)
    }

    fun runCommand(cmd: String) {
        if (cmd.isBlank()) return
        lines.add("$ $cmd")

        scope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    if (cmd.startsWith("cd ")) {
                        val newPath = cmd.substring(3).trim()
                        val newDir = if (newPath.startsWith("/"))
                            File(newPath)
                        else File(workingDir, newPath)
                        if (newDir.exists() && newDir.isDirectory) {
                            workingDir = newDir
                            listOf("→ ${newDir.absolutePath}")
                        } else {
                            listOf("cd: no such directory: $newPath")
                        }
                    } else if (cmd == "clear") {
                        lines.clear()
                        listOf()
                    } else if (cmd == "ls") {
                        workingDir.listFiles()
                            ?.sortedWith(compareByDescending<File> { it.isDirectory }.thenBy { it.name })
                            ?.map { if (it.isDirectory) "${it.name}/" else it.name }
                            ?: listOf("Permission denied")
                    } else if (cmd == "pwd") {
                        listOf(workingDir.absolutePath)
                    } else {
                        val process = ProcessBuilder("sh", "-c", cmd)
                            .directory(workingDir)
                            .redirectErrorStream(true)
                            .start()
                        val reader = BufferedReader(InputStreamReader(process.inputStream))
                        val output = mutableListOf<String>()
                        var line: String?
                        while (reader.readLine().also { line = it } != null) {
                            output.add(line!!)
                        }
                        process.waitFor()
                        if (output.isEmpty()) listOf("✓ Done") else output
                    }
                }
                lines.addAll(result)
            } catch (e: Exception) {
                lines.add("Error: ${e.message}")
            }
        }
    }

    Column(Modifier.fillMaxSize().background(Color(0xFF11111B))) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(12.dp),
        ) {
            items(lines) { line ->
                Text(
                    line,
                    color = if (line.startsWith("$")) Color(0xFF89B4FA)
                    else if (line.startsWith("Error")) Color(0xFFF38BA8)
                    else Color(0xFFCDD6F4),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                )
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                singleLine = true,
                placeholder = { Text("Enter command...", color = Color.Gray) },
            )
            IconButton(onClick = {
                runCommand(input)
                input = ""
            }) {
                Icon(Icons.Default.Send, contentDescription = "Run", tint = Color(0xFF89B4FA))
            }
        }
    }
}

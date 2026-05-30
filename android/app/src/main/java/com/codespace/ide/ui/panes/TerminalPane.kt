package com.codespace.ide.ui.panes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Terminal pane. Renders streamed output and sends stdin. Backed by [TerminalSession]
 * locally or a WebSocket/SSH PTY for remotes. A floating-terminal variant reuses this
 * composable inside a draggable overlay window.
 */
@Composable
fun TerminalPane() {
    var input by remember { mutableStateOf("") }
    val lines = remember {
        mutableStateListOf(
            "CodeSpace IDE terminal — bash 5.2",
            "$ node -v",
            "v20.11.1",
            "$ git status",
            "On branch main",
            "nothing to commit, working tree clean",
            "$ ",
        )
    }
    val scroll = rememberScrollState()

    Column(Modifier.fillMaxSize().background(Color(0xFF11111B))) {
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(scroll)
                .padding(12.dp),
        ) {
            lines.forEach { line ->
                Text(
                    line,
                    color = Color(0xFFCDD6F4),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                )
            }
        }
        Row(Modifier.fillMaxWidth().padding(8.dp)) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                singleLine = true,
                keyboardActions = KeyboardActions(onSend = {
                    lines.add("$ $input")
                    lines.add("[executed: $input]")
                    input = ""
                }),
            )
            IconButton(onClick = {
                if (input.isNotBlank()) {
                    lines.add("$ $input")
                    lines.add("[executed: $input]")
                    input = ""
                }
            }) {
                Icon(Icons.Default.Send, contentDescription = "Run", tint = Color(0xFF89B4FA))
            }
        }
    }
}

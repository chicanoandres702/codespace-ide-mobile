package com.codespace.ide.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.Dialog

// UI State Management
@Composable
fun ProjectShellScreen() {
    var showChat by remember { mutableStateOf(false) }
    var showTools by remember { mutableStateOf(false) }
    var showAttachments by remember { mutableStateOf(false) }
    var bottomPanelHeight by remember { mutableStateOf(200.dp) }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF1E1E1E))) {
        // Main Editor Surface (Tap outside to close modals)
        Box(modifier = Modifier.fillMaxSize().pointerInput(Unit) {
            detectDragGestures { change, dragAmount -> 
                // Logic for panel resizing
            }
        })

        // Top Right Split-View Trigger (Extreme End)
        IconButton(
            onClick = { showChat = !showChat },
            modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
        ) {
            Icon(Icons.Default.VerticalSplit, "Toggle Chat", tint = if (showChat) Color.White else Color.Gray)
        }

        // Floating AI Chat Panel (Right Side)
        if (showChat) {
            Surface(modifier = Modifier.fillMaxHeight().width(300.dp).align(Alignment.CenterEnd), color = Color(0xFF252526)) {
                Column {
                    Row(modifier = Modifier.padding(8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("CHAT", color = Color.White)
                        IconButton(onClick = { showChat = false }) { Icon(Icons.Default.Close, "Close", tint = Color.Gray) }
                    }
                    // Chat content with Add-on Menu (3-dot + icon)
                    IconButton(onClick = { showAttachments = true }) { Icon(Icons.Default.Add, "Attach") }
                    IconButton(onClick = { /* Undo logic */ }) { Icon(Icons.Default.MoreVert, "Menu") }
                    // Tool Configuration Button (2 lines with circles)
                    IconButton(onClick = { showTools = true }) { Icon(Icons.Default.Tune, "Tools") }
                }
            }
        }

        // Configure Tools Modal (Never Stuck - Includes Close Button)
        if (showTools) {
            Dialog(onDismissRequest = { showTools = false }) {
                Surface(color = Color(0xFF252526), modifier = Modifier.padding(16.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Configure Tools", color = Color.White)
                        Checkbox(checked = true, onCheckedChange = {})
                        Text("Agent | Execute | Read | Todo", color = Color.Gray)
                        Button(onClick = { showTools = false }) { Text("OK") }
                    }
                }
            }
        }

        // Notification Bell (Bottom Right)
        Icon(
            Icons.Default.Notifications, 
            "Notifications", 
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp).clickable { /* Open Notification Hub */ },
            tint = Color.White
        )
    }
}

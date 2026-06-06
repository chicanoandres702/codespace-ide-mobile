package com.codespace.ide.ui.panes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File

private val BgColor      = Color(0xFFFFFFFF)
private val HoverBg      = Color(0xFFE8F0FE)
private val SelectedBg   = Color(0xFFCCE5FF)
private val TextColor    = Color(0xFF333333)
private val MutedColor   = Color(0xFF717171)
private val DividerColor = Color(0xFFE0E0E0)
private val FolderColor  = Color(0xFFDCB67A)
private val IconColor    = Color(0xFF007ACC)

data class FsNode(
    val file: File,
    val depth: Int,
    val isExpanded: Boolean = false,
)

@Composable
fun ExplorerSidePanel(
    onOpenFile: (String) -> Unit,
    onMoreMenu: () -> Unit,
) {
    val context = LocalContext.current
    val root    = remember { File("/storage/emulated/0") }

    // Which dirs are expanded
    val expanded    = remember { mutableStateMapOf<String, Boolean>() }
    var selected    by remember { mutableStateOf<String?>(null) }
    var contextFile by remember { mutableStateOf<File?>(null) }
    var showCtxMenu by remember { mutableStateOf(false) }
    var showNewFile by remember { mutableStateOf(false) }
    var showNewFolder by remember { mutableStateOf(false) }
    var showRename  by remember { mutableStateOf(false) }
    var showDelete  by remember { mutableStateOf(false) }
    var nameInput   by remember { mutableStateOf("") }
    var refresh     by remember { mutableStateOf(0) } // increment to force recompose

    fun buildNodes(dir: File, depth: Int): List<FsNode> {
        val nodes = mutableListOf<FsNode>()
        val isExp = expanded[dir.absolutePath] ?: false
        nodes.add(FsNode(dir, depth, isExp))
        if (isExp) {
            val children = dir.listFiles()
                ?.sortedWith(compareByDescending<File> { it.isDirectory }.thenBy { it.name })
                ?: emptyList()
            children.forEach { child ->
                if (child.isDirectory) nodes.addAll(buildNodes(child, depth + 1))
                else nodes.add(FsNode(child, depth + 1))
            }
        }
        return nodes
    }

    val nodes = remember(expanded.toMap(), refresh) {
        val topLevel = root.listFiles()
            ?.filter { !it.name.startsWith(".") }
            ?.sortedWith(compareByDescending<File> { it.isDirectory }.thenBy { it.name })
            ?: emptyList()
        topLevel.flatMap { f ->
            if (f.isDirectory) buildNodes(f, 0)
            else listOf(FsNode(f, 0))
        }
    }

    // ── Header ───────────────────────────────────────────────────────────────
    Column(Modifier.fillMaxSize().background(BgColor)) {
        Row(
            Modifier.fillMaxWidth().height(35.dp).background(Color(0xFFF3F3F3))
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("EXPLORER", fontSize = 11.sp, color = MutedColor,
                fontFamily = FontFamily.Default, modifier = Modifier.weight(1f))
            Icon(Icons.Default.CreateNewFolder, null, tint = MutedColor,
                modifier = Modifier.size(16.dp).clickable {
                    contextFile = root; showNewFolder = true; nameInput = ""
                })
            Spacer(Modifier.width(8.dp))
            Icon(Icons.Default.Add, null, tint = MutedColor,
                modifier = Modifier.size(16.dp).clickable {
                    contextFile = root; showNewFile = true; nameInput = ""
                })
            Spacer(Modifier.width(8.dp))
            Icon(Icons.Default.Refresh, null, tint = MutedColor,
                modifier = Modifier.size(16.dp).clickable { refresh++ })
            Spacer(Modifier.width(8.dp))
            Icon(Icons.Default.MoreVert, null, tint = MutedColor,
                modifier = Modifier.size(16.dp).clickable { onMoreMenu() })
        }
        HorizontalDivider(color = DividerColor)

        // ── File tree ─────────────────────────────────────────────────────
        LazyColumn(Modifier.fillMaxSize()) {
            items(nodes, key = { it.file.absolutePath }) { node ->
                val isSelected = selected == node.file.absolutePath
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(
                            when {
                                isSelected -> SelectedBg
                                else       -> BgColor
                            }
                        )
                        .clickable {
                            selected = node.file.absolutePath
                            if (node.file.isDirectory) {
                                expanded[node.file.absolutePath] =
                                    !(expanded[node.file.absolutePath] ?: false)
                                refresh++
                            } else {
                                onOpenFile(node.file.absolutePath)
                            }
                        }
                        .padding(start = (8 + node.depth * 12).dp, top = 4.dp, bottom = 4.dp, end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (node.file.isDirectory) {
                        Icon(
                            if (node.isExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.ChevronRight,
                            null, tint = MutedColor, modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(2.dp))
                        Icon(Icons.Default.Description, null, tint = FolderColor, modifier = Modifier.size(16.dp))
                    } else {
                        Spacer(Modifier.width(18.dp))
                        Icon(fileIcon(node.file.name), null, tint = IconColor, modifier = Modifier.size(16.dp))
                    }
                    Spacer(Modifier.width(4.dp))
                    Text(
                        node.file.name,
                        fontSize = 13.sp, color = TextColor, maxLines = 1,
                        overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }

    // ── Context menu ─────────────────────────────────────────────────────────
    if (showCtxMenu && contextFile != null) {
        val f = contextFile!!
        AlertDialog(
            onDismissRequest = { showCtxMenu = false },
            title = { Text(f.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            text = {
                Column {
                    listOf(
                        "Open"          to Icons.Default.OpenInNew,
                        "Rename"        to Icons.Default.Edit,
                        "Delete"        to Icons.Default.Delete,
                        "Copy Path"     to Icons.Default.ContentCopy,
                        "New File Here" to Icons.Default.Add,
                        "New Folder Here" to Icons.Default.CreateNewFolder,
                    ).forEach { (label, icon) ->
                        Row(
                            Modifier.fillMaxWidth().clickable {
                                showCtxMenu = false
                                when (label) {
                                    "Open"   -> if (!f.isDirectory) onOpenFile(f.absolutePath)
                                               else { expanded[f.absolutePath] = true; refresh++ }
                                    "Rename" -> { nameInput = f.name; showRename = true }
                                    "Delete" -> showDelete = true
                                    "New File Here" -> { nameInput = ""; showNewFile = true }
                                    "New Folder Here" -> { nameInput = ""; showNewFolder = true }
                                }
                            }.padding(vertical = 10.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(icon, null, tint = MutedColor, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(12.dp))
                            Text(label, fontSize = 14.sp)
                        }
                    }
                }
            },
            confirmButton = {},
        )
    }

    // New File dialog
    if (showNewFile) {
        AlertDialog(
            onDismissRequest = { showNewFile = false },
            title            = { Text("New File") },
            text             = {
                OutlinedTextField(
                    value = nameInput, onValueChange = { nameInput = it },
                    label = { Text("File name") }, singleLine = true,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (nameInput.isNotBlank()) {
                        val dir = contextFile?.let { if (it.isDirectory) it else it.parentFile } ?: root
                        File(dir, nameInput).createNewFile()
                        refresh++
                    }
                    showNewFile = false; nameInput = ""
                }) { Text("Create") }
            },
            dismissButton = { TextButton(onClick = { showNewFile = false }) { Text("Cancel") } },
        )
    }

    // New Folder dialog
    if (showNewFolder) {
        AlertDialog(
            onDismissRequest = { showNewFolder = false },
            title            = { Text("New Folder") },
            text             = {
                OutlinedTextField(
                    value = nameInput, onValueChange = { nameInput = it },
                    label = { Text("Folder name") }, singleLine = true,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (nameInput.isNotBlank()) {
                        val dir = contextFile?.let { if (it.isDirectory) it else it.parentFile } ?: root
                        File(dir, nameInput).mkdirs()
                        refresh++
                    }
                    showNewFolder = false; nameInput = ""
                }) { Text("Create") }
            },
            dismissButton = { TextButton(onClick = { showNewFolder = false }) { Text("Cancel") } },
        )
    }

    // Rename dialog
    if (showRename && contextFile != null) {
        AlertDialog(
            onDismissRequest = { showRename = false },
            title            = { Text("Rename") },
            text             = {
                OutlinedTextField(
                    value = nameInput, onValueChange = { nameInput = it },
                    label = { Text("New name") }, singleLine = true,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (nameInput.isNotBlank()) {
                        contextFile!!.renameTo(File(contextFile!!.parent, nameInput))
                        refresh++
                    }
                    showRename = false; nameInput = ""
                }) { Text("Rename") }
            },
            dismissButton = { TextButton(onClick = { showRename = false }) { Text("Cancel") } },
        )
    }

    // Delete confirmation
    if (showDelete && contextFile != null) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title   = { Text("Delete ${contextFile!!.name}?") },
            text    = { Text("This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    contextFile!!.deleteRecursively()
                    refresh++
                    showDelete = false
                }) { Text("Delete", color = Color.Red) }
            },
            dismissButton = { TextButton(onClick = { showDelete = false }) { Text("Cancel") } },
        )
    }
}

private fun fileIcon(name: String) = when {
    name.endsWith(".kt")   -> Icons.Default.Code
    name.endsWith(".java") -> Icons.Default.Code
    name.endsWith(".py")   -> Icons.Default.Code
    name.endsWith(".js") || name.endsWith(".ts") -> Icons.Default.Code
    name.endsWith(".html") || name.endsWith(".xml") -> Icons.Default.Code
    name.endsWith(".json") -> Icons.Default.Code
    name.endsWith(".md")   -> Icons.Default.Article
    name.endsWith(".gradle") || name.endsWith(".kts") -> Icons.Default.Build
    name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".svg") -> Icons.Default.Image
    name.endsWith(".zip") || name.endsWith(".apk") -> Icons.Default.FolderZip
    name.endsWith(".sh")   -> Icons.Default.Computer
    else                   -> Icons.Default.Article
}

// ── Stub panels kept for compilation ────────────────────────────────────────
@Composable fun SearchPanel() {
    var searchQuery by remember { mutableStateOf("") }
    var replaceQuery by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(8.dp)) {
        OutlinedTextField(value = searchQuery, onValueChange = { searchQuery = it }, label = { Text("Search") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = replaceQuery, onValueChange = { replaceQuery = it }, label = { Text("Replace") }, singleLine = true, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
        Row(Modifier.padding(top = 8.dp)) {
            listOf("Aa", "\\b", ".*").forEach { opt ->
                Box(Modifier.border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(3.dp))
                    .padding(horizontal = 6.dp, vertical = 4.dp)) {
                    Text(opt, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }
                Spacer(Modifier.width(4.dp))
            }
        }
        Text("No results", fontSize = 13.sp, color = Color(0xFF717171), modifier = Modifier.padding(top = 16.dp))
    }
}

@Composable fun GitSidePanel()      { SourceControlPane() }
@Composable fun RunDebugPanel(onMoreMenu: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("RUN AND DEBUG", fontSize = 11.sp, color = Color(0xFF717171))
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {}) { Text("▶ Run") }
            OutlinedButton(onClick = {}) { Text("⏹ Stop") }
        }
        Spacer(Modifier.height(16.dp))
        Text("VARIABLES", fontSize = 11.sp, color = Color(0xFF717171))
        Text("No active debug session.", fontSize = 13.sp, color = Color(0xFFAAAAAA), modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable fun ExtensionsPanel() {
    var extQuery by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(8.dp)) {
        OutlinedTextField(value = extQuery, onValueChange = { extQuery = it }, label = { Text("Search Extensions") },
            singleLine = true, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        Text("INSTALLED", fontSize = 11.sp, color = Color(0xFF717171))
        listOf("Kotlin", "Git Lens", "Prettier", "ESLint").forEach { ext ->
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Extension, null, tint = Color(0xFF007ACC), modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(ext, fontSize = 13.sp, modifier = Modifier.weight(1f))
                Text("✓", fontSize = 12.sp, color = Color(0xFF4CAF50))
            }
            HorizontalDivider(color = Color(0xFFEEEEEE))
        }
    }
}

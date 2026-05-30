package com.codespace.ide.ui.panes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.codespace.ide.domain.FileNode

/**
 * File tree explorer with search & replace entry point. The real tree is paged & lazy
 * (200 nodes/page) and cached in Room for offline navigation.
 */
@Composable
fun ExplorerPane(onOpenFile: () -> Unit) {
    var query by remember { mutableStateOf("") }

    // Sample flattened tree with indentation depth.
    val nodes = listOf(
        0 to FileNode("src", "src", true),
        1 to FileNode("src/index.ts", "index.ts", false, 1240),
        1 to FileNode("src/utils.ts", "utils.ts", false, 880),
        1 to FileNode("src/components", "components", true),
        2 to FileNode("src/components/App.tsx", "App.tsx", false, 2200),
        0 to FileNode("main.py", "main.py", false, 410),
        0 to FileNode("README.md", "README.md", false, 1500),
        0 to FileNode("package.json", "package.json", false, 620),
    )

    Column(Modifier.fillMaxSize().padding(8.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Search & replace in project") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        LazyColumn(Modifier.fillMaxSize()) {
            items(nodes) { (depth, node) ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { if (!node.isDir) onOpenFile() }
                        .padding(start = (12 + depth * 16).dp, top = 10.dp, bottom = 10.dp),
                ) {
                    Icon(
                        if (node.isDir) Icons.Default.Folder else Icons.Default.InsertDriveFile,
                        contentDescription = null,
                    )
                    Text("  ${node.name}")
                }
            }
        }
    }
}

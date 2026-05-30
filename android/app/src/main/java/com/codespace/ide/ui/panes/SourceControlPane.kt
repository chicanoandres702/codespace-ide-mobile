package com.codespace.ide.ui.panes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Source control pane: branch, change list, commit box, and push/pull/PR actions.
 * Wires to [com.codespace.ide.git.GitEngine] for on-device Git and the backend
 * GithubModule for pull requests.
 */
@Composable
fun SourceControlPane() {
    var message by remember { mutableStateOf("") }
    val changes = listOf(
        "M  src/index.ts",
        "M  src/utils.ts",
        "A  src/components/App.tsx",
        "?? notes.md",
    )

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Branch: main", style = MaterialTheme.typography.titleMedium)
        Text("↑0 ↓0 • 4 changes", style = MaterialTheme.typography.bodySmall)
        HorizontalDivider(Modifier.padding(vertical = 8.dp))

        OutlinedTextField(
            value = message,
            onValueChange = { message = it },
            label = { Text("Commit message") },
            modifier = Modifier.fillMaxWidth(),
        )
        Row(
            Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(onClick = { }, modifier = Modifier.weight(1f)) { Text("Commit") }
            OutlinedButton(onClick = { }, modifier = Modifier.weight(1f)) { Text("Push") }
            OutlinedButton(onClick = { }, modifier = Modifier.weight(1f)) { Text("Pull") }
        }
        OutlinedButton(onClick = { }, modifier = Modifier.fillMaxWidth()) {
            Text("Create Pull Request")
        }
        HorizontalDivider(Modifier.padding(vertical = 8.dp))
        Text("Changes", style = MaterialTheme.typography.titleSmall)
        LazyColumn(Modifier.fillMaxSize()) {
            items(changes) { change ->
                Text(change, Modifier.fillMaxWidth().padding(vertical = 6.dp))
            }
        }
    }
}

package com.codespace.ide.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.codespace.ide.domain.Project
import com.codespace.ide.domain.ProjectKind

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenProject: (String) -> Unit,
    onOpenSettings: () -> Unit,
) {
    // Sample recent projects; real data comes from the ProjectsRepository (Room + API).
    val recents = listOf(
        Project("1", "my-api", ProjectKind.GIT, "github.com/me/my-api", "main", 0),
        Project("2", "portfolio", ProjectKind.GIT, "github.com/me/portfolio", "main", 0),
        Project("3", "prod-server", ProjectKind.SSH, "ssh://root@1.2.3.4", null, 0),
        Project("4", "ml-codespace", ProjectKind.CODESPACE, "codespaces/ml", null, 0),
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Workspaces") },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onOpenProject("new") },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("New project") },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
        ) {
            items(recents) { project ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .clickable { onOpenProject(project.id) },
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row {
                            Icon(Icons.Default.Folder, contentDescription = null)
                            Text(
                                "  ${project.name}",
                                style = MaterialTheme.typography.titleMedium,
                            )
                        }
                        Text(
                            "${project.kind.name.lowercase()} • ${project.pathOrUrl}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        )
                    }
                }
            }
        }
    }
}

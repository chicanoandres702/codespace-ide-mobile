package com.codespace.ide.ui.screens

import android.content.Context
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.codespace.ide.domain.Project
import com.codespace.ide.domain.ProjectKind
import org.json.JSONArray
import org.json.JSONObject

private fun saveProjects(context: Context, projects: List<Project>) {
    val arr = JSONArray()
    projects.forEach {
        arr.put(JSONObject()
            .put("id", it.id)
            .put("name", it.name)
            .put("kind", it.kind.name)
            .put("pathOrUrl", it.pathOrUrl)
            .put("defaultBranch", it.defaultBranch ?: "")
        )
    }
    context.getSharedPreferences("projects", Context.MODE_PRIVATE)
        .edit().putString("list", arr.toString()).apply()
}

private fun loadProjects(context: Context): List<Project> {
    val str = context.getSharedPreferences("projects", Context.MODE_PRIVATE)
        .getString("list", null) ?: return emptyList()
    return try {
        val arr = JSONArray(str)
        (0 until arr.length()).map {
            val obj = arr.getJSONObject(it)
            Project(
                id = obj.getString("id"),
                name = obj.getString("name"),
                kind = ProjectKind.valueOf(obj.getString("kind")),
                pathOrUrl = obj.getString("pathOrUrl"),
                defaultBranch = obj.getString("defaultBranch").ifBlank { null },
            )
        }
    } catch (e: Exception) { emptyList() }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenProject: (String) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val context = LocalContext.current
    val projects = remember {
        mutableStateListOf<Project>().apply { addAll(loadProjects(context)) }
    }
    var showAddDialog by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var newPath by remember { mutableStateOf("") }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("New Project") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Project name") },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = newPath,
                        onValueChange = { newPath = it },
                        label = { Text("Path or GitHub URL") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (newName.isNotBlank()) {
                        val project = Project(
                            id = System.currentTimeMillis().toString(),
                            name = newName,
                            kind = if (newPath.contains("github")) ProjectKind.GIT else ProjectKind.LOCAL,
                            pathOrUrl = newPath.ifBlank { "local" },
                            defaultBranch = "main",
                        )
                        projects.add(project)
                        saveProjects(context, projects)
                        newName = ""
                        newPath = ""
                        showAddDialog = false
                    }
                }) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("Cancel") }
            }
        )
    }

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
                onClick = { showAddDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("New project") },
            )
        },
    ) { padding ->
        if (projects.isEmpty()) {
            Column(
                Modifier.fillMaxSize().padding(padding).padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("No projects yet", style = MaterialTheme.typography.titleMedium)
                Text("Tap 'New project' to create one", style = MaterialTheme.typography.bodySmall)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            ) {
                items(projects) { project ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .clickable { onOpenProject(project.id) },
                    ) {
                        Row(
                            Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Default.Description, contentDescription = null)
                            Column(Modifier.weight(1f).padding(start = 8.dp)) {
                                Text(project.name, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "${project.kind.name.lowercase()} • ${project.pathOrUrl}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                )
                            }
                            IconButton(onClick = {
                                projects.remove(project)
                                saveProjects(context, projects)
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete")
                            }
                        }
                    }
                }
            }
        }
    }
}

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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

private fun runGit(dir: File, vararg args: String): String {
    return try {
        val gitBin = if (File("/data/data/com.termux/files/usr/bin/git").exists())
            "/data/data/com.termux/files/usr/bin/git" else "git"
        val pb = ProcessBuilder(gitBin, *args)
            .directory(dir)
            .redirectErrorStream(true)
        pb.environment().apply {
            val prefix = "/data/data/com.termux/files/usr"
            put("PREFIX", prefix)
            put("HOME", "/data/data/com.termux/files/home")
            put("PATH", "$prefix/bin:/system/bin:/system/xbin")
            put("LD_LIBRARY_PATH", "$prefix/lib")
        }
        val process = pb.start()
        val reader = BufferedReader(InputStreamReader(process.inputStream))
        val output = reader.readText()
        process.waitFor()
        output.trim()
    } catch (e: Exception) {
        "Error: ${e.message}"
    }
}

@Composable
fun SourceControlPane() {
    var message by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("Tap 'Refresh' to load git status") }
    var branch by remember { mutableStateOf("unknown") }
    val changes = remember { mutableStateListOf<String>() }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    // Find the git repo starting from storage root
    val repoDir = remember {
        val candidates = listOf(
            File("/storage/emulated/0/codespace-ide-mobile"),
            File("/storage/emulated/0"),
        )
        candidates.firstOrNull { File(it, ".git").exists() }
            ?: File("/storage/emulated/0")
    }

    fun refresh() {
        scope.launch {
            loading = true
            val branchResult = withContext(Dispatchers.IO) {
                runGit(repoDir, "branch", "--show-current")
            }
            val statusResult = withContext(Dispatchers.IO) {
                runGit(repoDir, "status", "--short")
            }
            branch = branchResult.ifBlank { "unknown" }
            changes.clear()
            if (statusResult.isNotBlank()) {
                changes.addAll(statusResult.lines().filter { it.isNotBlank() })
                status = "${changes.size} changes"
            } else {
                status = "Nothing to commit"
            }
            loading = false
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text("Branch: $branch", style = MaterialTheme.typography.titleMedium)
                Text(status, style = MaterialTheme.typography.bodySmall)
            }
            OutlinedButton(onClick = { refresh() }) { Text("Refresh") }
        }

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
            Button(
                onClick = {
                    if (message.isNotBlank()) {
                        scope.launch {
                            withContext(Dispatchers.IO) {
                                runGit(repoDir, "add", ".")
                                runGit(repoDir, "commit", "-m", message)
                            }
                            message = ""
                            refresh()
                        }
                    }
                },
                modifier = Modifier.weight(1f),
            ) { Text("Commit") }

            OutlinedButton(
                onClick = {
                    scope.launch {
                        withContext(Dispatchers.IO) {
                            runGit(repoDir, "push")
                        }
                        refresh()
                    }
                },
                modifier = Modifier.weight(1f),
            ) { Text("Push") }

            OutlinedButton(
                onClick = {
                    scope.launch {
                        withContext(Dispatchers.IO) {
                            runGit(repoDir, "pull")
                        }
                        refresh()
                    }
                },
                modifier = Modifier.weight(1f),
            ) { Text("Pull") }
        }

        HorizontalDivider(Modifier.padding(vertical = 8.dp))
        Text("Changes", style = MaterialTheme.typography.titleSmall)

        if (changes.isEmpty()) {
            Text("No changes", Modifier.padding(8.dp))
        }

        LazyColumn(Modifier.fillMaxSize()) {
            items(changes) { change ->
                Text(change, Modifier.fillMaxWidth().padding(vertical = 6.dp))
                HorizontalDivider()
            }
        }
    }
}

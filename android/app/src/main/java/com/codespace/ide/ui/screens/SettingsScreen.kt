package com.codespace.ide.ui.screens

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.codespace.ide.data.SecureTokenStore
import com.codespace.ide.domain.AiProviderId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    isDark: Boolean,
    onToggleTheme: () -> Unit,
    onBack: () -> Unit,
    tokenStore: SecureTokenStore,
) {
    val context = LocalContext.current
    val keyMap = remember {
        mutableStateMapOf<AiProviderId, String>().apply {
            AiProviderId.entries.forEach { provider ->
                put(provider, tokenStore.aiKey(provider.name) ?: "")
            }
        }
    }
    val visibleMap = remember {
        mutableStateMapOf<AiProviderId, Boolean>().apply {
            AiProviderId.entries.forEach { put(it, false) }
        }
    }
    var activeProvider by remember {
        mutableStateOf(
            AiProviderId.entries.firstOrNull {
                tokenStore.aiKey(it.name) != null
            } ?: AiProviderId.CLAUDE
        )
    }
    var savedMsg by remember { mutableStateOf("") }
    var showClearDialog by remember { mutableStateOf<String?>(null) }

    if (showClearDialog != null) {
        AlertDialog(
            onDismissRequest = { showClearDialog = null },
            title = { Text("Clear ${showClearDialog}?") },
            text = { Text("This cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        when (showClearDialog) {
                            "Terminal History" -> context.getSharedPreferences("terminal_history", Context.MODE_PRIVATE).edit().clear().apply()
                            "AI Chat History" -> context.getSharedPreferences("ai_chat_history", Context.MODE_PRIVATE).edit().clear().apply()
                            "Projects" -> context.getSharedPreferences("projects", Context.MODE_PRIVATE).edit().clear().apply()
                            "All Data" -> {
                                context.getSharedPreferences("terminal_history", Context.MODE_PRIVATE).edit().clear().apply()
                                context.getSharedPreferences("ai_chat_history", Context.MODE_PRIVATE).edit().clear().apply()
                                context.getSharedPreferences("projects", Context.MODE_PRIVATE).edit().clear().apply()
                            }
                        }
                        showClearDialog = null
                        savedMsg = "✓ Cleared!"
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) { Text("Clear") }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = null }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // Theme
            ListItem(
                headlineContent = { Text("Dark mode") },
                trailingContent = {
                    Switch(checked = isDark, onCheckedChange = { onToggleTheme() })
                },
            )
            HorizontalDivider()

            // AI Providers
            Text("AI Providers", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(16.dp))
            AiProviderId.entries.forEach { provider ->
                val key = keyMap[provider] ?: ""
                val visible = visibleMap[provider] ?: false
                val isActive = activeProvider == provider
                Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                    ListItem(
                        headlineContent = { Text(provider.displayName) },
                        supportingContent = { Text(if (isActive) "✓ Active" else "Tap switch to activate") },
                        trailingContent = {
                            Switch(checked = isActive, onCheckedChange = { if (it) activeProvider = provider })
                        },
                    )
                    OutlinedTextField(
                        value = key,
                        onValueChange = { keyMap[provider] = it },
                        label = {
                            Text(if (provider == AiProviderId.OLLAMA) "Base URL e.g. http://192.168.1.x:11434"
                            else "${provider.displayName} API Key")
                        },
                        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { visibleMap[provider] = !visible }) {
                                Icon(if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility, contentDescription = null)
                            }
                        },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        singleLine = true,
                    )
                }
                HorizontalDivider()
            }

            Button(
                onClick = {
                    AiProviderId.entries.forEach { provider ->
                        val key = keyMap[provider] ?: ""
                        tokenStore.setAiKey(provider.name, key.ifBlank { null })
                    }
                    tokenStore.setAiKey("active", activeProvider.name)
                    savedMsg = "✓ Saved!"
                },
                modifier = Modifier.fillMaxWidth().padding(16.dp),
            ) { Text("Save API Keys") }

            if (savedMsg.isNotEmpty()) {
                Text(savedMsg, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 16.dp))
            }

            HorizontalDivider()

            // Clear Data section
            Text("Clear Data", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(16.dp))

            listOf("Terminal History", "AI Chat History", "Projects").forEach { item ->
                ListItem(
                    headlineContent = { Text(item) },
                    trailingContent = {
                        OutlinedButton(
                            onClick = { showClearDialog = item },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                        ) { Text("Clear") }
                    }
                )
                HorizontalDivider()
            }

            OutlinedButton(
                onClick = { showClearDialog = "All Data" },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                modifier = Modifier.fillMaxWidth().padding(16.dp),
            ) { Text("Clear All Data") }
        }
    }
}

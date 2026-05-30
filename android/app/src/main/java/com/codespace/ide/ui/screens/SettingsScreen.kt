package com.codespace.ide.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.codespace.ide.domain.AiProviderId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    isDark: Boolean,
    onToggleTheme: () -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            ListItem(
                headlineContent = { Text("Dark mode") },
                trailingContent = { Switch(checked = isDark, onCheckedChange = { onToggleTheme() }) },
            )
            HorizontalDivider()
            Text(
                "AI Providers",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(16.dp),
            )
            AiProviderId.entries.forEach { provider ->
                ListItem(
                    headlineContent = { Text(provider.displayName) },
                    supportingContent = {
                        Text(
                            when (provider) {
                                AiProviderId.OLLAMA -> "Local — set base URL"
                                else -> "Bring your own API key (stored in Keystore)"
                            }
                        )
                    },
                )
            }
            HorizontalDivider()
            ListItem(headlineContent = { Text("Connected accounts") }, supportingContent = { Text("GitHub") })
            ListItem(headlineContent = { Text("Editor settings") }, supportingContent = { Text("Font size, tab width, format on save") })
            ListItem(headlineContent = { Text("Plugins") }, supportingContent = { Text("Marketplace & installed extensions") })
            ListItem(headlineContent = { Text("Backups & sync") }, supportingContent = { Text("Background sync, automatic backups") })
        }
    }
}

package com.codespace.ide.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AuthScreen(onAuthenticated: () -> Unit) {
    val context = LocalContext.current
    var token by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "CodeSpace IDE",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            "Mobile IDE for Android",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(48.dp))

        // GitHub token input
        OutlinedTextField(
            value = token,
            onValueChange = { token = it; error = "" },
            label = { Text("GitHub Personal Access Token") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = error.isNotEmpty(),
            supportingText = if (error.isNotEmpty()) {
                { Text(error, color = MaterialTheme.colorScheme.error) }
            } else null,
        )

        Spacer(Modifier.height(8.dp))

        // Sign in with token
        Button(
            onClick = {
                if (token.isBlank()) {
                    error = "Please enter your GitHub token"
                } else {
                    loading = true
                    onAuthenticated()
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !loading,
        ) {
            if (loading) CircularProgressIndicator()
            else Text("Sign In")
        }

        Spacer(Modifier.height(16.dp))

        // Get token button
        OutlinedButton(
            onClick = {
                val intent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://github.com/settings/tokens/new?scopes=repo,read:user&description=CodeSpaceIDE")
                )
                context.startActivity(intent)
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Get GitHub Token")
        }

        Spacer(Modifier.height(24.dp))

        Text(
            "Tap 'Get GitHub Token' to open GitHub in your browser.\nCreate a token with 'repo' and 'read:user' scopes, then paste it above.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

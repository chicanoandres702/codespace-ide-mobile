package com.codespace.ide.ui.panes

import android.content.Context
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.codespace.ide.data.SecureTokenStore
import com.codespace.ide.domain.AiAction
import com.codespace.ide.domain.ChatMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

private const val PREFS_NAME = "ai_chat_history"
private const val KEY_HISTORY = "chat_history"

private fun saveHistory(context: Context, messages: List<ChatMessage>) {
    val arr = JSONArray()
    messages.forEach { arr.put(JSONObject().put("role", it.role).put("content", it.content)) }
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit().putString(KEY_HISTORY, arr.toString()).apply()
}

private fun loadHistory(context: Context): List<ChatMessage> {
    val str = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .getString(KEY_HISTORY, null) ?: return emptyList()
    return try {
        val arr = JSONArray(str)
        (0 until arr.length()).map {
            val obj = arr.getJSONObject(it)
            ChatMessage(obj.getString("role"), obj.getString("content"))
        }
    } catch (e: Exception) { emptyList() }
}

@Composable
fun AiAssistantPane(tokenStore: SecureTokenStore) {
    val context = LocalContext.current
    var input by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    val messages = remember {
        val saved = loadHistory(context)
        mutableStateListOf<ChatMessage>().apply {
            if (saved.isEmpty()) {
                add(ChatMessage("assistant", "Hi! Ask me anything about your code!"))
            } else {
                addAll(saved)
            }
        }
    }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    suspend fun sendMessage(userMessage: String) {
        if (userMessage.isBlank()) return
        messages.add(ChatMessage("user", userMessage))
        loading = true
        input = ""

        try {
            val providerList = listOf("OPENROUTER","OPENAI","CLAUDE","GEMINI","DEEPSEEK","OLLAMA")
            var apiKey = ""
            var activeProvider = "OPENROUTER"
            for (p in providerList) {
                val k = tokenStore.aiKey(p)
                if (!k.isNullOrBlank()) {
                    apiKey = k
                    activeProvider = p
                    break
                }
            }

            if (apiKey.isBlank()) {
                messages.add(ChatMessage("assistant", "⚠️ No API key found. Go to Settings → enter your OpenRouter key → tap Save API Keys."))
                saveHistory(context, messages)
                loading = false
                return
            }

            val model = when (activeProvider) {
                "CLAUDE" -> "claude-haiku-4-5-20251001"
                "OPENAI" -> "gpt-4o-mini"
                "GEMINI" -> "gemini-1.5-flash"
                "DEEPSEEK" -> "deepseek-chat"
                else -> "meta-llama/llama-3.1-8b-instruct:free"
            }

            val messagesJson = JSONArray()
            messages.filter { it.role == "user" || messages.indexOf(it) > 0 }.forEach { m ->
                messagesJson.put(JSONObject().put("role", m.role).put("content", m.content))
            }

            val body = JSONObject()
                .put("model", model)
                .put("messages", messagesJson)
                .toString()

            val reqBuilder = Request.Builder()
                .header("Content-Type", "application/json")

            when (activeProvider) {
                "CLAUDE" -> {
                    reqBuilder.url("https://api.anthropic.com/v1/messages")
                    reqBuilder.header("x-api-key", apiKey)
                    reqBuilder.header("anthropic-version", "2023-06-01")
                }
                "GEMINI" -> {
                    reqBuilder.url("https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey")
                }
                else -> {
                    val url = if (activeProvider == "OPENROUTER")
                        "https://openrouter.ai/api/v1/chat/completions"
                    else "https://api.openai.com/v1/chat/completions"
                    reqBuilder.url(url)
                    reqBuilder.header("Authorization", "Bearer $apiKey")
                    if (activeProvider == "OPENROUTER")
                        reqBuilder.header("HTTP-Referer", "https://codespace-ide.app")
                }
            }

            reqBuilder.post(body.toRequestBody("application/json".toMediaType()))

            val response = withContext(Dispatchers.IO) {
                OkHttpClient().newCall(reqBuilder.build()).execute()
            }

            val responseBody = response.body?.string() ?: ""
            val json = JSONObject(responseBody)

            val content = when (activeProvider) {
                "CLAUDE" -> json.getJSONArray("content").getJSONObject(0).getString("text")
                "GEMINI" -> json.getJSONArray("candidates")
                    .getJSONObject(0).getJSONObject("content")
                    .getJSONArray("parts").getJSONObject(0).getString("text")
                else -> json.getJSONArray("choices")
                    .getJSONObject(0).getJSONObject("message").getString("content")
            }

            messages.add(ChatMessage("assistant", content))
        } catch (e: Exception) {
            messages.add(ChatMessage("assistant", "❌ Error: ${e.message}"))
        } finally {
            saveHistory(context, messages)
            loading = false
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AiAction.entries.forEach { action ->
                AssistChip(
                    onClick = {
                        scope.launch {
                            sendMessage(action.name.lowercase().replaceFirstChar { it.uppercase() } + " the selected code")
                        }
                    },
                    label = { Text(action.name.lowercase().replaceFirstChar { it.uppercase() }) },
                )
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(messages) { msg ->
                val isUser = msg.role == "user"
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isUser) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant,
                    ) {
                        Text(
                            msg.content,
                            Modifier.padding(12.dp),
                            color = if (isUser) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
            if (loading) {
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                        CircularProgressIndicator(Modifier.padding(8.dp))
                    }
                }
            }
        }

        Row(
            Modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Ask about your project…") },
                enabled = !loading,
            )
            IconButton(
                onClick = { scope.launch { sendMessage(input) } },
                enabled = !loading && input.isNotBlank(),
            ) {
                Icon(Icons.Default.Send, contentDescription = "Send")
            }
        }
    }
}

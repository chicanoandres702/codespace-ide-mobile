package com.codespace.ide.ai

import com.codespace.ide.domain.AiChunk
import com.codespace.ide.domain.AiContext
import com.codespace.ide.domain.AiProviderId
import com.codespace.ide.domain.ChatMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

data class ProviderConfig(val apiKey: String, val baseUrl: String = "")

private fun openAiStyleChat(
    messages: List<ChatMessage>,
    baseUrl: String,
    model: String,
    apiKey: String,
    client: OkHttpClient,
    extraHeaders: Map<String, String> = emptyMap(),
): Flow<AiChunk> = flow {
    val messagesJson = JSONArray().apply {
        messages.forEach { m ->
            put(JSONObject().apply {
                put("role", m.role)
                put("content", m.content)
            })
        }
    }
    val body = JSONObject().apply {
        put("model", model)
        put("messages", messagesJson)
    }.toString()

    val reqBuilder = Request.Builder()
        .url(baseUrl)
        .header("Authorization", "Bearer $apiKey")
        .header("Content-Type", "application/json")
    extraHeaders.forEach { (k, v) -> reqBuilder.header(k, v) }
    reqBuilder.post(body.toRequestBody("application/json".toMediaType()))

    try {
        val response = client.newCall(reqBuilder.build()).execute()
        if (!response.isSuccessful) {
            emit(AiChunk.Error("HTTP ${response.code}: ${response.message}"))
            return@flow
        }
        val text = response.body?.string() ?: ""
        val json = JSONObject(text)
        val content = json.getJSONArray("choices")
            .getJSONObject(0)
            .getJSONObject("message")
            .getString("content")
        emit(AiChunk.Token(content))
        emit(AiChunk.Done(0, 0))
    } catch (e: Exception) {
        emit(AiChunk.Error(e.message ?: "Unknown error"))
    }
}

class OpenAiProvider(private val config: ProviderConfig, private val client: OkHttpClient) : AiProvider {
    override val id = AiProviderId.OPENAI
    override val models = listOf("gpt-4o", "gpt-4o-mini", "gpt-3.5-turbo")
    override fun chat(model: String, messages: List<ChatMessage>, context: AiContext) =
        openAiStyleChat(messages, "https://api.openai.com/v1/chat/completions", model, config.apiKey, client)
}

class ClaudeProvider(private val config: ProviderConfig, private val client: OkHttpClient) : AiProvider {
    override val id = AiProviderId.CLAUDE
    override val models = listOf("claude-sonnet-4-6", "claude-haiku-4-5-20251001")
    override fun chat(model: String, messages: List<ChatMessage>, context: AiContext): Flow<AiChunk> = flow {
        val messagesJson = JSONArray().apply {
            messages.forEach { m ->
                put(JSONObject().apply {
                    put("role", m.role)
                    put("content", m.content)
                })
            }
        }
        val body = JSONObject().apply {
            put("model", model)
            put("max_tokens", 1024)
            put("messages", messagesJson)
        }.toString()
        try {
            val request = Request.Builder()
                .url("https://api.anthropic.com/v1/messages")
                .header("x-api-key", config.apiKey)
                .header("anthropic-version", "2023-06-01")
                .header("Content-Type", "application/json")
                .post(body.toRequestBody("application/json".toMediaType()))
                .build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                emit(AiChunk.Error("HTTP ${response.code}"))
                return@flow
            }
            val text = response.body?.string() ?: ""
            val json = JSONObject(text)
            val content = json.getJSONArray("content")
                .getJSONObject(0)
                .getString("text")
            emit(AiChunk.Token(content))
            emit(AiChunk.Done(0, 0))
        } catch (e: Exception) {
            emit(AiChunk.Error(e.message ?: "Unknown error"))
        }
    }
}

class GeminiProvider(private val config: ProviderConfig, private val client: OkHttpClient) : AiProvider {
    override val id = AiProviderId.GEMINI
    override val models = listOf("gemini-1.5-flash", "gemini-1.5-pro")
    override fun chat(model: String, messages: List<ChatMessage>, context: AiContext): Flow<AiChunk> = flow {
        val contentsJson = JSONArray().apply {
            messages.forEach { m ->
                put(JSONObject().apply {
                    put("role", if (m.role == "assistant") "model" else "user")
                    put("parts", JSONArray().put(JSONObject().put("text", m.content)))
                })
            }
        }
        val body = JSONObject().put("contents", contentsJson).toString()
        try {
            val request = Request.Builder()
                .url("https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=${config.apiKey}")
                .header("Content-Type", "application/json")
                .post(body.toRequestBody("application/json".toMediaType()))
                .build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                emit(AiChunk.Error("HTTP ${response.code}"))
                return@flow
            }
            val text = response.body?.string() ?: ""
            val json = JSONObject(text)
            val content = json.getJSONArray("candidates")
                .getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .getString("text")
            emit(AiChunk.Token(content))
            emit(AiChunk.Done(0, 0))
        } catch (e: Exception) {
            emit(AiChunk.Error(e.message ?: "Unknown error"))
        }
    }
}

class DeepSeekProvider(private val config: ProviderConfig, private val client: OkHttpClient) : AiProvider {
    override val id = AiProviderId.DEEPSEEK
    override val models = listOf("deepseek-chat", "deepseek-coder")
    override fun chat(model: String, messages: List<ChatMessage>, context: AiContext) =
        openAiStyleChat(messages, "https://api.deepseek.com/chat/completions", model, config.apiKey, client)
}

class OllamaProvider(private val config: ProviderConfig, private val client: OkHttpClient) : AiProvider {
    override val id = AiProviderId.OLLAMA
    override val models = listOf("llama3", "mistral", "codellama")
    override fun chat(model: String, messages: List<ChatMessage>, context: AiContext) =
        openAiStyleChat(messages, "${config.baseUrl}/api/chat", model, config.apiKey, client)
}

class OpenRouterProvider(private val config: ProviderConfig, private val client: OkHttpClient) : AiProvider {
    override val id = AiProviderId.OPENROUTER
    override val models = listOf(
        "anthropic/claude-sonnet-4-6",
        "openai/gpt-4o",
        "google/gemini-flash-1.5",
        "deepseek/deepseek-chat",
        "meta-llama/llama-3.1-8b-instruct:free",
    )
    override fun chat(model: String, messages: List<ChatMessage>, context: AiContext) =
        openAiStyleChat(
            messages,
            "https://openrouter.ai/api/v1/chat/completions",
            model,
            config.apiKey,
            client,
            mapOf("HTTP-Referer" to "https://codespace-ide.app"),
        )
}

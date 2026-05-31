package com.codespace.ide.ai

import com.codespace.ide.domain.AiChunk
import com.codespace.ide.domain.AiContext
import com.codespace.ide.domain.AiProviderId
import com.codespace.ide.domain.ChatMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

data class ProviderConfig(val apiKey: String, val baseUrl: String = "")

private fun openAiStyleChat(
    messages: List<ChatMessage>,
    baseUrl: String,
    model: String,
    apiKey: String,
    client: OkHttpClient,
    extraHeaders: Map<String, String> = emptyMap(),
): Flow<AiChunk> = flow {
    val body = buildString {
        append("{\"model\":\"$model\",\"messages\":[")
        messages.forEachIndexed { i, m ->
            if (i > 0) append(",")
            append("{\"role\":\"${m.role}\",\"content\":\"${m.content.replace("\"", "\\\"")}\"}")
        }
        append("]}")
    }
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
        val json = Json.parseToJsonElement(text).jsonObject
        val content = json["choices"]?.jsonArray
            ?.firstOrNull()?.jsonObject
            ?.get("message")?.jsonObject
            ?.get("content")?.jsonPrimitive?.content ?: ""
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
        val body = buildString {
            append("{\"model\":\"$model\",\"max_tokens\":1024,\"messages\":[")
            messages.forEachIndexed { i, m ->
                if (i > 0) append(",")
                append("{\"role\":\"${m.role}\",\"content\":\"${m.content.replace("\"", "\\\"")}\"}")
            }
            append("]}")
        }
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
            val json = Json.parseToJsonElement(text).jsonObject
            val content = json["content"]?.jsonArray
                ?.firstOrNull()?.jsonObject
                ?.get("text")?.jsonPrimitive?.content ?: ""
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
        val body = buildString {
            append("{\"contents\":[")
            messages.forEachIndexed { i, m ->
                if (i > 0) append(",")
                append("{\"role\":\"${if (m.role == "assistant") "model" else "user"}\",\"parts\":[{\"text\":\"${m.content.replace("\"", "\\\"")}\"}]}")
            }
            append("]}")
        }
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
            val json = Json.parseToJsonElement(text).jsonObject
            val content = json["candidates"]?.jsonArray
                ?.firstOrNull()?.jsonObject
                ?.get("content")?.jsonObject
                ?.get("parts")?.jsonArray
                ?.firstOrNull()?.jsonObject
                ?.get("text")?.jsonPrimitive?.content ?: ""
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

package com.codespace.ide.ai

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

/** Configuration for a provider instance (key from Keystore or backend proxy URL). */
data class ProviderConfig(
    val apiKey: String? = null,
    val baseUrl: String? = null,   // for Ollama / self-hosted / backend proxy
)

private val JSON = "application/json".toMediaType()

/**
 * OpenAI-compatible SSE streaming. DeepSeek and Ollama also speak this shape, so they
 * subclass with a different base URL. Claude and Gemini override [chat] with their own
 * wire formats.
 */
open class OpenAiCompatProvider(
    override val id: AiProviderId,
    private val config: ProviderConfig,
    private val defaultBase: String,
    override val models: List<String>,
    private val client: OkHttpClient = OkHttpClient(),
) : AiProvider {

    override fun chat(
        model: String,
        messages: List<ChatMessage>,
        context: AiContext,
    ): Flow<AiChunk> = flow {
        val base = config.baseUrl ?: defaultBase
        val body = JSONObject().apply {
            put("model", model)
            put("stream", true)
            val arr = JSONArray()
            arr.put(JSONObject().put("role", "system").put("content", PromptBuilder.systemPrompt(context)))
            messages.forEach { m ->
                arr.put(JSONObject().put("role", m.role).put("content", m.content))
            }
            put("messages", arr)
        }.toString()

        val req = Request.Builder()
            .url("$base/chat/completions")
            .apply { config.apiKey?.let { header("Authorization", "Bearer $it") } }
            .post(body.toRequestBody(JSON))
            .build()

        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) {
                emit(AiChunk.Error("HTTP ${resp.code}"))
                return@flow
            }
            val source = resp.body?.source() ?: return@flow
            while (!source.exhausted()) {
                val line = source.readUtf8Line() ?: break
                if (!line.startsWith("data:")) continue
                val data = line.removePrefix("data:").trim()
                if (data == "[DONE]") { emit(AiChunk.Done(0, 0)); break }
                runCatching {
                    val delta = JSONObject(data)
                        .getJSONArray("choices").getJSONObject(0)
                        .optJSONObject("delta")?.optString("content").orEmpty()
                    if (delta.isNotEmpty()) emit(AiChunk.Token(delta))
                }
            }
        }
    }
}

class OpenAiProvider(config: ProviderConfig, client: OkHttpClient = OkHttpClient()) :
    OpenAiCompatProvider(
        id = AiProviderId.OPENAI,
        config = config,
        defaultBase = "https://api.openai.com/v1",
        models = listOf("gpt-4o", "gpt-4o-mini", "o3-mini"),
        client = client,
    )

class DeepSeekProvider(config: ProviderConfig, client: OkHttpClient = OkHttpClient()) :
    OpenAiCompatProvider(
        id = AiProviderId.DEEPSEEK,
        config = config,
        defaultBase = "https://api.deepseek.com/v1",
        models = listOf("deepseek-chat", "deepseek-reasoner"),
        client = client,
    )

class OllamaProvider(config: ProviderConfig, client: OkHttpClient = OkHttpClient()) :
    OpenAiCompatProvider(
        id = AiProviderId.OLLAMA,
        // Local models: default to user's machine; configurable in settings.
        config = config.copy(baseUrl = config.baseUrl ?: "http://localhost:11434/v1"),
        defaultBase = "http://localhost:11434/v1",
        models = listOf("llama3.1", "qwen2.5-coder", "deepseek-coder-v2", "codellama"),
        client = client,
    )

/** Claude (Anthropic Messages API). */
class ClaudeProvider(
    private val config: ProviderConfig,
    private val client: OkHttpClient = OkHttpClient(),
) : AiProvider {
    override val id = AiProviderId.CLAUDE
    override val models = listOf("claude-3-5-sonnet-latest", "claude-3-5-haiku-latest")

    override fun chat(model: String, messages: List<ChatMessage>, context: AiContext) = flow {
        val base = config.baseUrl ?: "https://api.anthropic.com/v1"
        val body = JSONObject().apply {
            put("model", model)
            put("max_tokens", 4096)
            put("stream", true)
            put("system", PromptBuilder.systemPrompt(context))
            val arr = JSONArray()
            messages.forEach { arr.put(JSONObject().put("role", it.role).put("content", it.content)) }
            put("messages", arr)
        }.toString()
        val req = Request.Builder()
            .url("$base/messages")
            .header("x-api-key", config.apiKey.orEmpty())
            .header("anthropic-version", "2023-06-01")
            .post(body.toRequestBody(JSON))
            .build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) { emit(AiChunk.Error("HTTP ${resp.code}")); return@flow }
            val source = resp.body?.source() ?: return@flow
            while (!source.exhausted()) {
                val line = source.readUtf8Line() ?: break
                if (!line.startsWith("data:")) continue
                val data = line.removePrefix("data:").trim()
                runCatching {
                    val obj = JSONObject(data)
                    if (obj.optString("type") == "content_block_delta") {
                        val delta = obj.getJSONObject("delta").optString("text")
                        if (delta.isNotEmpty()) emit(AiChunk.Token(delta))
                    }
                    if (obj.optString("type") == "message_stop") emit(AiChunk.Done(0, 0))
                }
            }
        }
    }
}

/** Gemini (Google Generative Language API). */
class GeminiProvider(
    private val config: ProviderConfig,
    private val client: OkHttpClient = OkHttpClient(),
) : AiProvider {
    override val id = AiProviderId.GEMINI
    override val models = listOf("gemini-1.5-pro", "gemini-1.5-flash", "gemini-2.0-flash")

    override fun chat(model: String, messages: List<ChatMessage>, context: AiContext) = flow {
        val base = config.baseUrl ?: "https://generativelanguage.googleapis.com/v1beta"
        val contents = JSONArray()
        messages.forEach { m ->
            contents.put(
                JSONObject()
                    .put("role", if (m.role == "assistant") "model" else "user")
                    .put("parts", JSONArray().put(JSONObject().put("text", m.content)))
            )
        }
        val body = JSONObject().put("contents", contents).toString()
        val req = Request.Builder()
            .url("$base/models/$model:streamGenerateContent?alt=sse&key=${config.apiKey}")
            .post(body.toRequestBody(JSON))
            .build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) { emit(AiChunk.Error("HTTP ${resp.code}")); return@flow }
            val source = resp.body?.source() ?: return@flow
            while (!source.exhausted()) {
                val line = source.readUtf8Line() ?: break
                if (!line.startsWith("data:")) continue
                runCatching {
                    val delta = JSONObject(line.removePrefix("data:").trim())
                        .getJSONArray("candidates").getJSONObject(0)
                        .getJSONObject("content").getJSONArray("parts")
                        .getJSONObject(0).optString("text")
                    if (delta.isNotEmpty()) emit(AiChunk.Token(delta))
                }
            }
            emit(AiChunk.Done(0, 0))
        }
    }
}

package com.codespace.ide.ai

import com.codespace.ide.domain.AiAction
import com.codespace.ide.domain.AiProviderId
import com.codespace.ide.domain.ChatMessage
import kotlinx.coroutines.flow.Flow

/** Context handed to the AI for project-aware prompts ("chat with project files"). */
data class AiContext(
    val projectId: String? = null,
    val openFiles: List<String> = emptyList(),
    val selection: String? = null,
    val language: String? = null,
    val retrievedChunks: List<String> = emptyList(),
)

/** A streamed token or terminal event from a provider. */
sealed interface AiChunk {
    data class Token(val delta: String) : AiChunk
    data class Done(val promptTokens: Int, val completionTokens: Int) : AiChunk
    data class Error(val message: String) : AiChunk
}

/**
 * Uniform interface every provider adapter implements. The app can call providers
 * directly (BYOK; key in Android Keystore) or via the backend proxy (`/ai/*`), which is
 * the same wire shape. Adding a provider = one new class registered in [AiRegistry].
 */
interface AiProvider {
    val id: AiProviderId
    val models: List<String>

    fun chat(
        model: String,
        messages: List<ChatMessage>,
        context: AiContext,
    ): Flow<AiChunk>

    fun codeAction(
        action: AiAction,
        code: String,
        context: AiContext,
        model: String,
    ): Flow<AiChunk> {
        val prompt = PromptBuilder.forAction(action, code, context)
        return chat(model, listOf(ChatMessage("user", prompt)), context)
    }
}

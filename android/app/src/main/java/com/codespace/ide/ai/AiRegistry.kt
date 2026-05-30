package com.codespace.ide.ai

import com.codespace.ide.domain.AiProviderId
import okhttp3.OkHttpClient
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Central place that constructs providers from stored configs. Registering a new AI
 * vendor is a one-line addition here — the rest of the app talks only to [AiProvider].
 */
@Singleton
class AiRegistry @Inject constructor(
    private val client: OkHttpClient,
) {
    fun create(id: AiProviderId, config: ProviderConfig): AiProvider = when (id) {
        AiProviderId.OPENAI -> OpenAiProvider(config, client)
        AiProviderId.CLAUDE -> ClaudeProvider(config, client)
        AiProviderId.GEMINI -> GeminiProvider(config, client)
        AiProviderId.DEEPSEEK -> DeepSeekProvider(config, client)
        AiProviderId.OLLAMA -> OllamaProvider(config, client)
    }
}

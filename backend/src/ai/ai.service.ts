import { Injectable } from '@nestjs/common';

export type ProviderId = 'openai' | 'claude' | 'gemini' | 'deepseek' | 'ollama';

interface ChatParams {
  provider: ProviderId;
  model: string;
  messages: { role: string; content: string }[];
  apiKey?: string;
  baseUrl?: string;
}

/**
 * Server-side AI proxy. Normalizes every vendor's streaming format into a single
 * `{ delta }` / `{ usage }` SSE shape so the app code is provider-agnostic. Keys can be
 * the user's stored key (proxy mode) or a server default; BYOK calls go direct from the
 * app instead.
 */
@Injectable()
export class AiService {
  /** Returns an async iterable of normalized SSE-ready strings. */
  async *chatStream(params: ChatParams): AsyncGenerator<string> {
    const { provider } = params;
    const upstream = await this.openUpstream(params);
    if (!upstream.body) {
      yield this.sse('error', { message: `Upstream ${provider} returned no body` });
      return;
    }
    const reader = (upstream.body as any).getReader?.();
    const decoder = new TextDecoder();

    // Fallback for Node streams without getReader()
    if (!reader) {
      for await (const chunk of upstream.body as any) {
        for (const frame of this.parse(provider, decoder.decode(chunk))) {
          yield frame;
        }
      }
      yield this.sse('done', { usage: {} });
      return;
    }

    while (true) {
      const { done, value } = await reader.read();
      if (done) break;
      for (const frame of this.parse(provider, decoder.decode(value))) {
        yield frame;
      }
    }
    yield this.sse('done', { usage: {} });
  }

  private async openUpstream(p: ChatParams): Promise<Response> {
    switch (p.provider) {
      case 'openai':
      case 'deepseek':
      case 'ollama': {
        const base =
          p.baseUrl ??
          (p.provider === 'openai'
            ? 'https://api.openai.com/v1'
            : p.provider === 'deepseek'
              ? 'https://api.deepseek.com/v1'
              : process.env.OLLAMA_BASE_URL ?? 'http://localhost:11434/v1');
        return fetch(`${base}/chat/completions`, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            ...(p.apiKey ? { Authorization: `Bearer ${p.apiKey}` } : {}),
          },
          body: JSON.stringify({ model: p.model, messages: p.messages, stream: true }),
        });
      }
      case 'claude':
        return fetch('https://api.anthropic.com/v1/messages', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'x-api-key': p.apiKey ?? '',
            'anthropic-version': '2023-06-01',
          },
          body: JSON.stringify({
            model: p.model,
            max_tokens: 4096,
            stream: true,
            messages: p.messages,
          }),
        });
      case 'gemini':
        return fetch(
          `https://generativelanguage.googleapis.com/v1beta/models/${p.model}:streamGenerateContent?alt=sse&key=${p.apiKey}`,
          {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
              contents: p.messages.map((m) => ({
                role: m.role === 'assistant' ? 'model' : 'user',
                parts: [{ text: m.content }],
              })),
            }),
          },
        );
    }
  }

  /** Convert vendor stream lines into normalized SSE token frames. */
  private parse(provider: ProviderId, text: string): string[] {
    const out: string[] = [];
    for (const line of text.split('\n')) {
      if (!line.startsWith('data:')) continue;
      const data = line.slice(5).trim();
      if (!data || data === '[DONE]') continue;
      try {
        const json = JSON.parse(data);
        let delta = '';
        if (provider === 'claude') {
          if (json.type === 'content_block_delta') delta = json.delta?.text ?? '';
        } else if (provider === 'gemini') {
          delta = json.candidates?.[0]?.content?.parts?.[0]?.text ?? '';
        } else {
          delta = json.choices?.[0]?.delta?.content ?? '';
        }
        if (delta) out.push(this.sse('token', { delta }));
      } catch {
        /* skip non-JSON keepalives */
      }
    }
    return out;
  }

  private sse(event: string, data: unknown): string {
    return `event: ${event}\ndata: ${JSON.stringify(data)}\n\n`;
  }
}

import { Body, Controller, Post, Res, UseGuards } from '@nestjs/common';
import { Response } from 'express';
import { ApiBearerAuth, ApiTags } from '@nestjs/swagger';
import { JwtAuthGuard } from '../auth/jwt-auth.guard';
import { AiService, ProviderId } from './ai.service';

interface ChatBody {
  provider: ProviderId;
  model: string;
  messages: { role: string; content: string }[];
  apiKey?: string;
  baseUrl?: string;
}

@ApiTags('ai')
@ApiBearerAuth()
@UseGuards(JwtAuthGuard)
@Controller('ai')
export class AiController {
  constructor(private readonly ai: AiService) {}

  @Post('chat')
  async chat(@Body() body: ChatBody, @Res() res: Response) {
    res.setHeader('Content-Type', 'text/event-stream');
    res.setHeader('Cache-Control', 'no-cache');
    res.setHeader('Connection', 'keep-alive');
    res.flushHeaders();
    try {
      for await (const frame of this.ai.chatStream(body)) {
        res.write(frame);
      }
    } catch (e: any) {
      res.write(`event: error\ndata: ${JSON.stringify({ message: e?.message })}\n\n`);
    } finally {
      res.end();
    }
  }
}

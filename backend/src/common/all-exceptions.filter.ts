import {
  ArgumentsHost,
  Catch,
  ExceptionFilter,
  HttpException,
  HttpStatus,
  Logger,
} from '@nestjs/common';
import { Request, Response } from 'express';
import { randomUUID } from 'crypto';

/** Stable, machine-readable error codes (see docs/09-error-handling.md). */
const STATUS_CODE_MAP: Record<number, string> = {
  400: 'VALIDATION_FAILED',
  401: 'UNAUTHORIZED',
  403: 'FORBIDDEN',
  404: 'RESOURCE_NOT_FOUND',
  409: 'CONFLICT',
  429: 'RATE_LIMITED',
  502: 'UPSTREAM_ERROR',
};

@Catch()
export class AllExceptionsFilter implements ExceptionFilter {
  private readonly logger = new Logger('Exception');

  catch(exception: unknown, host: ArgumentsHost) {
    const ctx = host.switchToHttp();
    const res = ctx.getResponse<Response>();
    const req = ctx.getRequest<Request>();
    const traceId = (req.headers['x-trace-id'] as string) ?? randomUUID();

    const status =
      exception instanceof HttpException
        ? exception.getStatus()
        : HttpStatus.INTERNAL_SERVER_ERROR;

    const raw =
      exception instanceof HttpException ? exception.getResponse() : null;

    const message =
      typeof raw === 'object' && raw !== null && 'message' in raw
        ? (raw as any).message
        : status >= 500
          ? 'Internal server error'
          : (exception as Error)?.message ?? 'Error';

    const code = STATUS_CODE_MAP[status] ?? 'INTERNAL';

    // Full detail server-side, sanitized payload to client.
    this.logger.error(`[${traceId}] ${status} ${code}`, (exception as Error)?.stack);

    res.status(status).json({
      error: {
        code,
        message: Array.isArray(message) ? message.join('; ') : message,
        traceId,
        timestamp: new Date().toISOString(),
      },
    });
  }
}

import { Body, Controller, Get, Param, Post, Query, UseGuards } from '@nestjs/common';
import { ApiBearerAuth, ApiTags } from '@nestjs/swagger';
import { JwtAuthGuard } from '../auth/jwt-auth.guard';
import { SyncService } from './sync.service';

@ApiTags('sync')
@ApiBearerAuth()
@UseGuards(JwtAuthGuard)
@Controller('sync')
export class SyncController {
  constructor(private readonly sync: SyncService) {}

  @Get(':projectId/pull')
  pull(@Param('projectId') id: string, @Query('sinceRev') sinceRev = '0') {
    return this.sync.pull(id, Number(sinceRev));
  }

  @Post(':projectId/push')
  push(
    @Param('projectId') id: string,
    @Body() body: { rev: number; changes: any[] },
  ) {
    return this.sync.push(id, body.rev, body.changes);
  }
}

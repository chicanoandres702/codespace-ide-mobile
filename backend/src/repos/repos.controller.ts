import { Controller, Get, Post, Body, Param, UseGuards, Request } from '@nestjs/common';
import { ApiBearerAuth, ApiTags } from '@nestjs/swagger';
import { JwtAuthGuard } from '../auth/jwt-auth.guard';
import { GithubService } from './github.service';

@ApiTags('github')
@ApiBearerAuth()
@UseGuards(JwtAuthGuard)
@Controller('github')
export class ReposController {
  constructor(private readonly github: GithubService) {}

  // In production, resolve the user's stored GitHub token from oauth_accounts.
  private tokenFor(_req: any): string {
    return process.env.GITHUB_TEST_TOKEN ?? '';
  }

  @Get('repos')
  repos(@Request() req: any) {
    return this.github.listRepos(this.tokenFor(req));
  }

  @Post('repos/:owner/:repo/pulls')
  createPr(
    @Request() req: any,
    @Param('owner') owner: string,
    @Param('repo') repo: string,
    @Body() body: { title: string; head: string; base: string; body?: string },
  ) {
    return this.github.createPullRequest(this.tokenFor(req), owner, repo, body);
  }

  @Get('codespaces')
  codespaces(@Request() req: any) {
    return this.github.listCodespaces(this.tokenFor(req));
  }
}

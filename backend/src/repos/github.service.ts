import { Injectable } from '@nestjs/common';
import { Octokit } from 'octokit';

/**
 * GitHub operations that require the user's OAuth token. The token is looked up from the
 * encrypted oauth_accounts store (omitted here for brevity) and never returned to clients.
 */
@Injectable()
export class GithubService {
  private client(token: string) {
    return new Octokit({ auth: token });
  }

  async listRepos(token: string) {
    const octokit = this.client(token);
    const { data } = await octokit.rest.repos.listForAuthenticatedUser({
      per_page: 100,
      sort: 'updated',
    });
    return data.map((r) => ({
      id: r.id,
      name: r.name,
      fullName: r.full_name,
      defaultBranch: r.default_branch,
      private: r.private,
    }));
  }

  async createPullRequest(
    token: string,
    owner: string,
    repo: string,
    body: { title: string; head: string; base: string; body?: string },
  ) {
    const octokit = this.client(token);
    const { data } = await octokit.rest.pulls.create({ owner, repo, ...body });
    return { number: data.number, title: data.title, state: data.state, url: data.html_url };
  }

  async listCodespaces(token: string) {
    const octokit = this.client(token);
    const { data } = await octokit.rest.codespaces.listForAuthenticatedUser();
    return data.codespaces.map((c) => ({
      name: c.name,
      state: c.state,
      repo: c.repository?.full_name,
    }));
  }
}

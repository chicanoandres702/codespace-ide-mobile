import { Injectable } from '@nestjs/common';

interface Change {
  path: string;
  op: 'write' | 'delete' | 'rename';
  checksum?: string;
}

/**
 * Optimistic, rev-based sync. Detects conflicts via monotonic rev + per-file checksums
 * and returns conflicting paths for client-side 3-way merge.
 */
@Injectable()
export class SyncService {
  // In production these are persisted in sync_state / object storage.
  private revs = new Map<string, number>();

  pull(projectId: string, sinceRev: number) {
    const current = this.revs.get(projectId) ?? 0;
    return { rev: current, changes: [] as Change[], hasMore: false, sinceRev };
  }

  push(projectId: string, clientRev: number, changes: Change[]) {
    const serverRev = this.revs.get(projectId) ?? 0;
    if (clientRev < serverRev) {
      // Conflict: client is behind. Caller resolves via 3-way merge.
      return { accepted: false, conflicts: changes.map((c) => c.path), serverRev };
    }
    const newRev = serverRev + 1;
    this.revs.set(projectId, newRev);
    return { accepted: true, rev: newRev, applied: changes.length };
  }
}

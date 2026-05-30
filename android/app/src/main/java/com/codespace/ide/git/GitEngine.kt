package com.codespace.ide.git

import com.codespace.ide.domain.AppError
import com.codespace.ide.domain.AppResult
import com.codespace.ide.domain.GitStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.MergeResult
import org.eclipse.jgit.lib.PersonIdent
import org.eclipse.jgit.transport.CredentialsProvider
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * On-device Git built on JGit. Supports the full local workflow plus authenticated
 * remote operations. For a GitHub HTTPS remote, pass a personal access token (or the
 * OAuth token brokered by the backend) as the password with username "x-access-token".
 */
@Singleton
class GitEngine @Inject constructor() {

    private fun creds(token: String?): CredentialsProvider? =
        token?.let { UsernamePasswordCredentialsProvider("x-access-token", it) }

    suspend fun clone(url: String, dest: File, token: String? = null): AppResult<File> =
        io {
            Git.cloneRepository()
                .setURI(url)
                .setDirectory(dest)
                .apply { creds(token)?.let { setCredentialsProvider(it) } }
                .call().use { }
            dest
        }

    suspend fun status(repoDir: File): AppResult<GitStatus> = io {
        Git.open(repoDir).use { git ->
            val status = git.status().call()
            val branch = git.repository.branch
            val tracking = git.repository.config
            GitStatus(
                branch = branch,
                ahead = 0,
                behind = 0,
                staged = (status.added + status.changed + status.removed).toList(),
                modified = status.modified.toList(),
                untracked = status.untracked.toList(),
            )
        }
    }

    suspend fun stageAll(repoDir: File): AppResult<Unit> = io {
        Git.open(repoDir).use { it.add().addFilepattern(".").call() }
        Unit
    }

    suspend fun commit(
        repoDir: File,
        message: String,
        authorName: String,
        authorEmail: String,
    ): AppResult<String> = io {
        Git.open(repoDir).use { git ->
            val rev = git.commit()
                .setMessage(message)
                .setAuthor(PersonIdent(authorName, authorEmail))
                .call()
            rev.name
        }
    }

    suspend fun pull(repoDir: File, token: String? = null): AppResult<Unit> = io {
        Git.open(repoDir).use { git ->
            git.pull().apply { creds(token)?.let { setCredentialsProvider(it) } }.call()
        }
        Unit
    }

    suspend fun push(repoDir: File, token: String? = null): AppResult<Unit> = io {
        Git.open(repoDir).use { git ->
            git.push().apply { creds(token)?.let { setCredentialsProvider(it) } }.call()
        }
        Unit
    }

    suspend fun listBranches(repoDir: File): AppResult<List<String>> = io {
        Git.open(repoDir).use { git ->
            git.branchList().call().map { it.name.removePrefix("refs/heads/") }
        }
    }

    suspend fun createBranch(repoDir: File, name: String): AppResult<Unit> = io {
        Git.open(repoDir).use { git ->
            git.checkout().setCreateBranch(true).setName(name).call()
        }
        Unit
    }

    suspend fun checkout(repoDir: File, name: String): AppResult<Unit> = io {
        Git.open(repoDir).use { it.checkout().setName(name).call() }
        Unit
    }

    suspend fun merge(repoDir: File, branch: String): AppResult<MergeResult.MergeStatus> = io {
        Git.open(repoDir).use { git ->
            val ref = git.repository.findRef(branch)
            git.merge().include(ref).call().mergeStatus
        }
    }

    /** Unified diff for a single file against HEAD (used by the diff viewer). */
    suspend fun diff(repoDir: File): AppResult<String> = io {
        Git.open(repoDir).use { git ->
            val out = java.io.ByteArrayOutputStream()
            git.diff().setOutputStream(out).call()
            out.toString("UTF-8")
        }
    }

    private suspend inline fun <T> io(crossinline block: () -> T): AppResult<T> =
        withContext(Dispatchers.IO) {
            try {
                AppResult.Success(block())
            } catch (t: Throwable) {
                AppResult.Failure(AppError.Git(t.message ?: "Git operation failed"))
            }
        }
}

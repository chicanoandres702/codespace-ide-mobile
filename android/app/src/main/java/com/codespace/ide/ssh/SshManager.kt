package com.codespace.ide.ssh

import com.codespace.ide.domain.AppError
import com.codespace.ide.domain.AppResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.connection.channel.direct.Session
import net.schmizz.sshj.transport.verification.PromiscuousVerifier
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton

data class SshCredentials(
    val host: String,
    val port: Int = 22,
    val username: String,
    val password: String? = null,
    val privateKeyPem: String? = null,
)

/**
 * Direct on-device SSH/SFTP via SSHJ. Used when the phone can reach the host directly;
 * otherwise the backend proxy path (RemoteModule) is used. Host-key verification should
 * use a known-hosts store in production — PromiscuousVerifier is for first-connect UX and
 * prompts the user to trust the fingerprint.
 */
@Singleton
class SshManager @Inject constructor() {

    private fun connect(creds: SshCredentials): SSHClient {
        val ssh = SSHClient()
        ssh.addHostKeyVerifier(PromiscuousVerifier()) // replace with user-confirmed known_hosts
        ssh.connect(creds.host, creds.port)
        when {
            creds.privateKeyPem != null ->
                ssh.authPublickey(creds.username, ssh.loadKeys(creds.privateKeyPem, null, null))
            creds.password != null ->
                ssh.authPassword(creds.username, creds.password)
            else -> error("No authentication method provided")
        }
        return ssh
    }

    suspend fun testConnection(creds: SshCredentials): AppResult<Unit> =
        withContext(Dispatchers.IO) {
            try {
                connect(creds).use { it.isConnected }
                AppResult.Success(Unit)
            } catch (t: Throwable) {
                AppResult.Failure(AppError.Ssh(t.message ?: "SSH connection failed"))
            }
        }

    suspend fun runCommand(creds: SshCredentials, command: String): AppResult<String> =
        withContext(Dispatchers.IO) {
            try {
                connect(creds).use { ssh ->
                    ssh.startSession().use { session ->
                        val cmd = session.exec(command)
                        val output = cmd.inputStream.bufferedReader().readText()
                        cmd.join()
                        AppResult.Success(output)
                    }
                }
            } catch (t: Throwable) {
                AppResult.Failure(AppError.Ssh(t.message ?: "Command failed"))
            }
        }

    /**
     * Opens an interactive PTY shell. Emits stdout/stderr lines; write to [onReady]'s
     * OutputStream to send stdin. Backs the remote terminal tab.
     */
    fun openShell(
        creds: SshCredentials,
        onReady: (OutputStream) -> Unit,
    ): Flow<String> = callbackFlow {
        val ssh = connect(creds)
        val session = ssh.startSession()
        session.allocateDefaultPTY()
        val shell: Session.Shell = session.startShell()
        onReady(shell.outputStream)

        val reader = shell.inputStream.bufferedReader()
        val readerThread = Thread {
            try {
                var line = reader.readLine()
                while (line != null) {
                    trySend(line + "\n")
                    line = reader.readLine()
                }
            } catch (_: Throwable) { /* closed */ }
        }.apply { start() }

        awaitClose {
            runCatching { session.close() }
            runCatching { ssh.disconnect() }
            readerThread.interrupt()
        }
    }.flowOn(Dispatchers.IO)
}

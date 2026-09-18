package fr.stellaronstep.app.core.onstep

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.net.InetSocketAddress
import java.net.Socket

class OnStepTcpClient(
    private val configProvider: () -> OnStepConnectionConfig
) {
    private val lock = Any()

    suspend fun queryHash(command: String): String = withContext(Dispatchers.IO) {
        transact(command, ReplyMode.HASH_TERMINATED)
    }

    suspend fun queryByte(command: String): String = withContext(Dispatchers.IO) {
        transact(command, ReplyMode.ONE_BYTE)
    }

    suspend fun send(command: String) = withContext(Dispatchers.IO) {
        transact(command, ReplyMode.NONE)
        Unit
    }

    private fun transact(command: String, mode: ReplyMode): String = synchronized(lock) {
        require(command.startsWith(":") && command.endsWith("#")) {
            "Commande OnStep invalide: $command"
        }

        val config = configProvider()
        Socket().use { socket ->
            socket.soTimeout = config.timeoutMs
            socket.connect(InetSocketAddress(config.host, config.port), config.timeoutMs)
            val output = socket.getOutputStream()
            val input = BufferedInputStream(socket.getInputStream())

            output.write(command.toByteArray(Charsets.US_ASCII))
            output.flush()

            when (mode) {
                ReplyMode.NONE -> ""
                ReplyMode.ONE_BYTE -> input.read().let { value ->
                    if (value < 0) error("Connexion fermée par OnStep")
                    value.toChar().toString()
                }
                ReplyMode.HASH_TERMINATED -> buildString {
                    while (true) {
                        val value = input.read()
                        if (value < 0) error("Connexion fermée avant la fin de réponse")
                        val c = value.toChar()
                        if (c == '#') break
                        append(c)
                    }
                }
            }
        }
    }

    private enum class ReplyMode { NONE, ONE_BYTE, HASH_TERMINATED }
}

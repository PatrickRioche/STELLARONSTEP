package fr.stellaronstep.app.data

import android.content.Context
import fr.stellaronstep.app.core.onstep.OnStepTcpClient

/**
 * Session OnStepX unique au niveau de l'application.
 *
 * Home, Control, GOTO, Align et Config utilisent le meme
 * OnStepTcpClient et le meme OnStepRepository.
 */
object OnStepSession {
    private val creationLock = Any()

    @Volatile
    private var clientInstance: OnStepTcpClient? = null

    @Volatile
    private var repositoryInstance: OnStepRepository? = null

    fun client(
        context: Context
    ): OnStepTcpClient {
        ensure(context)
        return checkNotNull(clientInstance)
    }

    fun repository(
        context: Context
    ): OnStepRepository {
        ensure(context)
        return checkNotNull(repositoryInstance)
    }

    private fun ensure(
        context: Context
    ) {
        if (
            clientInstance != null &&
            repositoryInstance != null
        ) {
            return
        }

        synchronized(creationLock) {
            if (
                clientInstance == null ||
                repositoryInstance == null
            ) {
                val appContext =
                    context.applicationContext

                val settings =
                    SettingsStore(appContext)

                val client =
                    OnStepTcpClient {
                        /*
                         * Recharge CONFIG a chaque transaction.
                         * Un changement IP/port est pris en compte
                         * sans recreer la session.
                         */
                        settings.load()
                    }

                clientInstance = client
                repositoryInstance =
                    OnStepRepository(client)
            }
        }
    }
}
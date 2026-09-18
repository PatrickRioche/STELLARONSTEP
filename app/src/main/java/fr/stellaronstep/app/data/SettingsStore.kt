package fr.stellaronstep.app.data

import android.content.Context
import fr.stellaronstep.app.core.onstep.OnStepConnectionConfig

class SettingsStore(context: Context) {

    private val prefs =
        context.getSharedPreferences(
            "stellaronstep",
            Context.MODE_PRIVATE
        )

    fun load(): OnStepConnectionConfig {

        val savedHost =
            prefs.getString(
                "host",
                null
            )?.trim()

        val host =
            when {
                savedHost.isNullOrBlank() ->
                    DEFAULT_HOST

                savedHost == LEGACY_STELLARPILOT_HOST -> {
                    prefs.edit()
                        .putString(
                            "host",
                            DEFAULT_HOST
                        )
                        .apply()

                    DEFAULT_HOST
                }

                else ->
                    savedHost
            }

        return OnStepConnectionConfig(
            host = host,
            port = prefs.getInt(
                "port",
                DEFAULT_PORT
            ),
            timeoutMs = prefs.getInt(
                "timeout",
                DEFAULT_TIMEOUT_MS
            )
        )
    }

    fun save(
        config: OnStepConnectionConfig
    ) {
        prefs.edit()
            .putString(
                "host",
                config.host.trim()
            )
            .putInt(
                "port",
                config.port
            )
            .putInt(
                "timeout",
                config.timeoutMs
            )
            .apply()
    }

    companion object {

        const val DEFAULT_HOST =
            "192.168.0.1"

        const val DEFAULT_PORT =
            9999

        const val DEFAULT_TIMEOUT_MS =
            1800

        private const val LEGACY_STELLARPILOT_HOST =
            "192.168.1.46"
    }
}
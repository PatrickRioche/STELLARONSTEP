package fr.onsteppilot.app.data

import android.content.Context
import fr.onsteppilot.app.core.onstep.OnStepConnectionConfig

class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("onsteppilot", Context.MODE_PRIVATE)

    fun load(): OnStepConnectionConfig = OnStepConnectionConfig(
        host = prefs.getString("host", "192.168.1.46") ?: "192.168.1.46",
        port = prefs.getInt("port", 9999),
        timeoutMs = prefs.getInt("timeout", 1800)
    )

    fun save(config: OnStepConnectionConfig) {
        prefs.edit()
            .putString("host", config.host)
            .putInt("port", config.port)
            .putInt("timeout", config.timeoutMs)
            .apply()
    }
}

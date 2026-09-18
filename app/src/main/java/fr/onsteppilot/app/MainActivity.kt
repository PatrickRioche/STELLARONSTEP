package fr.onsteppilot.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import fr.onsteppilot.app.ui.theme.OnStepPilotTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            OnStepPilotTheme {
                OnStepPilotApp()
            }
        }
    }
}

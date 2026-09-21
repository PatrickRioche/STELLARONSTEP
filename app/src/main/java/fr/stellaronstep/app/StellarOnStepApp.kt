package fr.stellaronstep.app

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.stellaronstep.app.feature.align.AlignScreen
import fr.stellaronstep.app.feature.config.ConfigScreen
import fr.stellaronstep.app.feature.control.ControlScreen
import fr.stellaronstep.app.feature.goto.GotoScreen
import fr.stellaronstep.app.feature.home.HomeScreen
import fr.stellaronstep.app.ui.theme.Bg
import fr.stellaronstep.app.ui.theme.Surface

private enum class Screen(
    val label: String,
    val glyph: String
) {
    HOME("Home", "⌂"),
    CONTROL("Control", "✥"),
    GOTO("Goto", "◎"),
    ALIGN("Align", "✦"),
    CONFIG("Config", "⚙")
}

@Composable
fun StellarOnStepApp(
    vm: AppViewModel = viewModel()
) {
    var screen by
        remember {
            mutableStateOf(Screen.HOME)
        }

    LaunchedEffect(Unit) {
        vm.connectAndPoll()
    }

    Scaffold(
        containerColor = Bg,
        bottomBar = {
            NavigationBar(
                containerColor = Surface
            ) {
                Screen.entries
                    .forEach { item ->
                        NavigationBarItem(
                            selected =
                                screen == item,
                            onClick = {
                                screen = item
                            },
                            icon = {
                                Text(item.glyph)
                            },
                            label = {
                                Text(item.label)
                            }
                        )
                    }
            }
        }
    ) { padding ->
        when (screen) {
            Screen.HOME ->
                HomeScreen(
                    vm,
                    Modifier.padding(padding)
                )

            Screen.CONTROL ->
                ControlScreen(
                    vm,
                    Modifier.padding(padding)
                )

            Screen.GOTO ->
                GotoScreen(
                    vm,
                    Modifier.padding(padding)
                )

            Screen.ALIGN ->
                AlignScreen(
                    vm = vm,
                    modifier =
                        Modifier.padding(
                            padding
                        ),
                    onOpenControl = {
                        screen =
                            Screen.CONTROL
                    }
                )

            Screen.CONFIG ->
                ConfigScreen(
                    vm,
                    Modifier.padding(padding)
                )
        }
    }
}

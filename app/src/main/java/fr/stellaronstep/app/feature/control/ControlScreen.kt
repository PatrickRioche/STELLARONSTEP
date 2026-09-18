package fr.stellaronstep.app.feature.control

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import fr.stellaronstep.app.AppViewModel
import fr.stellaronstep.app.core.onstep.SlewDirection
import fr.stellaronstep.app.core.onstep.SlewRate
import fr.stellaronstep.app.ui.theme.Danger

@Composable
fun ControlScreen(
    vm: AppViewModel,
    modifier: Modifier = Modifier
) {

    val status =
        vm.status

    val canMove =
        status.connected &&
            !status.parked

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(20.dp),
        horizontalAlignment =
            Alignment.CenterHorizontally,
        verticalArrangement =
            Arrangement.spacedBy(14.dp)
    ) {

        Text(
            "CONTROL",
            fontWeight =
                FontWeight.Bold
        )

        Text(
            if (status.connected) {
                "OnStepX connecte"
            } else {
                "OnStepX deconnecte"
            }
        )

        if (status.parked) {
            Text(
                "PARK actif - UNPARK requis",
                color =
                    Danger,
                fontWeight =
                    FontWeight.Bold
            )
        }

        Text(
            "Vitesse : ${vm.selectedRate.label}"
        )

        Row(
            horizontalArrangement =
                Arrangement.spacedBy(8.dp)
        ) {

            SlewRate.entries.forEach {
                rate ->

                if (
                    vm.selectedRate ==
                        rate
                ) {
                    Button(
                        onClick = {
                            vm.setRate(
                                rate
                            )
                        }
                    ) {
                        Text(
                            rate.label
                        )
                    }
                } else {
                    OutlinedButton(
                        onClick = {
                            vm.setRate(
                                rate
                            )
                        }
                    ) {
                        Text(
                            rate.label
                        )
                    }
                }
            }
        }

        Text(
            "Maintenir une direction pour deplacer la monture."
        )

        HoldDirectionButton(
            label = "N",
            direction =
                SlewDirection.NORTH,
            vm = vm,
            enabled = canMove
        )

        Row(
            horizontalArrangement =
                Arrangement.spacedBy(30.dp)
        ) {

            HoldDirectionButton(
                label = "W",
                direction =
                    SlewDirection.WEST,
                vm = vm,
                enabled = canMove
            )

            HoldDirectionButton(
                label = "E",
                direction =
                    SlewDirection.EAST,
                vm = vm,
                enabled = canMove
            )
        }

        HoldDirectionButton(
            label = "S",
            direction =
                SlewDirection.SOUTH,
            vm = vm,
            enabled = canMove
        )

        Button(
            onClick = {
                vm.stopAll()
            },
            modifier =
                Modifier.fillMaxWidth()
        ) {
            Text(
                "STOP GLOBAL",
                color =
                    Danger,
                fontWeight =
                    FontWeight.Bold
            )
        }

        Row(
            horizontalArrangement =
                Arrangement.spacedBy(10.dp)
        ) {

            OutlinedButton(
                onClick = {
                    vm.goHome()
                }
            ) {
                Text("HOME")
            }

            OutlinedButton(
                onClick = {
                    vm.park()
                }
            ) {
                Text("PARK")
            }

            OutlinedButton(
                onClick = {
                    vm.unpark()
                }
            ) {
                Text("UNPARK")
            }
        }

        vm.movingDirection?.let {
            direction ->

            Text(
                "Mouvement : ${direction.label} / ${vm.selectedRate.label}",
                fontWeight =
                    FontWeight.Bold
            )
        }

        vm.message?.let {
            Text(it)
        }

        if (
            status.raw.isNotBlank()
        ) {
            Text(
                "GU: ${status.raw}",
                style =
                    MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun HoldDirectionButton(
    label: String,
    direction: SlewDirection,
    vm: AppViewModel,
    enabled: Boolean
) {

    val background =
        if (enabled) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        }

    val foreground =
        if (enabled) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        }

    Surface(
        modifier =
            Modifier
                .size(88.dp)
                .pointerInput(
                    direction,
                    enabled,
                    vm.selectedRate
                ) {

                    detectTapGestures(
                        onPress = {

                            if (!enabled) {
                                return@detectTapGestures
                            }

                            vm.startMove(
                                direction
                            )

                            try {
                                tryAwaitRelease()
                            } finally {
                                vm.stopMove(
                                    direction
                                )
                            }
                        }
                    )
                },
        shape =
            RoundedCornerShape(18.dp),
        color =
            background,
        contentColor =
            foreground,
        tonalElevation =
            4.dp
    ) {

        Column(
            modifier =
                Modifier.fillMaxSize(),
            verticalArrangement =
                Arrangement.Center,
            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {

            Text(
                label,
                style =
                    MaterialTheme.typography.headlineMedium,
                fontWeight =
                    FontWeight.Bold
            )
        }
    }
}
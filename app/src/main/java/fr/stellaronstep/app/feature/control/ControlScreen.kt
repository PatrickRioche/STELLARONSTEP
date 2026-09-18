package fr.stellaronstep.app.feature.control

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import fr.stellaronstep.app.ui.theme.AccentOrange
import fr.stellaronstep.app.ui.theme.Danger
import fr.stellaronstep.app.ui.theme.Muted
import fr.stellaronstep.app.ui.theme.Success
import fr.stellaronstep.app.ui.theme.Surface
import fr.stellaronstep.app.ui.theme.SurfaceRaised

@Composable
fun ControlScreen(
    vm: AppViewModel,
    modifier: Modifier = Modifier
) {
    val status = vm.status
    val canMove =
        status.connected &&
            !status.parked

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(horizontal = 18.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            "CONTROLE MONTURE",
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleLarge
        )

        Card(
            colors = CardDefaults.cardColors(
                containerColor = Surface
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(
                            color = if (status.connected) {
                                Success
                            } else {
                                Danger
                            },
                            shape = CircleShape
                        )
                )

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        if (status.connected) {
                            "OnStepX connecte"
                        } else {
                            "OnStepX deconnecte"
                        },
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        if (status.parked) {
                            "PARK actif - UNPARK requis"
                        } else {
                            "Pret au mouvement"
                        },
                        color = if (status.parked) Danger else Muted,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Text(
                    vm.selectedRate.label,
                    color = AccentOrange,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Text(
            "VITESSE",
            color = Muted,
            modifier = Modifier.align(Alignment.Start),
            style = MaterialTheme.typography.labelLarge
        )

        RateRow(
            first = SlewRate.GUIDE,
            second = SlewRate.CENTER,
            vm = vm
        )

        RateRow(
            first = SlewRate.MOVE,
            second = SlewRate.SLEW,
            vm = vm
        )

        Text(
            "Maintenir une direction pour deplacer la monture",
            color = Muted,
            style = MaterialTheme.typography.bodySmall
        )

        HoldDirectionButton(
            label = "N",
            direction = SlewDirection.NORTH,
            vm = vm,
            enabled = canMove
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            HoldDirectionButton(
                label = "W",
                direction = SlewDirection.WEST,
                vm = vm,
                enabled = canMove
            )

            Button(
                onClick = { vm.stopAll() },
                modifier = Modifier.size(86.dp),
                shape = CircleShape
            ) {
                Text(
                    "STOP",
                    color = Danger,
                    fontWeight = FontWeight.Black
                )
            }

            HoldDirectionButton(
                label = "E",
                direction = SlewDirection.EAST,
                vm = vm,
                enabled = canMove
            )
        }

        HoldDirectionButton(
            label = "S",
            direction = SlewDirection.SOUTH,
            vm = vm,
            enabled = canMove
        )

        Text(
            "DEPLACEMENTS AUTO",
            color = Muted,
            modifier = Modifier.align(Alignment.Start),
            style = MaterialTheme.typography.labelLarge
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { vm.goHome() },
                modifier = Modifier.weight(1f)
            ) {
                Text("HOME")
            }

            OutlinedButton(
                onClick = { vm.park() },
                modifier = Modifier.weight(1f)
            ) {
                Text("PARK")
            }

            OutlinedButton(
                onClick = { vm.unpark() },
                modifier = Modifier.weight(1f)
            ) {
                Text("UNPARK")
            }
        }

        Text(
            "REFERENCES",
            color = Muted,
            modifier = Modifier.align(Alignment.Start),
            style = MaterialTheme.typography.labelLarge
        )

        Card(
            colors = CardDefaults.cardColors(
                containerColor = Surface
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "Ces commandes redefinissent les references de la monture.",
                    color = AccentOrange,
                    style = MaterialTheme.typography.bodySmall
                )

                Text(
                    "RESET HOME : placer d'abord la monture physiquement en position HOME.",
                    color = Muted,
                    style = MaterialTheme.typography.bodySmall
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { vm.resetHome() },
                        enabled = status.connected && !vm.busy,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("RESET HOME")
                    }

                    OutlinedButton(
                        onClick = { vm.setParkPosition() },
                        enabled = status.connected && !vm.busy,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("RESET PARK")
                    }
                }

                Text(
                    "RESET PARK : memorise la position actuelle comme position PARK.",
                    color = Muted,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        vm.movingDirection?.let { direction ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = SurfaceRaised
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Mouvement : ${direction.label} / ${vm.selectedRate.label}",
                    modifier = Modifier.padding(12.dp),
                    fontWeight = FontWeight.Bold
                )
            }
        }

        vm.message?.let {
            Text(
                it,
                color = Muted,
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (status.raw.isNotBlank()) {
            Text(
                "GU: ${status.raw}",
                color = Muted,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun RateRow(
    first: SlewRate,
    second: SlewRate,
    vm: AppViewModel
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        RateButton(
            rate = first,
            vm = vm,
            modifier = Modifier.weight(1f)
        )

        RateButton(
            rate = second,
            vm = vm,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun RateButton(
    rate: SlewRate,
    vm: AppViewModel,
    modifier: Modifier = Modifier
) {
    if (vm.selectedRate == rate) {
        Button(
            onClick = { vm.setRate(rate) },
            modifier = modifier
        ) {
            Text(
                rate.label,
                fontWeight = FontWeight.Bold
            )
        }
    } else {
        OutlinedButton(
            onClick = { vm.setRate(rate) },
            modifier = modifier
        ) {
            Text(rate.label)
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
            MaterialTheme.colorScheme.surfaceVariant
        } else {
            MaterialTheme.colorScheme.surface
        }

    val foreground =
        if (enabled) {
            MaterialTheme.colorScheme.primary
        } else {
            Muted
        }

    Surface(
        modifier = Modifier
            .size(78.dp)
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

                        vm.startMove(direction)

                        try {
                            tryAwaitRelease()
                        } finally {
                            vm.stopMove(direction)
                        }
                    }
                )
            },
        shape = RoundedCornerShape(22.dp),
        color = background,
        contentColor = foreground,
        tonalElevation = 3.dp
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                label,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black
            )
        }
    }
}
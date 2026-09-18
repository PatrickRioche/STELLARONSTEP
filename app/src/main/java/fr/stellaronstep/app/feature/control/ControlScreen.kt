package fr.stellaronstep.app.feature.control

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fr.stellaronstep.app.AppViewModel
import fr.stellaronstep.app.core.onstep.SlewDirection
import fr.stellaronstep.app.core.onstep.SlewRate
import fr.stellaronstep.app.ui.theme.Danger

@Composable
fun ControlScreen(vm: AppViewModel, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxSize().padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("CONTROL")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SlewRate.entries.forEach { rate -> OutlinedButton(onClick = { vm.setRate(rate) }) { Text(rate.label) } }
        }
        DirectionButton("N", SlewDirection.NORTH, vm)
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            DirectionButton("W", SlewDirection.WEST, vm)
            DirectionButton("E", SlewDirection.EAST, vm)
        }
        DirectionButton("S", SlewDirection.SOUTH, vm)
        Button(onClick = { vm.stopAll() }, modifier = Modifier.fillMaxWidth()) {
            Text("STOP GLOBAL", color = Danger)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = { vm.goHome() }) { Text("HOME") }
            OutlinedButton(onClick = { vm.park() }) { Text("PARK") }
            OutlinedButton(onClick = { vm.unpark() }) { Text("UNPARK") }
        }
    }
}

@Composable
private fun DirectionButton(label: String, direction: SlewDirection, vm: AppViewModel) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Button(onClick = { vm.startMove(direction) }) { Text("$label ▶") }
        OutlinedButton(onClick = { vm.stopMove(direction) }) { Text("■") }
    }
}

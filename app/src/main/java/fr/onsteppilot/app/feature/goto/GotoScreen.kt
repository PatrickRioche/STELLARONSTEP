package fr.onsteppilot.app.feature.goto

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fr.onsteppilot.app.AppViewModel

private data class Target(val name: String, val ra: String, val dec: String)

private val quickTargets = listOf(
    Target("Polaris", "02:31:49", "+89*15:51"),
    Target("Capella", "05:16:41", "+45*59:53"),
    Target("Vega", "18:36:56", "+38*47:01"),
    Target("M31", "00:42:44", "+41*16:09"),
    Target("M42", "05:35:17", "-05*23:28")
)

@Composable
fun GotoScreen(vm: AppViewModel, modifier: Modifier = Modifier) {
    var ra by remember { mutableStateOf("") }
    var dec by remember { mutableStateOf("") }
    Column(modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("GOTO / CIEL")
        Text("Base inspirée de l'écran Ciel de StellarPilot : sélection rapide puis GOTO direct OnStep.")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            quickTargets.take(3).forEach { target ->
                FilterChip(selected = false, onClick = { ra = target.ra; dec = target.dec }, label = { Text(target.name) })
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            quickTargets.drop(3).forEach { target ->
                FilterChip(selected = false, onClick = { ra = target.ra; dec = target.dec }, label = { Text(target.name) })
            }
        }
        OutlinedTextField(ra, { ra = it }, label = { Text("RA HH:MM:SS") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(dec, { dec = it }, label = { Text("DEC ±DD*MM:SS") }, modifier = Modifier.fillMaxWidth())
        Button(onClick = { vm.goto(ra, dec) }, enabled = ra.isNotBlank() && dec.isNotBlank() && !vm.busy, modifier = Modifier.fillMaxWidth()) {
            Text("GOTO")
        }
        vm.message?.let { Text(it) }
    }
}

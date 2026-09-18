package fr.stellaronstep.app.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import fr.stellaronstep.app.AppViewModel
import fr.stellaronstep.app.ui.theme.Accent
import fr.stellaronstep.app.ui.theme.Surface

@Composable
fun HomeScreen(vm: AppViewModel, modifier: Modifier = Modifier) {
    val s = vm.status
    Column(modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("STELLARONSTEP", color = Accent, fontWeight = FontWeight.Bold)
        Text("Monture OnStep", fontWeight = FontWeight.Bold)
        Card(colors = CardDefaults.cardColors(containerColor = Surface), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(if (s.connected) "● Connecté" else "○ Déconnecté")
                Text("RA  ${s.ra ?: "—"}")
                Text("DEC ${s.dec ?: "—"}")
                Text("Suivi: ${if (s.tracking) "ON" else "OFF"}   GOTO: ${if (s.slewing) "EN COURS" else "IDLE"}")
                Text("Park: ${if (s.parked) "OUI" else "NON"}   Home: ${if (s.atHome) "OUI" else "NON"}")
                if (s.pierSide != null) Text("Pier side: ${s.pierSide}")
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = { vm.refreshStatus() }, enabled = !vm.busy) { Text("Actualiser") }
            Button(onClick = { vm.tracking(!s.tracking) }, enabled = s.connected && !vm.busy) {
                Text(if (s.tracking) "Stop suivi" else "Suivi ON")
            }
        }
        Spacer(Modifier.height(4.dp))
        vm.message?.let { Text(it) }
    }
}

package fr.stellaronstep.app.feature.goto

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import fr.stellaronstep.app.AppViewModel
import fr.stellaronstep.app.ui.theme.Surface

private data class Target(
    val name: String,
    val ra: String,
    val dec: String
)

private val quickTargets = listOf(
    Target("Polaris", "02:31:49", "+89*15:51"),
    Target("Capella", "05:16:41", "+45*59:53"),
    Target("Vega", "18:36:56", "+38*47:01"),
    Target("M31", "00:42:44", "+41*16:09"),
    Target("M42", "05:35:17", "-05*23:28")
)

@Composable
fun GotoScreen(
    vm: AppViewModel,
    modifier: Modifier = Modifier
) {
    var ra by remember { mutableStateOf("") }
    var dec by remember { mutableStateOf("") }
    val s = vm.status

    Column(
        modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("GOTO / CIEL", fontWeight = FontWeight.Bold)

        Card(
            colors = CardDefaults.cardColors(containerColor = Surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(if (s.connected) "OnStepX : CONNECTE" else "OnStepX : DECONNECTE")
                Text("RA actuelle  : ${s.ra ?: "--"}")
                Text("DEC actuelle : ${s.dec ?: "--"}")
                Text("Suivi : ${if (s.tracking) "ON" else "OFF"}")
                Text("Park : ${if (s.parked) "OUI" else "NON"}")
            }
        }

        Text("Cibles rapides")

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            quickTargets.take(3).forEach { target ->
                FilterChip(
                    selected = ra == target.ra && dec == target.dec,
                    onClick = {
                        ra = target.ra
                        dec = target.dec
                    },
                    label = { Text(target.name) }
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            quickTargets.drop(3).forEach { target ->
                FilterChip(
                    selected = ra == target.ra && dec == target.dec,
                    onClick = {
                        ra = target.ra
                        dec = target.dec
                    },
                    label = { Text(target.name) }
                )
            }
        }

        OutlinedTextField(
            value = ra,
            onValueChange = { ra = it },
            label = { Text("RA HH:MM:SS") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = dec,
            onValueChange = { dec = it },
            label = { Text("DEC +DD*MM:SS") },
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = { vm.goto(ra, dec) },
            enabled =
                s.connected &&
                !s.parked &&
                ra.isNotBlank() &&
                dec.isNotBlank() &&
                !vm.busy,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("LANCER GOTO")
        }

        OutlinedButton(
            onClick = { vm.stopAll() },
            enabled = s.connected,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("STOP GOTO")
        }

        if (s.parked) {
            Text(
                "Monture parkee : faire UNPARK dans Control.",
                fontWeight = FontWeight.Bold
            )
        }

        vm.message?.let {
            Text(it, fontWeight = FontWeight.Bold)
        }
    }
}
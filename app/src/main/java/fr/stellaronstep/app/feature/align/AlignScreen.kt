package fr.stellaronstep.app.feature.align

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fr.stellaronstep.app.AppViewModel

@Composable
fun AlignScreen(vm: AppViewModel, modifier: Modifier = Modifier) {
    var stars by remember { mutableFloatStateOf(2f) }
    val count = stars.toInt().coerceIn(1, 6)
    Column(modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("ALIGN")
        Text("Nombre d'étoiles: $count")
        Slider(value = stars, onValueChange = { stars = it }, valueRange = 1f..6f, steps = 4)
        Button(onClick = { vm.startAlignment(count) }, modifier = Modifier.fillMaxWidth()) { Text("Démarrer l'alignement") }
        Text("Séquence: HOME → cible brillante → GOTO → centrage dans Control → accepter l'étoile.")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { vm.alignmentStatus() }) { Text("Statut") }
            Button(onClick = { vm.acceptAlignmentStar() }) { Text("Accepter étoile") }
        }
        OutlinedButton(onClick = { vm.saveAlignment() }, modifier = Modifier.fillMaxWidth()) { Text("Sauvegarder le modèle") }
        vm.message?.let { Text(it) }
    }
}

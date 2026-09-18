package fr.stellaronstep.app.feature.config

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fr.stellaronstep.app.AppViewModel
import fr.stellaronstep.app.core.onstep.OnStepConnectionConfig

@Composable
fun ConfigScreen(vm: AppViewModel, modifier: Modifier = Modifier) {
    var host by remember(vm.config.host) { mutableStateOf(vm.config.host) }
    var port by remember(vm.config.port) { mutableStateOf(vm.config.port.toString()) }
    var timeout by remember(vm.config.timeoutMs) { mutableStateOf(vm.config.timeoutMs.toString()) }

    Column(modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("CONFIG")
        Text("Connexion directe TCP vers OnStep / OnStepX")
        OutlinedTextField(host, { host = it }, label = { Text("Adresse IP") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(port, { port = it }, label = { Text("Port TCP") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(timeout, { timeout = it }, label = { Text("Timeout ms") }, modifier = Modifier.fillMaxWidth())
        Button(
            onClick = {
                vm.stopPolling()
                vm.updateConfig(OnStepConnectionConfig(host.trim(), port.toIntOrNull() ?: 9999, timeout.toIntOrNull() ?: 1800))
                vm.connectAndPoll()
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Enregistrer et reconnecter") }
        Text("Note: le port dépend du canal réseau activé dans ton OnStep. 9999 est seulement une valeur de départ à adapter.")
    }
}

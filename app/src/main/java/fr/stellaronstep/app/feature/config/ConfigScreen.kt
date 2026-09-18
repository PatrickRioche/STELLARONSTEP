package fr.stellaronstep.app.feature.config

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import fr.stellaronstep.app.core.onstep.OnStepConnectionConfig
import fr.stellaronstep.app.ui.theme.Surface

@Composable
fun ConfigScreen(
    vm: AppViewModel,
    modifier: Modifier = Modifier
) {
    var host by remember(vm.config.host) {
        mutableStateOf(vm.config.host)
    }

    var port by remember(vm.config.port) {
        mutableStateOf(vm.config.port.toString())
    }

    var timeout by remember(vm.config.timeoutMs) {
        mutableStateOf(vm.config.timeoutMs.toString())
    }

    val d = vm.diagnostics

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement =
            Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "CONFIG",
            fontWeight = FontWeight.Bold
        )

        Text("Connexion directe TCP vers OnStepX")

        OutlinedTextField(
            value = host,
            onValueChange = { host = it },
            label = { Text("Adresse IP") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = port,
            onValueChange = { port = it },
            label = { Text("Port TCP") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = timeout,
            onValueChange = { timeout = it },
            label = { Text("Timeout ms") },
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {
                vm.stopPolling()

                vm.updateConfig(
                    OnStepConnectionConfig(
                        host.trim(),
                        port.toIntOrNull() ?: 9999,
                        timeout.toIntOrNull() ?: 1800
                    )
                )

                vm.connectAndPoll()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Enregistrer et reconnecter")
        }

        Button(
            onClick = {
                vm.refreshDiagnostics()
            },
            enabled = !vm.busy,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("LIRE CONFIGURATION ONSTEPX")
        }

        Card(
            colors =
                CardDefaults.cardColors(
                    containerColor = Surface
                ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement =
                    Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    "Site / horloge OnStepX",
                    fontWeight = FontWeight.Bold
                )

                Text("Latitude        : ${d.latitude}")
                Text("Longitude OnStep: ${d.longitudeOnStep}")
                Text("Date            : ${d.date}")
                Text("Heure locale    : ${d.localTime}")
                Text("UTC offset      : ${d.utcOffset}")
                Text("Temps sideral   : ${d.siderealTime}")

                Text("")
                Text(
                    "Position / limites",
                    fontWeight = FontWeight.Bold
                )

                Text("Altitude        : ${d.altitude}")
                Text("Azimut          : ${d.azimuth}")
                Text("Horizon mini    : ${d.horizonLimit}")
                Text("Hauteur maxi    : ${d.overheadLimit}")
                Text("Pier side       : ${d.pierSide}")
                Text("GU brut         : ${d.rawStatus}")

                Text("")
                Text(
                    "Limites OnStepX etendues",
                    fontWeight = FontWeight.Bold
                )

                Text("Meridien Est min : ${d.eastPastMeridianMin}")
                Text("Meridien Ouest min: ${d.westPastMeridianMin}")
                Text("Axe 1 min deg    : ${d.axis1MinDeg}")
                Text("Axe 1 max deg    : ${d.axis1MaxDeg}")
                Text("Axe 2 min deg    : ${d.axis2MinDeg}")
                Text("Axe 2 max deg    : ${d.axis2MaxDeg}")
            }
        }

        Text(
            "Convention OnStep : la longitude retournee par Gg utilise Est negatif et Ouest positif."
        )

        Text(
            "Le code GOTO 6 correspond surtout aux limites meridien/axes/declinaison, pas uniquement au site."
        )

        vm.message?.let {
            Text(
                it,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
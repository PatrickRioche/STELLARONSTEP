package fr.stellaronstep.app.feature.home

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import fr.stellaronstep.app.AppViewModel
import fr.stellaronstep.app.BuildConfig
import fr.stellaronstep.app.R
import fr.stellaronstep.app.ui.theme.AccentOrange
import fr.stellaronstep.app.ui.theme.Muted
import fr.stellaronstep.app.ui.theme.Success
import fr.stellaronstep.app.ui.theme.Surface

@Composable
fun HomeScreen(
    vm: AppViewModel,
    modifier: Modifier = Modifier
) {
    val s = vm.status
    val context = LocalContext.current

    val locationPermissionLauncher =
        rememberLauncherForActivityResult(
            contract =
                ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions: Map<String, Boolean> ->
            val granted =
                permissions[
                    Manifest.permission.ACCESS_FINE_LOCATION
                ] == true ||
                    permissions[
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ] == true

            if (granted) {
                vm.initializePreferredFromPhone()
            } else {
                vm.syncPhoneClock()
            }
        }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Image(
            painter = painterResource(R.drawable.stellaronstep_logo),
            contentDescription = "Logo StellarOnStep",
            modifier = Modifier
                .fillMaxWidth()
                .height(112.dp),
            contentScale = ContentScale.Fit
        )

        Card(
            colors = CardDefaults.cardColors(
                containerColor = Surface
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(11.dp)
                            .background(
                                color = if (s.connected) {
                                    Success
                                } else {
                                    MaterialTheme.colorScheme.error
                                },
                                shape = CircleShape
                            )
                    )

                    Text(
                        if (s.connected) {
                            "OnStepX connecte"
                        } else {
                            "OnStepX deconnecte"
                        },
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                Text(
                    "${vm.config.host}:${vm.config.port}",
                    color = Muted,
                    style = MaterialTheme.typography.bodySmall
                )

                Text(
                    "StellarOnStep v${BuildConfig.VERSION_NAME}",
                    color = AccentOrange,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Card(
            colors = CardDefaults.cardColors(
                containerColor = Surface
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "DEMARRAGE ONSTEPX",
                    color = Muted,
                    style = MaterialTheme.typography.labelLarge
                )

                Text(
                    "Sur un telephone avec GPS, initialiser OnStepX depuis le telephone avant RESET HOME.",
                    style = MaterialTheme.typography.bodyMedium
                )

                Button(
                    onClick = {
                        val fine =
                            context.checkSelfPermission(
                                Manifest.permission.ACCESS_FINE_LOCATION
                            ) == PackageManager.PERMISSION_GRANTED

                        val coarse =
                            context.checkSelfPermission(
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            ) == PackageManager.PERMISSION_GRANTED

                        if (fine || coarse) {
                            vm.initializePreferredFromPhone()
                        } else {
                            locationPermissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        }
                    },
                    enabled = s.connected && !vm.busy,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("INITIALISER DEPUIS LE TELEPHONE")
                }

                Text(
                    "GPS disponible : position + date/heure/fuseau. GPS indisponible : date/heure/fuseau uniquement.",
                    color = Muted,
                    style = MaterialTheme.typography.bodySmall
                )

                Text(
                    "Etape suivante : placer physiquement la monture en HOME, puis utiliser RESET HOME dans Control.",
                    color = AccentOrange,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        Card(
            colors = CardDefaults.cardColors(
                containerColor = Surface
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "POSITION MONTURE",
                    color = Muted,
                    style = MaterialTheme.typography.labelLarge
                )

                Text(
                    "RA   ${s.ra ?: "--"}",
                    style = MaterialTheme.typography.titleMedium
                )

                Text(
                    "DEC  ${s.dec ?: "--"}",
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(Modifier.height(2.dp))

                val trackingLabel =
                    when {
                        !s.tracking ->
                            "OFF"

                        s.raw.contains('(') ->
                            "LUNAIRE"

                        s.raw.contains('O') ->
                            "SOLAIRE"

                        s.raw.contains('k') ->
                            "KING"

                        else ->
                            "SIDERAL"
                    }

                Text("Suivi : $trackingLabel")
                Text("GOTO : ${if (s.slewing) "EN COURS" else "IDLE"}")

                Text(
                    "Parking : ${
                        when {
                            s.parking ->
                                "PARK EN COURS"

                            s.parkFailed ->
                                "ECHEC PARK"

                            s.parked ->
                                "PARKEE - UNPARK requis"

                            s.connected ->
                                "NON PARKEE"

                            else ->
                                "--"
                        }
                    }"
                )

                Text("Home : ${if (s.atHome) "OUI" else "NON"}")

                if (
                    s.connected &&
                    !s.parked &&
                    s.atHome
                ) {
                    Text(
                        "Monture au HOME - etat NON PARKE.",
                        color = AccentOrange,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                if (s.pierSide != null) {
                    Text("Pier side : ${s.pierSide}")
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = { vm.refreshStatus() },
                enabled = !vm.busy,
                modifier = Modifier.weight(1f)
            ) {
                Text("Actualiser")
            }

            Button(
                onClick = { vm.tracking(!s.tracking) },
                enabled = s.connected && !vm.busy,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    if (s.tracking) {
                        "Stop suivi"
                    } else {
                        "Suivi ON"
                    }
                )
            }
        }

        vm.message?.let {
            Text(
                it,
                color = Muted
            )
        }
    }
}
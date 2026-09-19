package fr.stellaronstep.app.feature.goto

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.stellaronstep.app.AppViewModel
import fr.stellaronstep.app.ui.theme.AccentOrange
import fr.stellaronstep.app.ui.theme.Danger
import fr.stellaronstep.app.ui.theme.Muted
import fr.stellaronstep.app.ui.theme.Success
import fr.stellaronstep.app.ui.theme.Surface
import java.util.Locale

private val categories =
    listOf(
        "all" to "Tout",
        "star" to "Étoiles",
        "galaxy" to "Galaxies",
        "nebula" to "Nébuleuses",
        "cluster" to "Amas"
    )

@Composable
fun GotoScreen(
    vm: AppViewModel,
    modifier: Modifier = Modifier,
    gotoVm: GotoViewModel = viewModel()
) {
    var mode by rememberSaveable { mutableStateOf("catalog") }
    var search by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf("all") }
    var minAltitude by rememberSaveable { mutableStateOf(15.0) }
    var direction by rememberSaveable { mutableStateOf<String?>(null) }
    var maxMagnitude by rememberSaveable { mutableStateOf<Double?>(null) }
    var selectedConstellations by remember { mutableStateOf(emptySet<String>()) }
    var showConstellations by rememberSaveable { mutableStateOf(false) }
    var sort by rememberSaveable { mutableStateOf("magnitude") }
    var order by rememberSaveable { mutableStateOf("asc") }
    var ra by rememberSaveable { mutableStateOf("00:42:44") }
    var dec by rememberSaveable { mutableStateOf("+41*16:09") }
    var pendingSolar by remember { mutableStateOf<SkyTarget?>(null) }

    fun currentQuery(offset: Int = 0) =
        CatalogQuery(
            category = category,
            text = search,
            minAltitudeDeg = minAltitude,
            direction = direction,
            constellations = selectedConstellations,
            maxMagnitude = maxMagnitude,
            sort = sort,
            order = order,
            offset = offset,
            limit = 30
        )

    fun reload(offset: Int = 0) {
        gotoVm.loadCatalog(currentQuery(offset))
    }

    LaunchedEffect(gotoVm.observer) {
        if (gotoVm.observer != null) {
            if (gotoVm.catalogResult == null) reload()
            if (gotoVm.solarTargets.isEmpty()) gotoVm.loadSolarSystem()
        }
    }

    if (showConstellations) {
        ConstellationDialog(
            selected = selectedConstellations,
            onDismiss = { showConstellations = false },
            onApply = {
                selectedConstellations = it
                showConstellations = false
                reload()
            }
        )
    }

    pendingSolar?.let { target ->
        AlertDialog(
            onDismissRequest = { pendingSolar = null },
            title = { Text("⚠ DANGER — OBSERVATION SOLAIRE") },
            text = {
                Text(
                    "Ne jamais pointer ou observer le Soleil sans filtre solaire astronomique adapté placé à l'ouverture de l'instrument. Confirmer uniquement si l'installation est sécurisée."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        pendingSolar = null
                        gotoVm.gotoTarget(target)
                    }
                ) {
                    Text("JE CONFIRME — GOTO SOLEIL")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingSolar = null }) {
                    Text("Annuler")
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "CIEL & GOTO",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        ObserverCard(vm, gotoVm)

        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ModeChip(mode == "catalog", "Catalogue du ciel") {
                mode = "catalog"
                reload()
            }
            ModeChip(mode == "solar", "Système solaire") {
                mode = "solar"
                gotoVm.loadSolarSystem()
            }
            ModeChip(mode == "manual", "Coordonnées") {
                mode = "manual"
            }
        }

        gotoVm.selectedTarget?.let { target ->
            SelectedTargetCard(
                target = target,
                motion = gotoVm.gotoState,
                onGoto = {
                    if (target.solarWarning) {
                        pendingSolar = target
                    } else {
                        gotoVm.gotoTarget(target)
                    }
                },
                onStop = gotoVm::stopGoto
            )
        }

        when (mode) {
            "solar" ->
                SolarPanel(
                    targets = gotoVm.solarTargets,
                    selected = gotoVm.selectedTarget,
                    loading = gotoVm.loading,
                    onRefresh = gotoVm::loadSolarSystem,
                    onSelect = gotoVm::select
                )

            "manual" ->
                ManualPanel(
                    ra = ra,
                    dec = dec,
                    onRa = { ra = it },
                    onDec = { dec = it },
                    onGoto = { gotoVm.gotoCoordinates(ra, dec) },
                    onStop = gotoVm::stopGoto
                )

            else ->
                CatalogPanel(
                    gotoVm = gotoVm,
                    search = search,
                    onSearch = { search = it },
                    category = category,
                    onCategory = {
                        category = it
                        reload()
                    },
                    minAltitude = minAltitude,
                    onMinAltitude = {
                        minAltitude = it
                        reload()
                    },
                    direction = direction,
                    onDirection = {
                        direction = it
                        reload()
                    },
                    maxMagnitude = maxMagnitude,
                    onMaxMagnitude = {
                        maxMagnitude = it
                        reload()
                    },
                    selectedConstellations = selectedConstellations,
                    onConstellations = { showConstellations = true },
                    sort = sort,
                    order = order,
                    onSort = { id ->
                        if (sort == id) {
                            order = if (order == "asc") "desc" else "asc"
                        } else {
                            sort = id
                            order = if (id == "altitude") "desc" else "asc"
                        }
                        reload()
                    },
                    onSearchClick = { reload() },
                    onPage = { reload(it) },
                    onSelect = gotoVm::select
                )
        }

        gotoVm.error?.let {
            Text(it, color = Danger, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ObserverCard(
    vm: AppViewModel,
    gotoVm: GotoViewModel
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(
                if (vm.status.connected) "OnStepX connecté" else "OnStepX déconnecté",
                color = if (vm.status.connected) Success else Danger,
                fontWeight = FontWeight.Bold
            )

            val observer = gotoVm.observer
            if (observer != null) {
                Text(
                    "Observateur : %.5f°, %.5f° • %s".format(
                        Locale.US,
                        observer.latitude,
                        observer.longitude,
                        observer.source
                    ),
                    color = Muted
                )
            } else {
                Text(
                    "Position observateur indisponible",
                    color = Danger
                )
            }

            OutlinedButton(
                onClick = gotoVm::refreshObserver,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("ACTUALISER LA POSITION")
            }
        }
    }
}

@Composable
private fun ModeChip(
    selected: Boolean,
    label: String,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) }
    )
}

@Composable
private fun CatalogPanel(
    gotoVm: GotoViewModel,
    search: String,
    onSearch: (String) -> Unit,
    category: String,
    onCategory: (String) -> Unit,
    minAltitude: Double,
    onMinAltitude: (Double) -> Unit,
    direction: String?,
    onDirection: (String?) -> Unit,
    maxMagnitude: Double?,
    onMaxMagnitude: (Double?) -> Unit,
    selectedConstellations: Set<String>,
    onConstellations: () -> Unit,
    sort: String,
    order: String,
    onSort: (String) -> Unit,
    onSearchClick: () -> Unit,
    onPage: (Int) -> Unit,
    onSelect: (SkyTarget) -> Unit
) {
    OutlinedTextField(
        value = search,
        onValueChange = onSearch,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        label = { Text("Rechercher un objet") },
        placeholder = { Text("Vega, Alpha Lyr, HIP 91262, M31, NGC 7000...") }
    )

    Button(
        onClick = onSearchClick,
        enabled = !gotoVm.loading,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("RECHERCHER")
    }

    ChipSection("CATÉGORIE") {
        categories.forEach { item ->
            FilterChip(
                selected = category == item.first,
                onClick = { onCategory(item.first) },
                label = { Text(item.second) }
            )
        }
    }

    ChipSection("HAUTEUR MINIMALE") {
        listOf(15.0, 30.0, 45.0, 60.0).forEach { value ->
            FilterChip(
                selected = minAltitude == value,
                onClick = { onMinAltitude(value) },
                label = { Text("${value.toInt()}°+") }
            )
        }
    }

    ChipSection("DIRECTION") {
        listOf<String?>(null, "N", "NE", "E", "SE", "S", "SW", "W", "NW")
            .forEach { value ->
                FilterChip(
                    selected = direction == value,
                    onClick = { onDirection(value) },
                    label = { Text(value ?: "Toutes") }
                )
            }
    }

    ChipSection("MAGNITUDE MAX") {
        listOf<Double?>(null, 2.0, 4.0, 6.0, 8.0, 10.0)
            .forEach { value ->
                FilterChip(
                    selected = maxMagnitude == value,
                    onClick = { onMaxMagnitude(value) },
                    label = { Text(value?.let { "≤ ${it.toInt()}" } ?: "Toutes") }
                )
            }
    }

    OutlinedButton(
        onClick = onConstellations,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            if (selectedConstellations.isEmpty()) {
                "Toutes les constellations"
            } else {
                "${selectedConstellations.size} constellation(s)"
            }
        )
    }

    ChipSection("TRIER PAR") {
        listOf(
            "magnitude" to "Magnitude",
            "altitude" to "Hauteur",
            "name" to "Nom"
        ).forEach { option ->
            val selected = sort == option.first
            FilterChip(
                selected = selected,
                onClick = { onSort(option.first) },
                label = {
                    Text(
                        if (selected) {
                            option.second + if (order == "asc") " ↑" else " ↓"
                        } else {
                            option.second
                        }
                    )
                }
            )
        }
    }

    if (gotoVm.loading) {
        CircularProgressIndicator()
        Text("Calcul des objets visibles...", color = Muted)
        return
    }

    val result = gotoVm.catalogResult ?: return
    val total =
        if (result.explicitSearch) result.matchedCount else result.visibleCount
    val first = if (result.returnedCount > 0) result.query.offset + 1 else 0
    val last = result.query.offset + result.returnedCount

    Text(
        if (result.explicitSearch) {
            "Résultats $first-$last / ${result.matchedCount} • ${result.visibleCount} au-dessus de ${result.query.minAltitudeDeg.toInt()}°"
        } else {
            "Objets $first-$last / ${result.visibleCount} visibles"
        },
        color = Muted
    )

    if (result.explicitSearch) {
        Text(
            "Une recherche explicite affiche aussi les objets trop bas ou sous l'horizon.",
            color = Muted,
            style = MaterialTheme.typography.bodySmall
        )
    }

    if (result.objects.isEmpty()) {
        Text(
            if (result.explicitSearch) {
                "Aucun objet du catalogue ne correspond à la recherche."
            } else {
                "Aucun objet visible ne correspond aux filtres."
            },
            color = Muted
        )
    } else {
        result.objects.forEach { target ->
            TargetRow(
                target = target,
                selected = gotoVm.selectedTarget?.id == target.id,
                onClick = { onSelect(target) }
            )
        }
    }

    if (total > result.query.limit) {
        val pageSize = result.query.limit
        val currentPage = result.query.offset / pageSize + 1
        val pageCount = maxOf(1, (total + pageSize - 1) / pageSize)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = {
                    onPage((result.query.offset - pageSize).coerceAtLeast(0))
                },
                enabled = result.query.offset > 0,
                modifier = Modifier.weight(1f)
            ) {
                Text("← Précédents")
            }

            Text(
                "$currentPage / $pageCount",
                modifier = Modifier.padding(top = 12.dp),
                color = Muted
            )

            Button(
                onClick = { onPage(result.query.offset + pageSize) },
                enabled =
                    result.query.offset + result.returnedCount < total,
                modifier = Modifier.weight(1f)
            ) {
                Text("Suivants →")
            }
        }
    }
}

@Composable
private fun ChipSection(
    title: String,
    content: @Composable () -> Unit
) {
    Text(title, color = AccentOrange, fontWeight = FontWeight.Bold)
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        content()
    }
}

@Composable
private fun SolarPanel(
    targets: List<SkyTarget>,
    selected: SkyTarget?,
    loading: Boolean,
    onRefresh: () -> Unit,
    onSelect: (SkyTarget) -> Unit
) {
    Text("SYSTÈME SOLAIRE", color = AccentOrange, fontWeight = FontWeight.Bold)
    Text("Positions calculées localement, sans Internet.", color = Muted)

    OutlinedButton(
        onClick = onRefresh,
        enabled = !loading,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("ACTUALISER LES ÉPHÉMÉRIDES")
    }

    if (loading) CircularProgressIndicator()

    targets.forEach { target ->
        TargetRow(
            target = target,
            selected = selected?.id == target.id,
            onClick = { onSelect(target) }
        )
    }
}

@Composable
private fun ManualPanel(
    ra: String,
    dec: String,
    onRa: (String) -> Unit,
    onDec: (String) -> Unit,
    onGoto: () -> Unit,
    onStop: () -> Unit
) {
    Text("COORDONNÉES MANUELLES", color = AccentOrange, fontWeight = FontWeight.Bold)

    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        listOf(
            Triple("Polaris", "02:31:49", "+89*15:51"),
            Triple("Vega", "18:36:56", "+38*47:01"),
            Triple("M31", "00:42:44", "+41*16:09"),
            Triple("M42", "05:35:17", "-05*23:28")
        ).forEach { item ->
            OutlinedButton(
                onClick = {
                    onRa(item.second)
                    onDec(item.third)
                }
            ) {
                Text(item.first)
            }
        }
    }

    OutlinedTextField(
        value = ra,
        onValueChange = onRa,
        label = { Text("Ascension droite HH:MM:SS") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
    OutlinedTextField(
        value = dec,
        onValueChange = onDec,
        label = { Text("Déclinaison +DD*MM:SS") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
    Button(onClick = onGoto, modifier = Modifier.fillMaxWidth()) {
        Text("LANCER GOTO")
    }
    OutlinedButton(onClick = onStop, modifier = Modifier.fillMaxWidth()) {
        Text("STOP GOTO")
    }
}

@Composable
private fun TargetRow(
    target: SkyTarget,
    selected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    if (selected) {
                        MaterialTheme.colorScheme.surfaceVariant
                    } else {
                        Surface
                    }
            )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    target.symbol ?: objectSymbol(target.objectType),
                    style = MaterialTheme.typography.titleLarge
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(target.name, fontWeight = FontWeight.Bold)
                    target.reference
                        ?.takeIf { it != target.name }
                        ?.let { Text(it, color = AccentOrange) }
                }
            }

            Text(
                buildString {
                    append(target.objectTypeLabel)
                    target.constellation?.let {
                        append(" • ")
                        append(it)
                    }
                },
                color = Muted
            )

            Text(
                "Alt. %.1f° • Az. %.1f° %s".format(
                    Locale.US,
                    target.altitudeDeg,
                    target.azimuthDeg,
                    target.azimuthDirection
                ),
                color = if (target.aboveHorizon) Muted else Danger
            )

            target.magnitude?.let {
                Text(
                    "Magnitude %.2f %s".format(
                        Locale.US,
                        it,
                        target.magnitudeBand ?: ""
                    ),
                    color = Muted
                )
            }

            if (!target.visible) {
                Text(
                    if (!target.aboveHorizon) {
                        "SOUS L'HORIZON"
                    } else {
                        "TROP BASSE POUR LE FILTRE"
                    },
                    color = AccentOrange,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun SelectedTargetCard(
    target: SkyTarget,
    motion: GotoMotionState,
    onGoto: () -> Unit,
    onStop: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text("CIBLE SÉLECTIONNÉE", color = AccentOrange, fontWeight = FontWeight.Bold)
            Text(
                "${target.symbol ?: objectSymbol(target.objectType)} ${target.name}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            target.reference?.let { Text(it, color = Muted) }

            Text(
                "AD %.4f h • Dec %+.4f°".format(
                    Locale.US,
                    target.raHours,
                    target.decDeg
                ),
                color = Muted
            )
            Text(
                "Alt. %.1f° • Az. %.1f° %s • suivi %s".format(
                    Locale.US,
                    target.altitudeDeg,
                    target.azimuthDeg,
                    target.azimuthDirection,
                    target.trackingMode.label
                ),
                color = Muted
            )

            if (target.majorAxisArcmin != null) {
                Text(
                    "Taille %.1f' × %s".format(
                        Locale.US,
                        target.majorAxisArcmin,
                        target.minorAxisArcmin?.let {
                            "%.1f'".format(Locale.US, it)
                        } ?: "—"
                    ),
                    color = Muted
                )
            }

            if (target.solarWarning) {
                Text(
                    "⚠ Soleil : filtre solaire obligatoire",
                    color = Danger,
                    fontWeight = FontWeight.Bold
                )
            }

            Button(
                onClick = onGoto,
                enabled = !motion.active && target.aboveHorizon,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    when {
                        !target.aboveHorizon -> "CIBLE SOUS L'HORIZON"
                        motion.active -> "POINTAGE EN COURS..."
                        else -> "POINTER LA CIBLE"
                    }
                )
            }

            if (motion.active) {
                OutlinedButton(
                    onClick = onStop,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("STOP GOTO")
                }
            }

            motion.message?.let {
                Text(
                    it,
                    color = if (motion.progress == 1.0) Success else AccentOrange,
                    fontWeight = FontWeight.Bold
                )
            }
            motion.error?.let {
                Text(it, color = Danger, fontWeight = FontWeight.Bold)
            }
            motion.progress?.let {
                Text(
                    "Progression %.0f %% • %s".format(
                        Locale.US,
                        it * 100.0,
                        motion.status ?: ""
                    ),
                    color = if (it >= 1.0) Success else AccentOrange
                )
            }
            if (motion.currentRaHours != null && motion.currentDecDeg != null) {
                Text(
                    "AD actuelle %.4f h • Dec %+.4f°".format(
                        Locale.US,
                        motion.currentRaHours,
                        motion.currentDecDeg
                    ),
                    color = Muted
                )
            }
        }
    }
}

@Composable
private fun ConstellationDialog(
    selected: Set<String>,
    onDismiss: () -> Unit,
    onApply: (Set<String>) -> Unit
) {
    var working by remember(selected) { mutableStateOf(selected) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Constellations") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 500.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { working = emptySet() }
                        .padding(vertical = 6.dp)
                ) {
                    Checkbox(checked = working.isEmpty(), onCheckedChange = null)
                    Text(
                        "Toutes les constellations",
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }

                HorizontalDivider()

                allConstellationsFr.forEach { name ->
                    val checked = name in working
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                working =
                                    if (checked) working - name else working + name
                            }
                            .padding(vertical = 2.dp)
                    ) {
                        Checkbox(
                            checked = checked,
                            onCheckedChange = { value ->
                                working =
                                    if (value) working + name else working - name
                            }
                        )
                        Text(name, modifier = Modifier.padding(top = 12.dp))
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onApply(working) }) {
                Text("Appliquer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}

private fun objectSymbol(objectType: String): String =
    when (objectType) {
        "star", "star_double" -> "★"
        "galaxy", "galaxy_pair", "galaxy_group" -> "◉"
        "cluster_open", "cluster_globular" -> "✦"
        "nebula_diffuse", "nebula_planetary", "nebula_dark",
        "supernova_remnant" -> "☁"
        "moon" -> "☾"
        "planet" -> "●"
        "sun" -> "☉"
        else -> "✧"
    }

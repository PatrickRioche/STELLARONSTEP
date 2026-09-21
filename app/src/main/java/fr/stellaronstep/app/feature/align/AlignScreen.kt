package fr.stellaronstep.app.feature.align

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.stellaronstep.app.AppViewModel
import fr.stellaronstep.app.core.onstep.SlewDirection
import fr.stellaronstep.app.core.onstep.SlewRate
import fr.stellaronstep.app.feature.goto.SkyTarget
import fr.stellaronstep.app.ui.theme.AccentOrange
import fr.stellaronstep.app.ui.theme.Danger
import fr.stellaronstep.app.ui.theme.Muted
import fr.stellaronstep.app.ui.theme.Success
import fr.stellaronstep.app.ui.theme.Surface
import fr.stellaronstep.app.ui.theme.SurfaceRaised
import java.util.Locale

@Composable
fun AlignScreen(
    vm: AppViewModel,
    modifier: Modifier = Modifier,
    onOpenControl: () -> Unit = {},
    alignVm: AlignViewModel = viewModel()
) {
    var requestedStars by
        rememberSaveable {
            mutableIntStateOf(3)
        }

    var showStartDialog by
        remember {
            mutableStateOf(false)
        }

    var selectedDirection by
        rememberSaveable {
            mutableStateOf("N")
        }

    val process =
        alignVm.process

    val maxSelectableStars =
        process.maxStars
            .takeIf { it > 0 }
            ?.coerceAtMost(6)
            ?: 6

    LaunchedEffect(maxSelectableStars) {
        requestedStars =
            requestedStars.coerceIn(
                1,
                maxSelectableStars
            )
    }

    LaunchedEffect(alignVm.candidates) {
        val directions =
            listOf(
                "N",
                "NE",
                "E",
                "SE",
                "S",
                "SW",
                "W",
                "NW"
            )

        if (
            alignVm.candidates.none {
                it.azimuthDirection ==
                    selectedDirection
            }
        ) {
            directions.firstOrNull { direction ->
                alignVm.candidates.any {
                    it.azimuthDirection ==
                        direction
                }
            }?.let {
                selectedDirection = it
            }
        }
    }

    val readyToStart =
        vm.status.connected &&
            !vm.status.parked &&
            vm.status.atHome &&
            !vm.status.slewing &&
            !process.active &&
            !alignVm.gotoActive

    if (showStartDialog) {
        AlertDialog(
            onDismissRequest = {
                showStartDialog = false
            },
            title = {
                Text("Démarrer l'alignement ?")
            },
            text = {
                Text(
                    "La monture doit être physiquement en position HOME polaire. " +
                        "Le démarrage initialise une nouvelle séquence d'alignement et active le suivi sidéral."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showStartDialog = false
                        alignVm.startAlignment(
                            requestedStars
                        )
                    }
                ) {
                    Text("DÉMARRER")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showStartDialog = false
                    }
                ) {
                    Text("Annuler")
                }
            }
        )
    }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(
                    rememberScrollState()
                )
                .navigationBarsPadding()
                .padding(
                    horizontal = 16.dp,
                    vertical = 12.dp
                ),
        verticalArrangement =
            Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "ALIGNEMENT",
            style =
                MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        SectionCard("MODÈLE ACTIF / MÉMOIRE NV") {
            Text(
                "NV = Non-Volatile : mémoire persistante d'OnStepX.",
                color = Muted
            )

            Text(
                if (alignVm.activeModelStars > 0) {
                    "Modèle actif : ${alignVm.activeModelStars} étoile(s)"
                } else {
                    "Modèle actif : aucun modèle multi-étoiles"
                },
                fontWeight = FontWeight.Bold,
                color =
                    if (alignVm.activeModelStars > 0) Success else Muted
            )

            alignVm.activeModelResult
                ?.let { model ->
                    PolarErrorSummary(
                        model = model,
                        observerLatitude =
                            alignVm.observer?.latitude
                    )
                }

            Text(
                if (alignVm.nvSavedThisSession) {
                    "NV : modèle sauvegardé pendant cette session"
                } else {
                    "NV : copie persistante non lisible séparément par le protocole OnStepX"
                },
                color =
                    if (alignVm.nvSavedThisSession) Success else AccentOrange,
                style = MaterialTheme.typography.bodySmall
            )

            Text(
                "Les valeurs affichées sont celles du modèle ACTIF appliqué aux GOTO. SAUVEGARDER écrit ce modèle en NV.",
                color = Muted,
                style = MaterialTheme.typography.bodySmall
            )

            OutlinedButton(
                onClick = alignVm::refresh,
                enabled = !alignVm.busy && !alignVm.gotoActive,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("ACTUALISER LE MODÈLE")
            }
        }

        SectionCard("ÉTAT ONSTEPX") {
            StatusLine(
                label = "Connexion",
                value =
                    if (vm.status.connected) {
                        "Connectée"
                    } else {
                        "Déconnectée"
                    },
                ok = vm.status.connected
            )

            StatusLine(
                label = "Position HOME",
                value =
                    if (vm.status.atHome) {
                        "Oui"
                    } else {
                        "Non"
                    },
                ok = vm.status.atHome
            )

            StatusLine(
                label = "Parking",
                value =
                    if (vm.status.parked) {
                        "Parquée"
                    } else {
                        "Libre"
                    },
                ok = !vm.status.parked
            )

            StatusLine(
                label = "Suivi",
                value =
                    if (vm.status.tracking) {
                        "Actif"
                    } else {
                        "Arrêté"
                    },
                ok =
                    vm.status.tracking ||
                        !process.active
            )

            Text(
                "AD ${vm.status.ra}  •  Dec ${vm.status.dec}",
                color = Muted,
                style =
                    MaterialTheme.typography.bodySmall
            )

            OutlinedButton(
                onClick = alignVm::refresh,
                enabled =
                    !alignVm.busy &&
                        !alignVm.gotoActive,
                modifier =
                    Modifier.fillMaxWidth()
            ) {
                Text("ACTUALISER L'ÉTAT ALIGN")
            }
        }

        SectionCard("POSITION OBSERVATEUR") {
            Text(
                "Choisir la position utilisée pour calculer les étoiles visibles.",
                color = Muted
            )

            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(
                            rememberScrollState()
                        ),
                horizontalArrangement =
                    Arrangement.spacedBy(8.dp)
            ) {
                AlignLocationSource.entries
                    .forEach { source ->
                        FilterChip(
                            selected =
                                alignVm.locationSource ==
                                    source,
                            onClick = {
                                alignVm.selectLocationSource(
                                    source
                                )
                            },
                            label = {
                                Text(source.label)
                            }
                        )
                    }
            }

            alignVm.observer
                ?.let { observer ->
                    Text(
                        "Source : ${observer.source}",
                        fontWeight = FontWeight.Bold,
                        color = Success
                    )

                    Text(
                        "Lat %.5f°  •  Lon %.5f°".format(
                            Locale.US,
                            observer.latitude,
                            observer.longitude
                        ),
                        color = Muted
                    )
                }
                ?: Text(
                    "Position non disponible",
                    color = Danger
                )

            OutlinedButton(
                onClick =
                    alignVm::loadCandidates,
                enabled =
                    !alignVm.loadingCandidates &&
                        !alignVm.gotoActive,
                modifier =
                    Modifier.fillMaxWidth()
            ) {
                Text("ACTUALISER LA POSITION ET LES ÉTOILES")
            }
        }

        SectionCard("MODÈLE D'ALIGNEMENT") {
            val modelText =
                when {
                    process.active ->
                        "Séquence en cours"

                    process.modelLevel >= 3 ->
                        "Modèle 3 étoiles ou plus"

                    process.modelLevel > 0 ->
                        "Modèle ${process.modelLevel} étoile(s)"

                    else ->
                        "Aucun modèle actif"
                }

            Text(
                modelText,
                fontWeight = FontWeight.Bold,
                color =
                    if (
                        process.active ||
                        process.modelLevel > 0
                    ) {
                        Success
                    } else {
                        Muted
                    }
            )

            if (process.active) {
                Text(
                    "Progression : étoile ${process.currentStar} / ${process.requestedStars} " +
                        "(${process.acceptedStars} validée(s))",
                    color = Muted
                )
            } else if (process.completed) {
                Text(
                    "Séquence terminée — le modèle peut être sauvegardé.",
                    color = Success
                )
            }

            if (
                process.rawProcess.isNotBlank()
            ) {
                Text(
                    "A?=${process.rawProcess}  •  GW=${process.rawGeneral.ifBlank { "?" }}",
                    color = Muted,
                    style =
                        MaterialTheme.typography.bodySmall
                )
            }
        }

        if (!process.active) {
            SectionCard("1. PRÉPARER ET DÉMARRER") {
                Text(
                    "Place physiquement la monture en HOME polaire avant de lancer la séquence.",
                    color = Muted
                )

                Text(
                    "Nombre d'étoiles",
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .horizontalScroll(
                                rememberScrollState()
                            ),
                    horizontalArrangement =
                        Arrangement.spacedBy(8.dp)
                ) {
                    (1..maxSelectableStars)
                        .forEach { count ->
                            FilterChip(
                                selected =
                                    requestedStars ==
                                        count,
                                onClick = {
                                    requestedStars =
                                        count
                                },
                                label = {
                                    Text(
                                        count.toString()
                                    )
                                }
                            )
                        }
                }

                Text(
                    "Pour un modèle multi-étoiles, répartir les étoiles dans le ciel et, si possible, de part et d'autre du méridien.",
                    color = Muted,
                    style =
                        MaterialTheme.typography.bodySmall
                )

                Button(
                    onClick = {
                        showStartDialog = true
                    },
                    enabled =
                        readyToStart &&
                            !alignVm.busy,
                    modifier =
                        Modifier.fillMaxWidth()
                ) {
                    Text(
                        if (
                            process.modelLevel > 0
                        ) {
                            "NOUVEL ALIGNEMENT"
                        } else {
                            "DÉMARRER L'ALIGNEMENT"
                        }
                    )
                }

                if (!readyToStart) {
                    Text(
                        startBlockReason(
                            vm = vm,
                            process = process,
                            gotoActive =
                                alignVm.gotoActive
                        ),
                        color = Danger,
                        style =
                            MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        if (process.active) {
            SectionCard(
                "2. ZONE DU CIEL ET ÉTOILE ${process.currentStar} / ${process.requestedStars}"
            ) {
                Text(
                    "Sélection locale : étoiles de magnitude < 3, hauteur ≥ 25°, classées par direction puis par luminosité.",
                    color = Muted
                )

                if (
                    process.requestedStars >= 2
                ) {
                    Text(
                        "Pour les étoiles suivantes, choisis de préférence un autre secteur du ciel afin d'améliorer la géométrie du modèle.",
                        color = Muted,
                        style =
                            MaterialTheme.typography.bodySmall
                    )
                }

                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .horizontalScroll(
                                rememberScrollState()
                            ),
                    horizontalArrangement =
                        Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "N" to "N",
                        "NE" to "NE",
                        "E" to "E",
                        "SE" to "SE",
                        "S" to "S",
                        "SW" to "SO",
                        "W" to "O",
                        "NW" to "NO"
                    ).forEach { item ->
                        val count =
                            alignVm.candidates.count {
                                it.azimuthDirection ==
                                    item.first
                            }

                        FilterChip(
                            selected =
                                selectedDirection ==
                                    item.first,
                            onClick = {
                                selectedDirection =
                                    item.first
                            },
                            label = {
                                Text(
                                    "${item.second} ($count)"
                                )
                            }
                        )
                    }
                }

                Text(
                    "ÉTOILES DISPONIBLES — ${
                        when (selectedDirection) {
                            "SW" -> "SO"
                            "W" -> "O"
                            "NW" -> "NO"
                            else -> selectedDirection
                        }
                    }",
                    fontWeight = FontWeight.Bold
                )

                if (
                    alignVm.loadingCandidates
                ) {
                    CircularProgressIndicator()
                    Text(
                        "Recherche des étoiles adaptées...",
                        color = Muted
                    )
                } else {
                    val directionalStars =
                        alignVm.candidates.filter {
                            it.azimuthDirection ==
                                selectedDirection
                        }

                    if (
                        directionalStars.isEmpty()
                    ) {
                        Text(
                            "Aucune étoile de magnitude < 3 et hauteur ≥ 25° dans ce secteur actuellement.",
                            color = Muted
                        )
                    } else {
                        directionalStars
                            .forEach { target ->
                                AlignmentTargetRow(
                                    target = target,
                                    selected =
                                        alignVm
                                            .selectedTarget
                                            ?.id ==
                                            target.id,
                                    enabled =
                                        !alignVm.gotoActive &&
                                            !alignVm.busy,
                                    onClick = {
                                        alignVm.selectTarget(
                                            target
                                        )
                                    }
                                )
                            }
                    }
                }

                OutlinedButton(
                    onClick =
                        alignVm::loadCandidates,
                    enabled =
                        !alignVm.loadingCandidates &&
                            !alignVm.gotoActive,
                    modifier =
                        Modifier.fillMaxWidth()
                ) {
                    Text(
                        "ACTUALISER LES ÉTOILES"
                    )
                }
            }

            alignVm.selectedTarget
                ?.let { target ->
                    SectionCard(
                        "3. GOTO ${target.name.uppercase()}"
                    ) {
                        TargetDetails(target)

                        if (
                            alignVm.gotoActive
                        ) {
                            CircularProgressIndicator()

                            Text(
                                "Pointage en cours...",
                                color = Muted
                            )

                            OutlinedButton(
                                onClick =
                                    alignVm::stopGoto,
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                            ) {
                                Text("STOP GOTO")
                            }
                        } else {
                            Button(
                                onClick =
                                    alignVm::gotoSelected,
                                enabled =
                                    !alignVm.busy,
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                            ) {
                                Text(
                                    if (
                                        alignVm
                                            .targetReadyForCentering
                                    ) {
                                        "REFAIRE LE GOTO"
                                    } else {
                                        "GOTO ÉTOILE"
                                    }
                                )
                            }
                        }
                    }

                    SectionCard(
                        "4. CENTRER ET VALIDER"
                    ) {
                        if (
                            alignVm
                                .targetReadyForCentering
                        ) {
                            Text(
                                "Le GOTO est terminé. Centre ${target.name}, puis valide.",
                                color = Success,
                                fontWeight = FontWeight.Bold
                            )

                            AlignControlStatusCard(vm)

                            Text(
                                "VITESSE",
                                color = Muted,
                                modifier = Modifier.fillMaxWidth(),
                                style = MaterialTheme.typography.labelLarge
                            )

                            AlignRateRow(
                                first = SlewRate.GUIDE,
                                second = SlewRate.CENTER,
                                vm = vm
                            )

                            AlignRateRow(
                                first = SlewRate.MOVE,
                                second = SlewRate.SLEW,
                                vm = vm
                            )

                            AlignRateButton(
                                rate = SlewRate.MAX,
                                vm = vm,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Text(
                                "Maintenir une direction pour déplacer la monture",
                                color = Muted,
                                style = MaterialTheme.typography.bodySmall
                            )

                            AlignCenterPad(vm)

                            Button(
                                onClick = alignVm::acceptCurrentStar,
                                enabled =
                                    !alignVm.gotoActive &&
                                        !alignVm.busy &&
                                        vm.movingDirection == null,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    "VALIDER LE CENTRAGE — ÉTOILE ${process.currentStar}"
                                )
                            }

                            OutlinedButton(
                                onClick = onOpenControl,
                                enabled = !alignVm.gotoActive,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("OUVRIR CONTROL")
                            }
                        } else {
                            Text(
                                "Les commandes de centrage apparaîtront ici automatiquement dès la fin du GOTO.",
                                color = Muted
                            )
                        }
                    }
                }
        }

        if (
            !process.active &&
            (
                process.completed ||
                    process.modelLevel > 0
                )
        ) {
            SectionCard("5. BILAN ET SAUVEGARDE") {
                if (
                    alignVm.starResults.isNotEmpty()
                ) {
                    Text(
                        "ÉCARTS DE CENTRAGE",
                        fontWeight = FontWeight.Bold
                    )

                    alignVm.starResults
                        .forEach { result ->
                            val correction =
                                result.correctionArcmin

                            Text(
                                buildString {
                                    append(
                                        "Étoile ${result.index} — ${result.name}"
                                    )

                                    append(
                                        " • ${
                                            directionFr(
                                                result.direction
                                            )
                                        }"
                                    )

                                    correction
                                        ?.let {
                                            append(
                                                " • écart ${
                                                    formatAngularError(
                                                        it
                                                    )
                                                }"
                                            )
                                        }
                                        ?: append(
                                            " • écart indisponible"
                                        )
                                },
                                color = Muted
                            )
                        }

                    val measured =
                        alignVm.starResults
                            .mapNotNull {
                                it.correctionArcmin
                            }

                    if (measured.isNotEmpty()) {
                        Text(
                            "Moyenne : ${
                                formatAngularError(
                                    measured.average()
                                )
                            }  •  Maximum : ${
                                formatAngularError(
                                    measured.maxOrNull()
                                        ?: 0.0
                                )
                            }",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                alignVm.modelResult
                    ?.let { model ->
                        Text(
                            "CORRECTION DU MODÈLE ONSTEPX",
                            fontWeight = FontWeight.Bold
                        )

                        PolarErrorSummary(
                            model = model,
                            observerLatitude =
                                alignVm.observer?.latitude
                        )
                    }

                Text(
                    "Les écarts de centrage correspondent au déplacement angulaire mesuré entre la fin du GOTO et la position centrée au moment de la validation.",
                    color = Muted,
                    style =
                        MaterialTheme.typography.bodySmall
                )

                Text(
                    "Sauvegarder écrit ensuite le modèle d'alignement dans la mémoire non volatile d'OnStepX.",
                    color = Muted
                )

                Button(
                    onClick = alignVm::saveModel,
                    enabled =
                        !alignVm.busy &&
                            !alignVm.gotoActive,
                    modifier =
                        Modifier.fillMaxWidth()
                ) {
                    Text(
                        "SAUVEGARDER LE MODÈLE"
                    )
                }
            }
        }

        alignVm.message
            ?.let {
                Text(
                    it,
                    color = Success,
                    fontWeight = FontWeight.Bold
                )
            }

        alignVm.error
            ?.let {
                Text(
                    it,
                    color = Danger,
                    fontWeight = FontWeight.Bold
                )
            }
    }
}

@Composable
private fun SectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        colors =
            CardDefaults.cardColors(
                containerColor = Surface
            ),
        modifier =
            Modifier.fillMaxWidth()
    ) {
        Column(
            modifier =
                Modifier.padding(14.dp),
            verticalArrangement =
                Arrangement.spacedBy(9.dp)
        ) {
            Text(
                title,
                fontWeight = FontWeight.Bold
            )

            content()
        }
    }
}

@Composable
private fun StatusLine(
    label: String,
    value: String,
    ok: Boolean
) {
    Row(
        modifier =
            Modifier.fillMaxWidth(),
        horizontalArrangement =
            Arrangement.SpaceBetween
    ) {
        Text(
            label,
            color = Muted
        )

        Text(
            value,
            color =
                if (ok) {
                    Success
                } else {
                    Danger
                },
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun AlignmentTargetRow(
    target: SkyTarget,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Card(
        colors =
            CardDefaults.cardColors(
                containerColor =
                    if (selected) {
                        SurfaceRaised
                    } else {
                        MaterialTheme
                            .colorScheme
                            .surface
                    }
            ),
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(
                    enabled = enabled,
                    onClick = onClick
                )
    ) {
        Column(
            modifier =
                Modifier.padding(12.dp),
            verticalArrangement =
                Arrangement.spacedBy(3.dp)
        ) {
            Text(
                target.name,
                style =
                    MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            target.reference
                ?.takeIf {
                    it != target.name
                }
                ?.let {
                    Text(
                        it,
                        color = Muted,
                        style =
                            MaterialTheme.typography.bodySmall
                    )
                }

            Text(
                buildString {
                    target.magnitude
                        ?.let {
                            append(
                                "Magnitude %.2f".format(
                                    Locale.US,
                                    it
                                )
                            )
                            append("  •  ")
                        }

                    append(
                        "Hauteur %.0f°".format(
                            Locale.US,
                            target.altitudeDeg
                        )
                    )

                    append(
                        "  •  Direction ${
                            when (
                                target.azimuthDirection
                            ) {
                                "SW" -> "SO"
                                "W" -> "O"
                                "NW" -> "NO"
                                else ->
                                    target.azimuthDirection
                            }
                        }"
                    )

                    target.constellation
                        ?.let {
                            append(
                                "  •  $it"
                            )
                        }
                },
                color = Muted,
                style =
                    MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun TargetDetails(
    target: SkyTarget
) {
    Text(
        target.reference
            ?.takeIf {
                it != target.name
            }
            ?.let {
                "${target.name} • $it"
            }
            ?: target.name,
        fontWeight = FontWeight.Bold
    )

    Text(
        "AD ${
            SkyTargetFormat.ra(
                target.raHours
            )
        }  •  Dec ${
            SkyTargetFormat.dec(
                target.decDeg
            )
        }",
        color = Muted
    )

    Text(
        "Altitude %.0f°  •  Azimut %.0f° (%s)".format(
            Locale.US,
            target.altitudeDeg,
            target.azimuthDeg,
            target.azimuthDirection
        ),
        color = Muted
    )
}

private fun startBlockReason(
    vm: AppViewModel,
    process: AlignProcessState,
    gotoActive: Boolean
): String =
    when {
        !vm.status.connected ->
            "Connexion OnStepX requise."

        vm.status.parked ->
            "Faire UNPARK avant l'alignement."

        vm.status.slewing ->
            "Attendre la fin du mouvement."

        !vm.status.atHome ->
            "La monture doit être en position HOME polaire."

        process.active ->
            "Une séquence d'alignement est déjà en cours."

        gotoActive ->
            "Attendre la fin du GOTO."

        else ->
            "Monture non prête."
    }

@Composable
private fun AlignControlStatusCard(
    vm: AppViewModel
) {
    val status = vm.status

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
                        color = if (status.connected) Success else Danger,
                        shape = androidx.compose.foundation.shape.CircleShape
                    )
            )

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    if (status.connected) {
                        "OnStepX connecté"
                    } else {
                        "OnStepX déconnecté"
                    },
                    fontWeight = FontWeight.Bold
                )

                Text(
                    "AD ${status.ra} • Dec ${status.dec}",
                    color = Muted,
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
}

@Composable
private fun AlignRateRow(
    first: SlewRate,
    second: SlewRate,
    vm: AppViewModel
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        AlignRateButton(
            rate = first,
            vm = vm,
            modifier = Modifier.weight(1f)
        )

        AlignRateButton(
            rate = second,
            vm = vm,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun AlignRateButton(
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
private fun AlignCenterPad(
    vm: AppViewModel
) {
    val enabled =
        vm.status.connected &&
            !vm.status.parked

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        AlignHoldDirectionButton(
            label = "N",
            direction = SlewDirection.NORTH,
            vm = vm,
            enabled = enabled
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            AlignHoldDirectionButton(
                label = "W",
                direction = SlewDirection.WEST,
                vm = vm,
                enabled = enabled
            )

            Button(
                onClick = { vm.stopAll() },
                modifier = Modifier.size(86.dp),
                shape = androidx.compose.foundation.shape.CircleShape
            ) {
                Text(
                    "STOP",
                    color = Danger,
                    fontWeight = FontWeight.Black
                )
            }

            AlignHoldDirectionButton(
                label = "E",
                direction = SlewDirection.EAST,
                vm = vm,
                enabled = enabled
            )
        }

        AlignHoldDirectionButton(
            label = "S",
            direction = SlewDirection.SOUTH,
            vm = vm,
            enabled = enabled
        )
    }
}

@Composable
private fun AlignHoldDirectionButton(
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

@Composable
private fun PolarErrorSummary(
    model: AlignModelResult,
    observerLatitude: Double?
) {
    val alt =
        model.altitudeCorrectionArcmin

    val az =
        model.azimuthCorrectionArcmin

    if (alt == null && az == null) {
        Text(
            "Erreur polaire : indisponible",
            color = Muted
        )
        return
    }

    Text(
        "ERREUR POLAIRE",
        fontWeight = FontWeight.Bold
    )

    alt?.let { value ->
        val amount =
            formatAngularError(
                kotlin.math.abs(value)
            )

        Text(
            when {
                value > 0.0 ->
                    "Altitude : $amount trop haut"

                value < 0.0 ->
                    "Altitude : $amount trop bas"

                else ->
                    "Altitude : correcte"
            },
            color = Muted
        )
    }

    az?.let { value ->
        val amount =
            formatAngularError(
                kotlin.math.abs(value)
            )

        val northHemisphere =
            observerLatitude == null ||
                observerLatitude >= 0.0

        val errorDirection =
            when {
                value == 0.0 ->
                    null

                northHemisphere && value > 0.0 ->
                    "Ouest"

                northHemisphere && value < 0.0 ->
                    "Est"

                !northHemisphere && value > 0.0 ->
                    "Est"

                else ->
                    "Ouest"
            }

        Text(
            if (errorDirection == null) {
                "Azimut : correct"
            } else {
                "Azimut : $amount vers l'$errorDirection"
            },
            color = Muted
        )
    }

    Text(
        "CORRECTION À EFFECTUER",
        fontWeight = FontWeight.Bold
    )

    alt?.let { value ->
        Text(
            when {
                value > 0.0 ->
                    "↓ Baisser l'altitude de ${
                        formatAngularError(
                            kotlin.math.abs(value)
                        )
                    }"

                value < 0.0 ->
                    "↑ Monter l'altitude de ${
                        formatAngularError(
                            kotlin.math.abs(value)
                        )
                    }"

                else ->
                    "Altitude : aucune correction"
            },
            color =
                if (value == 0.0) {
                    Success
                } else {
                    AccentOrange
                }
        )
    }

    az?.let { value ->
        val amount =
            formatAngularError(
                kotlin.math.abs(value)
            )

        val northHemisphere =
            observerLatitude == null ||
                observerLatitude >= 0.0

        val correctionDirection =
            when {
                value == 0.0 ->
                    null

                northHemisphere && value > 0.0 ->
                    "Est"

                northHemisphere && value < 0.0 ->
                    "Ouest"

                !northHemisphere && value > 0.0 ->
                    "Ouest"

                else ->
                    "Est"
            }

        Text(
            if (correctionDirection == null) {
                "Azimut : aucune correction"
            } else {
                "Déplacer l'axe polaire vers l'$correctionDirection de $amount"
            },
            color =
                if (correctionDirection == null) {
                    Success
                } else {
                    AccentOrange
                }
        )
    }

    if (observerLatitude == null) {
        Text(
            "Est/Ouest calculé par défaut pour l'hémisphère Nord tant que la position observateur n'est pas disponible.",
            color = Muted,
            style =
                MaterialTheme.typography.bodySmall
        )
    }
}

private fun directionFr(
    direction: String
): String =
    when (direction) {
        "SW" -> "SO"
        "W" -> "O"
        "NW" -> "NO"
        else -> direction
    }

private fun formatAngularError(
    arcmin: Double
): String {
    val totalArcsec =
        kotlin.math.abs(
            arcmin * 60.0
        )

    return if (totalArcsec < 60.0) {
        "%.0f″".format(
            Locale.US,
            totalArcsec
        )
    } else {
        "%d′ %02.0f″".format(
            Locale.US,
            (totalArcsec / 60.0)
                .toInt(),
            totalArcsec % 60.0
        )
    }
}

private fun formatSignedArcmin(
    arcmin: Double
): String {
    val sign =
        if (arcmin > 0.0) {
            "+"
        } else if (arcmin < 0.0) {
            "−"
        } else {
            ""
        }

    return sign +
        formatAngularError(
            kotlin.math.abs(
                arcmin
            )
        )
}

private object SkyTargetFormat {
    fun ra(hours: Double): String {
        var totalSeconds =
            kotlin.math.round(
                hours * 3600.0
            ).toInt()

        totalSeconds =
            ((totalSeconds % 86400) + 86400) %
                86400

        val h =
            totalSeconds / 3600

        val m =
            (totalSeconds % 3600) / 60

        val s =
            totalSeconds % 60

        return "%02d:%02d:%02d".format(
            h,
            m,
            s
        )
    }

    fun dec(degrees: Double): String {
        val sign =
            if (degrees < 0.0) "-" else "+"

        val totalSeconds =
            kotlin.math.round(
                kotlin.math.abs(degrees) *
                    3600.0
            ).toInt()

        val d =
            totalSeconds / 3600

        val m =
            (totalSeconds % 3600) / 60

        val s =
            totalSeconds % 60

        return "%s%02d*%02d:%02d".format(
            sign,
            d,
            m,
            s
        )
    }
}

package fr.stellaronstep.app.feature.goto

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GotoCatalogRepository(
    private val context: Context
) {
    private data class BaseTarget(
        val id: String,
        val name: String,
        val reference: String?,
        val objectType: String,
        val typeLabel: String,
        val constellation: String?,
        val raHours: Double,
        val decDeg: Double,
        val magnitude: Double?,
        val magnitudeBand: String?,
        val majorAxis: Double?,
        val minorAxis: Double?,
        val aliases: String?,
        val searchText: String,
        val source: String
    )

    private data class IauInfo(
        val aliases: String,
        val constellation: String?,
        val reference: String?
    )

    @Volatile
    private var cache: List<BaseTarget>? = null

    suspend fun search(
        observer: ObserverLocation,
        query: CatalogQuery
    ): CatalogResult =
        withContext(Dispatchers.Default) {
            val all = loadCatalog()
            val explicit = query.text.trim().isNotEmpty()
            val search = SkyMath.foldText(query.text)

            val categoryTypes =
                when (query.category) {
                    "star" -> setOf("star", "star_double")
                    "galaxy" -> setOf("galaxy", "galaxy_pair", "galaxy_group")
                    "nebula" -> setOf(
                        "nebula_diffuse",
                        "nebula_planetary",
                        "nebula_dark",
                        "supernova_remnant"
                    )
                    "cluster" -> setOf("cluster_open", "cluster_globular")
                    else -> emptySet()
                }

            val now = System.currentTimeMillis()

            val computed =
                all.asSequence()
                    .filter { categoryTypes.isEmpty() || it.objectType in categoryTypes }
                    .filter { !explicit || it.searchText.contains(search) }
                    .filter {
                        query.constellations.isEmpty() ||
                            it.constellation in query.constellations
                    }
                    .filter {
                        query.maxMagnitude == null ||
                            it.magnitude == null ||
                            it.magnitude <= query.maxMagnitude
                    }
                    .map { base ->
                        val horizontal =
                            SkyMath.horizontal(
                                raHours = base.raHours,
                                decDeg = base.decDeg,
                                latitudeDeg = observer.latitude,
                                longitudeDeg = observer.longitude,
                                epochMillis = now
                            )

                        SkyTarget(
                            id = base.id,
                            name = base.name,
                            reference = base.reference,
                            objectType = base.objectType,
                            objectTypeLabel = base.typeLabel,
                            constellation = base.constellation,
                            raHours = base.raHours,
                            decDeg = base.decDeg,
                            magnitude = base.magnitude,
                            magnitudeBand = base.magnitudeBand,
                            majorAxisArcmin = base.majorAxis,
                            minorAxisArcmin = base.minorAxis,
                            aliases = base.aliases,
                            altitudeDeg = horizontal.altitudeDeg,
                            azimuthDeg = horizontal.azimuthDeg,
                            azimuthDirection = horizontal.direction,
                            visible = horizontal.altitudeDeg >= query.minAltitudeDeg,
                            aboveHorizon = horizontal.altitudeDeg > 0.0,
                            source = base.source
                        )
                    }
                    .filter {
                        query.direction == null ||
                            it.azimuthDirection == query.direction
                    }
                    .toList()

            val visibleCount = computed.count { it.visible }
            val filtered = if (explicit) computed else computed.filter { it.visible }

            val sorted =
                when (query.sort.lowercase()) {
                    "altitude" -> filtered.sortedBy { it.altitudeDeg }
                    "name" -> filtered.sortedBy { SkyMath.foldText(it.name) }
                    else ->
                        filtered.sortedWith(
                            compareBy<SkyTarget> {
                                it.magnitude ?: Double.POSITIVE_INFINITY
                            }.thenByDescending {
                                it.altitudeDeg
                            }.thenBy {
                                SkyMath.foldText(it.name)
                            }
                        )
                }.let {
                    if (query.order == "desc") it.reversed() else it
                }

            val safeOffset = query.offset.coerceAtLeast(0)
            val safeLimit = query.limit.coerceIn(1, 100)
            val page = sorted.drop(safeOffset).take(safeLimit)

            CatalogResult(
                query = query.copy(offset = safeOffset, limit = safeLimit),
                explicitSearch = explicit,
                matchedCount = sorted.size,
                visibleCount = visibleCount,
                returnedCount = page.size,
                objects = page
            )
        }

    private fun loadCatalog(): List<BaseTarget> {
        cache?.let { return it }

        synchronized(this) {
            cache?.let { return it }

            val iau = loadIauInfo()
            val objects = mutableListOf<BaseTarget>()
            objects += loadOpenNgc("catalog/openngc/NGC.csv")
            objects += loadOpenNgc("catalog/openngc/addendum.csv")
            objects += loadSkyMapStars(iau)

            cache = objects
            return objects
        }
    }

    private fun loadOpenNgc(asset: String): List<BaseTarget> =
        readCsv(asset, ';').mapNotNull { row ->
            val sourceType = row["Type"].orEmpty().trim()
            if (sourceType == "NonEx" || sourceType == "Dup") {
                return@mapNotNull null
            }

            val ra = parseRa(row["RA"]) ?: return@mapNotNull null
            val dec = parseDec(row["Dec"]) ?: return@mapNotNull null
            val canonical = normalizeCatalogName(row["Name"]) ?: return@mapNotNull null
            val messier = normalizeMessier(row["M"])
            val ngc = normalizeCatalogName(row["NGC"])
            val ic = normalizeCatalogName(row["IC"])
            val reference = messier ?: ngc ?: ic ?: canonical

            val common = row["Common names"].orEmpty().trim()
            val commonFirst = common.split(',').firstOrNull()?.trim().orEmpty()

            val display =
                frenchObjectNames[reference]
                    ?: frenchObjectNames[canonical]
                    ?: messier
                    ?: commonFirst.ifBlank { null }
                    ?: canonical

            val typePair =
                openNgcTypes[sourceType]
                    ?: ("unknown" to (sourceType.ifBlank { "Objet céleste" }))

            val constellationCode = row["Const"].orEmpty().trim()
            val constellation =
                constellationFr[constellationCode]
                    ?: constellationCode.ifBlank { null }

            val vMag = row["V-Mag"]?.trim()?.toDoubleOrNull()
            val bMag = row["B-Mag"]?.trim()?.toDoubleOrNull()
            val magnitude = vMag ?: bMag
            val magnitudeBand =
                when {
                    vMag != null -> "V"
                    bMag != null -> "B"
                    else -> null
                }

            val identifiers = row["Identifiers"].orEmpty().trim()
            val aliases =
                listOfNotNull(
                    common.ifBlank { null },
                    identifiers.ifBlank { null },
                    messier,
                    ngc,
                    ic,
                    canonical
                ).distinct().joinToString(" | ")

            val searchText =
                SkyMath.foldText(
                    listOf(
                        display,
                        reference,
                        canonical,
                        common,
                        identifiers,
                        constellation.orEmpty(),
                        constellationCode
                    ).joinToString(" ")
                )

            BaseTarget(
                id = "openngc:$canonical",
                name = display,
                reference = reference,
                objectType = typePair.first,
                typeLabel = typePair.second,
                constellation = constellation,
                raHours = ra,
                decDeg = dec,
                magnitude = magnitude,
                magnitudeBand = magnitudeBand,
                majorAxis = row["MajAx"]?.trim()?.toDoubleOrNull(),
                minorAxis = row["MinAx"]?.trim()?.toDoubleOrNull(),
                aliases = aliases.ifBlank { null },
                searchText = searchText,
                source = "OpenNGC v20260501"
            )
        }

    private fun loadIauInfo(): Map<String, IauInfo> {
        val proper =
            readCsv(
                "catalog/stars/iau_proper_stars.csv",
                ','
            ).associateBy {
                SkyMath.foldText(it["Proper Names"].orEmpty())
            }

        return readCsv(
            "catalog/stars/stars_with_data.csv",
            ','
        ).mapNotNull { row ->
            val name = row["Common Name"].orEmpty().trim()
            if (name.isBlank()) return@mapNotNull null

            val key = SkyMath.foldText(name)
            val aliases = mutableListOf<String>()
            row["Alternative Names"]
                ?.takeIf { it.isNotBlank() }
                ?.let { aliases += it }

            val p = proper[key]
            p?.get("Designation")
                ?.takeIf { it.isNotBlank() }
                ?.let { aliases += it }
            p?.get("Bayer ID")
                ?.takeIf { it.isNotBlank() }
                ?.let { aliases += it }
            p?.get("HIP")
                ?.takeIf { it.isNotBlank() }
                ?.let { aliases += "HIP $it" }

            val constellationCode =
                p?.get("Constellation").orEmpty().trim()
            val constellation =
                constellationFr[constellationCode]
                    ?: constellationCode.ifBlank { null }

            val reference =
                p?.get("Bayer ID")
                    ?.takeIf { it.isNotBlank() }
                    ?: p?.get("Designation")
                        ?.takeIf { it.isNotBlank() }

            key to IauInfo(
                aliases = aliases.distinct().joinToString(" | "),
                constellation = constellation,
                reference = reference
            )
        }.toMap()
    }

    private fun loadSkyMapStars(
        iau: Map<String, IauInfo>
    ): List<BaseTarget> {
        val seen = mutableSetOf<String>()
        val stars = mutableListOf<BaseTarget>()

        readCsv(
            "catalog/stars/skymap_stars.csv",
            ','
        ).forEach { row ->
            val magnitude = row["magnitude"]?.trim()?.toDoubleOrNull() ?: return@forEach
            if (magnitude > 6.0) return@forEach

            val raDeg = row["ra_deg"]?.trim()?.toDoubleOrNull() ?: return@forEach
            val dec = row["dec_deg"]?.trim()?.toDoubleOrNull() ?: return@forEach
            val names =
                row["names"].orEmpty()
                    .split('|')
                    .map { it.trim() }
                    .filter { it.isNotBlank() }

            val name =
                names.firstOrNull()
                    ?: row["id"].orEmpty().removePrefix("star/")
            if (name.isBlank()) return@forEach

            val key = SkyMath.foldText(name)
            seen += key
            val extra = iau[key]

            val aliases =
                (names.drop(1) +
                    listOfNotNull(extra?.aliases?.takeIf { it.isNotBlank() }))
                    .joinToString(" | ")

            val reference =
                extra?.reference ?: name

            val searchText =
                SkyMath.foldText(
                    "$name $reference $aliases ${extra?.constellation.orEmpty()}"
                )

            stars +=
                BaseTarget(
                    id = "skymap:${row["id"].orEmpty()}",
                    name = name,
                    reference = reference,
                    objectType = "star",
                    typeLabel = "Étoile",
                    constellation = extra?.constellation,
                    raHours = raDeg / 15.0,
                    decDeg = dec,
                    magnitude = magnitude,
                    magnitudeBand = "V",
                    majorAxis = null,
                    minorAxis = null,
                    aliases = aliases.ifBlank { null },
                    searchText = searchText,
                    source = "Sky Map / IAU"
                )
        }

        readCsv(
            "catalog/stars/stars_with_data.csv",
            ','
        ).forEach { row ->
            val name = row["Common Name"].orEmpty().trim()
            val key = SkyMath.foldText(name)
            if (name.isBlank() || key in seen) return@forEach

            val magnitude =
                row["Magnitude (V, Visual)"]?.trim()?.toDoubleOrNull()
                    ?: return@forEach
            if (magnitude > 6.0) return@forEach

            val ra = parseCompactRa(row["Right Ascension (HH.MM.SS)"])
                ?: return@forEach
            val dec = parseCompactDec(row["Declination (DD.SS)"])
                ?: return@forEach
            val extra = iau[key]
            val aliases =
                listOfNotNull(
                    row["Alternative Names"]?.takeIf { it.isNotBlank() },
                    extra?.aliases?.takeIf { it.isNotBlank() }
                ).joinToString(" | ")

            stars +=
                BaseTarget(
                    id = "iau:$key",
                    name = name,
                    reference = extra?.reference ?: name,
                    objectType = "star",
                    typeLabel = "Étoile",
                    constellation = extra?.constellation,
                    raHours = ra,
                    decDeg = dec,
                    magnitude = magnitude,
                    magnitudeBand = "V",
                    majorAxis = null,
                    minorAxis = null,
                    aliases = aliases.ifBlank { null },
                    searchText =
                        SkyMath.foldText(
                            "$name ${extra?.reference.orEmpty()} $aliases ${extra?.constellation.orEmpty()}"
                        ),
                    source = "IAU/WGSN"
                )
        }

        return stars
    }

    private fun readCsv(
        asset: String,
        delimiter: Char
    ): List<Map<String, String>> =
        context.assets.open(asset)
            .bufferedReader(Charsets.UTF_8)
            .use { reader ->
                val headerLine = reader.readLine() ?: return@use emptyList()
                val headers = parseCsvLine(headerLine.removePrefix("\uFEFF"), delimiter)
                reader.lineSequence()
                    .filter { it.isNotBlank() }
                    .map { parseCsvLine(it, delimiter) }
                    .map { values ->
                        headers.mapIndexed { index, header ->
                            header to values.getOrElse(index) { "" }
                        }.toMap()
                    }
                    .toList()
            }

    private fun parseCsvLine(
        line: String,
        delimiter: Char
    ): List<String> {
        val result = mutableListOf<String>()
        val value = StringBuilder()
        var quoted = false
        var index = 0

        while (index < line.length) {
            val c = line[index]
            when {
                c == '"' && quoted &&
                    index + 1 < line.length &&
                    line[index + 1] == '"' -> {
                    value.append('"')
                    index++
                }
                c == '"' -> quoted = !quoted
                c == delimiter && !quoted -> {
                    result += value.toString()
                    value.setLength(0)
                }
                else -> value.append(c)
            }
            index++
        }

        result += value.toString()
        return result
    }

    private fun normalizeCatalogName(value: String?): String? {
        val raw = value?.trim().orEmpty()
        if (raw.isBlank()) return null

        Regex("""^(NGC|IC)0*(\d+)(.*)$""", RegexOption.IGNORE_CASE)
            .matchEntire(raw)
            ?.let { match ->
                val catalog = match.groupValues[1].uppercase()
                val number = match.groupValues[2].toIntOrNull() ?: return@let
                val suffix = match.groupValues[3].trim()
                return if (suffix.isBlank()) "$catalog $number" else "$catalog $number$suffix"
            }

        Regex("""^M0*(\d+)$""", RegexOption.IGNORE_CASE)
            .matchEntire(raw)
            ?.let {
                return "M${it.groupValues[1].toInt()}"
            }

        return raw
    }

    private fun normalizeMessier(value: String?): String? {
        val raw = value?.trim().orEmpty()
        if (raw.isBlank()) return null
        val number = raw.removePrefix("M").removePrefix("m").toIntOrNull()
        return if (number != null) "M$number" else raw
    }

    private fun parseRa(value: String?): Double? {
        val parts = value?.trim()?.split(':') ?: return null
        if (parts.size != 3) return null
        val h = parts[0].toDoubleOrNull() ?: return null
        val m = parts[1].toDoubleOrNull() ?: return null
        val s = parts[2].toDoubleOrNull() ?: return null
        return h + m / 60.0 + s / 3600.0
    }

    private fun parseDec(value: String?): Double? {
        val raw = value?.trim().orEmpty()
        if (raw.isBlank()) return null
        val sign = if (raw.startsWith("-")) -1.0 else 1.0
        val parts = raw.removePrefix("+").removePrefix("-").split(':')
        if (parts.size != 3) return null
        val d = parts[0].toDoubleOrNull() ?: return null
        val m = parts[1].toDoubleOrNull() ?: return null
        val s = parts[2].toDoubleOrNull() ?: return null
        return sign * (d + m / 60.0 + s / 3600.0)
    }

    private fun parseCompactRa(value: String?): Double? {
        val parts = value?.trim()?.split('.', limit = 3) ?: return null
        if (parts.size != 3) return null
        val h = parts[0].toIntOrNull() ?: return null
        val m = parts[1].toIntOrNull() ?: return null
        val secondsText =
            if (parts[2].length > 2) {
                parts[2].take(2) + "." + parts[2].drop(2)
            } else {
                parts[2]
            }
        val s = secondsText.toDoubleOrNull() ?: return null
        return h + m / 60.0 + s / 3600.0
    }

    private fun parseCompactDec(value: String?): Double? {
        val raw = value?.trim().orEmpty()
        if (raw.isBlank()) return null
        val sign = if (raw.startsWith("-")) -1.0 else 1.0
        val clean = raw.removePrefix("+").removePrefix("-")
        val parts = clean.split('.', limit = 2)
        if (parts.size != 2 || parts[1].length < 2) return null
        val d = parts[0].toIntOrNull() ?: return null
        val m = parts[1].take(2).toIntOrNull() ?: return null
        val tail = parts[1].drop(2)
        val secondsText =
            when {
                tail.isBlank() -> "0"
                tail.length > 2 -> tail.take(2) + "." + tail.drop(2)
                else -> tail
            }
        val s = secondsText.toDoubleOrNull() ?: 0.0
        return sign * (d + m / 60.0 + s / 3600.0)
    }
}

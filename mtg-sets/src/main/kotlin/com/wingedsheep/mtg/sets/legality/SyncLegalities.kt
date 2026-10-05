package com.wingedsheep.mtg.sets.legality

import com.wingedsheep.mtg.sets.MtgSetCatalog
import com.wingedsheep.mtg.sets.basicLandsFallback
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Duration

/**
 * Live-API sibling of [SyncLegalitiesFromDump]. Walks every unique card name in [MtgSetCatalog]
 * (the live API has no enumeration endpoint, so we can only sync names we already know about),
 * queries Scryfall for each, and writes per-letter result files in `mtg-sets/src/main/resources/`.
 *
 * For full Scryfall coverage — including cards we haven't registered yet — use the dump path
 * (`./gradlew :mtg-sets:syncLegalityFromDump`) which can scan the entire bulk-data download in
 * one pass.
 *
 * Resume: re-running picks up where the previous run left off. Names already populated in the
 * per-letter files are not re-fetched; names with the [SCRYFALL_NOT_FOUND_MARKER] sentinel are
 * also skipped so we don't spam 404s. Names with an empty legality list are retried (they
 * usually mean "previous run hit a transient HTTP error").
 *
 * Why we keep *every* legal Scryfall format, not just our [com.wingedsheep.sdk.core.DeckFormat]
 * enum: deck-construction formats come and go. Persisting the full set means adding a new
 * `DeckFormat` later doesn't need a re-sync — the data is already there.
 *
 * Rate limit: Scryfall recommends 50–100 ms between requests; bursts trigger HTTP 429. We pace
 * at 200 ms baseline and back off exponentially on 429s.
 */
private const val BASELINE_DELAY_MS = 200L
private const val MAX_RETRIES_ON_429 = 5
private const val SCRYFALL_NOT_FOUND_MARKER = "__NOT_ON_SCRYFALL__"

private val parser = Json { ignoreUnknownKeys = true }
private val mapSerializer = MapSerializer(String.serializer(), ListSerializer(String.serializer()))
private val outputJson = Json { prettyPrint = true; prettyPrintIndent = "  " }

fun main() {
    val cardNames = MtgSetCatalog.all
        .flatMap { set -> set.cards + set.basicLands + (set.basicLandsFallback?.basicLands ?: emptyList()) }
        .map { it.name }
        .distinct()
        .sorted()

    val outDir = Paths.get("mtg-sets/src/main/resources")
    Files.createDirectories(outDir)

    val existing = loadAllBuckets(outDir)
    // Keep a working copy partitioned by bucket so we can flush a single file at a time.
    val bucketState: MutableMap<String, MutableMap<String, List<String>>> = existing
        .mapValuesTo(linkedMapOf()) { (_, v) -> v.toMutableMap() }

    val toFetch = cardNames.filter { name ->
        val prior = bucketState[LegalityData.bucketFor(name)]?.get(name)
        // Re-fetch if not present, OR present-but-empty (previous attempt failed and is worth
        // retrying). The sentinel keeps us from spamming 404s on resume.
        prior == null || prior.isEmpty()
    }
    val skipped = cardNames.size - toFetch.size
    println("Syncing legality: ${cardNames.size} unique cards ($skipped already populated, ${toFetch.size} to fetch).")
    if (toFetch.isEmpty()) {
        println("Nothing to do.")
        return
    }

    val client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build()
    val dirtyBuckets = mutableSetOf<String>()
    var failures = 0
    var i = 0
    for (name in toFetch) {
        i++
        val bucket = LegalityData.bucketFor(name)
        val outcome = fetchWithBackoff(client, name)
        val target = bucketState.getOrPut(bucket) { sortedMapOf() }
        when (outcome) {
            is FetchResult.Ok -> target[name] = outcome.formats
            FetchResult.NotFound -> {
                target[name] = listOf(SCRYFALL_NOT_FOUND_MARKER)
                println("[$i/${toFetch.size}] $name → not on Scryfall (marker stored)")
            }
            FetchResult.Failed -> {
                target[name] = emptyList()
                failures++
                println("[$i/${toFetch.size}] $name → giving up (will retry on next run)")
            }
        }
        dirtyBuckets += bucket
        if (i % 50 == 0) {
            println("[$i/${toFetch.size}] processed; flushing ${dirtyBuckets.size} bucket(s)")
            flushDirty(outDir, bucketState, dirtyBuckets)
        }
        Thread.sleep(BASELINE_DELAY_MS)
    }

    flushDirty(outDir, bucketState, dirtyBuckets)
    val totalEntries = bucketState.values.sumOf { it.size }
    println("Wrote $totalEntries entries across ${bucketState.size} buckets ($failures hard failures this run)")
}

private fun loadAllBuckets(outDir: Path): Map<String, Map<String, List<String>>> {
    val out = sortedMapOf<String, Map<String, List<String>>>()
    if (!Files.exists(outDir)) return out
    Files.list(outDir).use { stream ->
        stream
            .filter { it.fileName.toString().startsWith("legalities_") && it.fileName.toString().endsWith(".json") }
            .forEach { path ->
                val bucket = path.fileName.toString()
                    .removePrefix("legalities_")
                    .removeSuffix(".json")
                runCatching {
                    val text = Files.readString(path)
                    val parsed = parser.decodeFromString(mapSerializer, text)
                    out[bucket] = parsed
                }.onFailure {
                    println("Failed to parse $path (${it.message}); ignoring")
                }
            }
    }
    // Surface the legacy monolithic file (if anyone still has it) so a half-migrated workspace
    // doesn't re-fetch everything from scratch. Each entry gets routed to its proper bucket.
    val legacy = outDir.resolve("legalities.json")
    if (Files.exists(legacy)) {
        runCatching {
            val text = Files.readString(legacy)
            val parsed = parser.decodeFromString(mapSerializer, text)
            for ((name, formats) in parsed) {
                val bucket = LegalityData.bucketFor(name)
                val target = (out[bucket]?.toMutableMap() ?: sortedMapOf())
                target.putIfAbsent(name, formats)
                out[bucket] = target
            }
        }
    }
    return out
}

private fun flushDirty(
    outDir: Path,
    bucketState: Map<String, Map<String, List<String>>>,
    dirty: MutableSet<String>,
) {
    for (bucket in dirty) {
        val entries = bucketState[bucket] ?: continue
        val sorted = entries.toSortedMap()
        val outPath = outDir.resolve("legalities_$bucket.json")
        Files.writeString(outPath, outputJson.encodeToString(mapSerializer, sorted))
    }
    dirty.clear()
}

private sealed interface FetchResult {
    data class Ok(val formats: List<String>) : FetchResult
    data object NotFound : FetchResult
    data object Failed : FetchResult
}

private fun fetchWithBackoff(client: HttpClient, name: String): FetchResult {
    val encoded = URLEncoder.encode(name, StandardCharsets.UTF_8)
    val url = "https://api.scryfall.com/cards/named?exact=$encoded"
    val request = HttpRequest.newBuilder()
        .uri(URI.create(url))
        .header("User-Agent", "argentum-engine-legality-sync/1.0")
        .header("Accept", "application/json")
        .GET()
        .timeout(Duration.ofSeconds(15))
        .build()

    var attempt = 0
    var backoffMs = 2000L
    while (attempt <= MAX_RETRIES_ON_429) {
        try {
            val response = client.send(request, HttpResponse.BodyHandlers.ofString())
            when (response.statusCode()) {
                200 -> {
                    val card = parser.decodeFromString<ScryfallCard>(response.body())
                    // Persist every format Scryfall flagged "legal", uppercased to match our
                    // on-disk convention. We deliberately don't filter by DeckFormat.entries here:
                    // adding a format to the enum later should be a metadata change, not a sync.
                    val legal = card.legalities
                        .asSequence()
                        .filter { (_, status) -> status == "legal" }
                        .map { (key, _) -> key.uppercase() }
                        .sorted()
                        .toList()
                    return FetchResult.Ok(legal)
                }
                404 -> return FetchResult.NotFound
                429 -> {
                    println("  $name: 429 rate-limited, backing off ${backoffMs}ms (attempt ${attempt + 1}/${MAX_RETRIES_ON_429 + 1})")
                    Thread.sleep(backoffMs)
                    backoffMs = (backoffMs * 2).coerceAtMost(60_000)
                }
                else -> {
                    println("  $name: HTTP ${response.statusCode()}")
                    return FetchResult.Failed
                }
            }
        } catch (e: Exception) {
            println("  $name: ${e.message}")
            return FetchResult.Failed
        }
        attempt++
    }
    return FetchResult.Failed
}

@Serializable
private data class ScryfallCard(
    val legalities: Map<String, String> = emptyMap(),
)

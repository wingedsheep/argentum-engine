package com.wingedsheep.mtg.sets.tokens

import com.wingedsheep.mtg.sets.MtgSetCatalog
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.nio.file.Paths
import java.time.Duration

/**
 * One-shot Scryfall sync populating `mtg-sets/src/main/resources/tokens.json` — the bulk token
 * art behind [TokenArtData].
 *
 * For every registered set, fetches Scryfall's matching token set (`t<code>`) and writes one row
 * per token printing: name, `normal` card image, P/T, colors. Sets with no token set on Scryfall
 * (everything before roughly Odyssey never had token *cards*) are skipped and reported.
 *
 * Run with: `./gradlew :mtg-sets:syncTokenArt`
 *
 * The output is deliberately committed: the game must render tokens offline and without a
 * Scryfall round-trip per token creation. Hand edits belong in a set's `MtgSet.tokenArt` (which
 * wins over synced rows), never here — this file is machine-owned.
 *
 * Resumable: a set already recorded is not re-fetched, and progress is flushed after every set, so
 * a run cut short by rate limiting can simply be re-run until it reports no failures. A set that
 * genuinely has no token set is recorded as an empty list (checked, none); a set whose fetch
 * *failed* is left absent, so a dropped request can never harden into "prints no tokens".
 *
 * Image form is `normal`, preserving the printed token's rules and reminder text. The client still
 * supports legacy `art_crop` rows by rendering those inside its generated token frame.
 *
 * Rate limit: Scryfall asks for 50–100 ms between requests. Syncing ~150 sets back to back will
 * still trip the limiter, so we pace at 250 ms and back off exponentially on 429/503 — and abort
 * rather than write a file with sets silently missing.
 */
private const val BASELINE_DELAY_MS = 250L
private const val MAX_RETRIES = 8

private val parser = Json { ignoreUnknownKeys = true }
private val outputJson = Json { prettyPrint = true; prettyPrintIndent = "  " }

@Serializable
private data class Row(
    val name: String,
    val imageUri: String,
    val power: Int? = null,
    val toughness: Int? = null,
    val colors: List<String> = emptyList(),
    val scryfallId: String? = null,
)

private val serializer = MapSerializer(String.serializer(), ListSerializer(Row.serializer()))

private val TARGET = Paths.get("mtg-sets/src/main/resources/tokens.json")

fun main() {
    val setCodes = MtgSetCatalog.all.map { it.code }.distinct().sorted()
    val client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build()

    // Resume: a set already recorded — including one recorded as an empty list, meaning "checked,
    // Scryfall has no token set" — is not re-fetched. Syncing ~150 sets reliably trips Scryfall's
    // rate limiter, and restarting from zero each time never converges.
    val out = loadExisting().toMutableMap()
    val alreadyDone = setCodes.count { it in out }
    val toFetch = setCodes.filterNot { it in out }

    println("Syncing token art: ${setCodes.size} sets ($alreadyDone already recorded, ${toFetch.size} to fetch).")
    if (toFetch.isEmpty()) {
        report(out, failed = emptyList())
        return
    }

    val failed = mutableListOf<String>()
    for (code in toFetch) {
        when (val result = fetchTokenSet(client, "t${code.lowercase()}")) {
            is Fetch.Failed -> {
                // Left absent, never recorded as empty: a dropped request must not become the
                // permanent fact "this set prints no tokens". Re-running picks it up.
                failed += code
                println("  $code: FAILED (${result.reason})")
            }
            is Fetch.NoSuchSet -> {
                out[code] = emptyList()
                println("  $code: no token set on Scryfall")
            }
            is Fetch.Ok -> {
                // Several printings of the same token (different collector numbers, promos)
                // collapse to the first: art choice among identical tokens is arbitrary, and one
                // row per distinct identity is what the matcher wants.
                val deduped = result.rows
                    .distinctBy { listOf(it.name, it.power, it.toughness, it.colors) }
                out[code] = deduped
                println("  $code: ${deduped.size} tokens")
            }
        }
        // Flush after every set so a rate-limited run keeps everything it managed to fetch.
        write(out)
    }

    report(out, failed)
    if (failed.isNotEmpty()) kotlin.system.exitProcess(1)
}

private fun loadExisting(): Map<String, List<Row>> =
    if (Files.exists(TARGET)) {
        runCatching { parser.decodeFromString(serializer, Files.readString(TARGET)) }.getOrElse {
            System.err.println("Could not parse existing $TARGET (${it.message}); starting fresh.")
            emptyMap()
        }
    } else {
        emptyMap()
    }

private fun write(out: Map<String, List<Row>>) {
    Files.createDirectories(TARGET.parent)
    Files.writeString(TARGET, outputJson.encodeToString(serializer, out.toSortedMap()) + "\n")
}

private fun report(out: Map<String, List<Row>>, failed: List<String>) {
    val withArt = out.filterValues { it.isNotEmpty() }
    val withoutTokenSet = out.filterValues { it.isEmpty() }.keys

    println()
    println("${withArt.values.sumOf { it.size }} token printings across ${withArt.size} sets in $TARGET")
    println("${withoutTokenSet.size} sets have no Scryfall token set: ${withoutTokenSet.joinToString(" ")}")
    println("Those fall back to the generic TokenArt table unless the set declares its own tokenArt.")
    println("Run `just token-art-gaps` for the list of tokens still without their own art.")

    if (failed.isNotEmpty()) {
        System.err.println()
        System.err.println("${failed.size} set(s) failed and were NOT recorded: ${failed.joinToString(" ")}")
        System.err.println("Scryfall was rate-limiting. Re-run to resume — recorded sets are not re-fetched.")
    }
}

/**
 * Outcome of fetching one token set. [NoSuchSet] and [Failed] are deliberately distinct: the first
 * is a fact about the set (pre-2001 sets have no token cards), the second is a transport problem
 * that must never be recorded as "no tokens".
 */
private sealed interface Fetch {
    data class Ok(val rows: List<Row>) : Fetch
    data object NoSuchSet : Fetch
    data class Failed(val reason: String) : Fetch
}

private fun fetchTokenSet(client: HttpClient, scryfallSet: String): Fetch {
    var url: String? =
        "https://api.scryfall.com/cards/search?q=" +
            "set%3A$scryfallSet&unique=prints&order=set"
    val rows = mutableListOf<Row>()
    var firstPage = true

    while (url != null) {
        when (val response = get(client, url)) {
            // Scryfall answers an empty search with 404, which for `set:t<code>` means the set has
            // no token cards. Only meaningful on the first page.
            is Http.NotFound -> return if (firstPage) Fetch.NoSuchSet else Fetch.Ok(rows)
            is Http.Failed -> return Fetch.Failed(response.reason)
            is Http.Ok -> {
                firstPage = false
                val json = parser.parseToJsonElement(response.body).jsonObject
                (json["data"] as? JsonArray)?.forEach { card ->
                    toRow(card.jsonObject)?.let(rows::add)
                }
                url = if (json["has_more"]?.jsonPrimitive?.contentOrNull == "true") {
                    json["next_page"]?.jsonPrimitive?.contentOrNull
                } else {
                    null
                }
                if (url != null) Thread.sleep(BASELINE_DELAY_MS)
            }
        }
    }
    return Fetch.Ok(rows)
}

private fun toRow(card: JsonObject): Row? {
    val name = card["name"]?.jsonPrimitive?.contentOrNull ?: return null
    val image = (card["image_uris"] as? JsonObject)?.get("normal")?.jsonPrimitive?.contentOrNull
        ?: return null
    // Emblems and art-series cards live in token sets too but are never minted by CreateToken.
    val typeLine = card["type_line"]?.jsonPrimitive?.contentOrNull.orEmpty()
    if (typeLine.contains("Emblem") || typeLine.contains("Card")) return null
    return Row(
        name = name,
        imageUri = image,
        power = card["power"]?.jsonPrimitive?.intOrNull,
        toughness = card["toughness"]?.jsonPrimitive?.intOrNull,
        colors = (card["colors"] as? JsonArray).orEmpty()
            .mapNotNull { (it as? JsonPrimitive)?.contentOrNull }
            .mapNotNull { symbol -> com.wingedsheep.sdk.core.Color.fromSymbol(symbol.first())?.name },
        scryfallId = card["id"]?.jsonPrimitive?.contentOrNull,
    )
}

/** One HTTP outcome. [NotFound] is an answer; everything else that isn't 200 is a failure. */
private sealed interface Http {
    data class Ok(val body: String) : Http
    data object NotFound : Http
    data class Failed(val reason: String) : Http
}

/**
 * GET with exponential back-off on 429/503.
 *
 * Crucially this never collapses a transport failure into "no data" — the caller has to decide,
 * and [main] refuses to write a partial file. Rate limiting is the normal failure here: syncing
 * ~150 sets back-to-back will trip Scryfall's limiter unless we actually wait it out.
 */
private fun get(client: HttpClient, url: String): Http {
    var delay = BASELINE_DELAY_MS
    var lastReason = "no attempt made"
    repeat(MAX_RETRIES) {
        Thread.sleep(delay)
        val request = HttpRequest.newBuilder(URI.create(url))
            .header("User-Agent", "argentum-engine/1.0")
            .header("Accept", "application/json")
            .timeout(Duration.ofSeconds(20))
            .GET()
            .build()
        val response = runCatching {
            client.send(request, HttpResponse.BodyHandlers.ofString())
        }.getOrElse {
            lastReason = it.message ?: it::class.simpleName.orEmpty()
            delay *= 2
            return@repeat
        }
        when (val status = response.statusCode()) {
            200 -> return Http.Ok(response.body())
            404 -> return Http.NotFound
            // 429 rate limited, 503 overloaded — both clear if we wait.
            429, 503 -> {
                lastReason = "HTTP $status"
                delay *= 2
            }
            else -> return Http.Failed("HTTP $status")
        }
    }
    return Http.Failed("$lastReason (gave up after $MAX_RETRIES attempts)")
}

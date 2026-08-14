package com.wingedsheep.tooling.coverage

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.time.format.DateTimeParseException

/**
 * Self-contained Scryfall layer — set discovery from source, implementation scanning, and the
 * Scryfall fetch + `~/.cache/scryfall/<code>.json` schema-v7 cache. It reads and writes the *same*
 * cache files as the `scripts/card-status` tool, so the two share state and never duplicate fetches.
 */
object Scryfall {
    private const val SCRYFALL_BASE = "https://api.scryfall.com"
    private const val USER_AGENT = "argentum-engine-card-status/1.0"
    private const val REQUEST_DELAY_MS = 150L
    private const val CONNECT_TIMEOUT_MS = 10_000
    private const val READ_TIMEOUT_MS = 30_000
    private const val REFRESH_WINDOW_DAYS = 30L
    private const val CACHE_SCHEMA_VERSION = 8
    val STANDARD_SET_TYPES = setOf("core", "expansion", "draft_innovation")

    private val CACHE_ROOT = File(System.getProperty("user.home"), ".cache/scryfall")
    private val SET_CODE_RE = Regex("""override\s+val\s+code\s*=\s*"([^"]+)"""")
    private val DISPLAY_NAME_RE = Regex("""override\s+val\s+displayName\s*=\s*"([^"]+)"""")
    private val CARD_DSL_RE = Regex("""\b(?:card|basicLand)\(\s*"([^"]+)"""")
    private val PRINTING_NAME_RE = Regex("""\bname\s*=\s*"([^"]+)"""")
    private val SKIP_FOLDERS = setOf("custom")

    data class SetInfo(val code: String, val displayName: String, val cardsDir: File)

    /** Strip ` // back` suffix from DFC / adventure names. */
    fun frontFace(name: String): String = name.split(" // ", limit = 2)[0].trim()

    private var discoverSetsCache: List<SetInfo>? = null

    fun discoverSets(): List<SetInfo> {
        discoverSetsCache?.let { return it }
        val out = mutableListOf<SetInfo>()
        val dirs = DEFINITIONS_ROOT.listFiles()?.sortedBy { it.name } ?: emptyList()
        for (dir in dirs) {
            if (!dir.isDirectory || dir.name in SKIP_FOLDERS) continue
            val setKt = dir.listFiles { f -> f.isFile && f.name.endsWith("Set.kt") }?.firstOrNull() ?: continue
            val text = setKt.readText()
            val code = SET_CODE_RE.find(text)?.groupValues?.get(1)
            if (code == null) {
                System.err.println("warning: no `code` field in ${setKt.relativeTo(REPO_ROOT)}")
                continue
            }
            val name = DISPLAY_NAME_RE.find(text)?.groupValues?.get(1) ?: code
            val cardsDir = File(dir, "cards").let { if (it.isDirectory) it else dir }
            out.add(SetInfo(code, name, cardsDir))
        }
        return out.also { discoverSetsCache = it }
    }

    private val scanCache = HashMap<String, Set<String>>()

    fun scanImplementations(cardsDir: File): Set<String> = scanCache.getOrPut(cardsDir.path) {
        if (!cardsDir.isDirectory) return@getOrPut emptySet()
        val names = mutableSetOf<String>()
        cardsDir.listFiles { f -> f.name.endsWith(".kt") }?.forEach { kt ->
            val text = kt.readText()
            CARD_DSL_RE.findAll(text).forEach { names.add(it.groupValues[1]) }
            PRINTING_NAME_RE.findAll(text).forEach { names.add(it.groupValues[1]) }
        }
        names
    }

    private fun cachePath(code: String): File = File(CACHE_ROOT, "${windowsSafeFileName(code.lowercase())}.json")

    /** Uppercased set codes that already have a cached Scryfall canonical payload on disk. */
    fun cachedSetCodes(): Set<String> =
        CACHE_ROOT.listFiles { f -> f.isFile && f.extension == "json" && !f.name.startsWith("_") }
            ?.map { fromWindowsSafeFileName(it.nameWithoutExtension).uppercase() }?.toSet()
            ?: emptySet()

    /** A set's `released_at` (ISO date) read straight from the cache file — null, never a fetch, if absent. */
    fun releaseDate(code: String): String? {
        val path = cachePath(code)
        if (!path.isFile) return null
        return runCatching { (J.parseToJsonElement(path.readText()) as JsonObject)["released_at"].asStr() }.getOrNull()
    }

    private val SETNAMES_FILE: File get() = File(CACHE_ROOT, "_setnames.json")
    private var setNamesCache: Map<String, String>? = null

    /**
     * Uppercased set code → human display name for every Scryfall set. Served from a local cache file
     * (`~/.cache/scryfall/_setnames.json`); populated on first need from the Scryfall `/sets` endpoint
     * with a bounded timeout. Degrades to an empty map when offline so callers fall back to the code.
     */
    fun setDisplayNames(): Map<String, String> {
        setNamesCache?.let { return it }
        if (SETNAMES_FILE.isFile) {
            runCatching { (J.parseToJsonElement(SETNAMES_FILE.readText()) as JsonObject).mapValues { it.value.asStr() ?: "" } }
                .getOrNull()?.let { setNamesCache = it; return it }
        }
        val map = runCatching {
            val conn = URI("$SCRYFALL_BASE/sets").toURL().openConnection() as HttpURLConnection
            conn.setRequestProperty("User-Agent", USER_AGENT)
            conn.connectTimeout = 4000
            conn.readTimeout = 8000
            val text = conn.inputStream.readBytes().toString(StandardCharsets.UTF_8)
            conn.disconnect()
            ((J.parseToJsonElement(text) as JsonObject)["data"].asArr ?: JsonArray(emptyList()))
                .filterIsInstance<JsonObject>()
                .associate { ((it["code"].asStr() ?: "").uppercase()) to (it["name"].asStr() ?: "") }
                .filterKeys { it.isNotEmpty() }
        }.getOrDefault(emptyMap())
        if (map.isNotEmpty()) runCatching {
            CACHE_ROOT.mkdirs()
            SETNAMES_FILE.writeText(PRETTY.encodeToString(JsonElement.serializer(), JsonObject(map.mapValues { JsonPrimitive(it.value) })))
        }
        setNamesCache = map
        return map
    }

    private fun isCacheFresh(payload: JsonObject): Boolean {
        if (payload["_v"].asInt() != CACHE_SCHEMA_VERSION) return false
        val released = payload["released_at"].asStr() ?: return false
        val releasedDate = try {
            LocalDate.parse(released)
        } catch (_: DateTimeParseException) {
            return false
        }
        return !releasedDate.isAfter(LocalDate.now().minusDays(REFRESH_WINDOW_DAYS))
    }

    /**
     * GET a Scryfall URL with polite pacing and exponential backoff over the transient failures:
     * 429 (rate limit) and 5xx (outage). Any other 4xx is a real answer about the URL and throws on
     * the first try. Both timeouts are set explicitly — an unbounded read once hung the sibling
     * `scripts/card-status` for 1h44m on a half-open socket during a Scryfall outage.
     */
    fun scryfallGet(url: String, maxRetries: Int = 5): JsonObject {
        for (attempt in 0 until maxRetries) {
            Thread.sleep(REQUEST_DELAY_MS)
            val conn = URI(url).toURL().openConnection() as HttpURLConnection
            conn.setRequestProperty("User-Agent", USER_AGENT)
            conn.setRequestProperty("Accept", "application/json")
            conn.connectTimeout = CONNECT_TIMEOUT_MS
            conn.readTimeout = READ_TIMEOUT_MS
            val code = conn.responseCode
            if ((code == 429 || code >= 500) && attempt < maxRetries - 1) {
                val retryAfter = conn.getHeaderField("Retry-After")
                val wait = retryAfter?.toLongOrNull()?.times(1000) ?: (1000L shl attempt)
                conn.disconnect()
                Thread.sleep(wait)
                continue
            }
            if (code >= 400) {
                val body = conn.errorStream?.readBytes()?.toString(StandardCharsets.UTF_8) ?: ""
                conn.disconnect()
                throw ScryfallHttpError(code, "$url -> HTTP $code $body")
            }
            val text = conn.inputStream.readBytes().toString(StandardCharsets.UTF_8)
            conn.disconnect()
            return J.parseToJsonElement(text) as JsonObject
        }
        error("unreachable")
    }

    class ScryfallHttpError(val status: Int, message: String) : RuntimeException(message)

    private fun cardMetadata(card: JsonObject): JsonObject {
        // Image lives at the card level for single-faced cards, on the front face for DFCs.
        var imageUris = card["image_uris"].asObj
        if (imageUris == null) {
            val faces = card["card_faces"].asArr
            (faces?.firstOrNull() as? JsonObject)?.let { imageUris = it["image_uris"].asObj }
        }
        var imageUri = imageUris?.get("normal").asStr()
        if (imageUri != null) imageUri = imageUri.split("?", limit = 2)[0]  // drop ?<cache-buster>
        // oracle_text: card-level for single-faced; per-face joined `\n//\n` for multi-faced.
        var oracle = card["oracle_text"].asStr()
        if (oracle == null) {
            val faces = card["card_faces"].asArr
            val faceTexts = faces?.mapNotNull { (it as? JsonObject)?.get("oracle_text").asStr() }?.filter { it.isNotEmpty() }
            oracle = faceTexts?.takeIf { it.isNotEmpty() }?.joinToString("\n//\n")
        }
        return buildJsonObject {
            put("rarity", card["rarity"].asStr())
            put("collector_number", card["collector_number"].asStr())
            put("artist", card["artist"].asStr())
            put("image_uri", imageUri)
            put("flavor_text", card["flavor_text"].asStr())
            put("color_identity", card["color_identity"].asArr ?: JsonArray(emptyList()))
            put("oracle_text", oracle)
        }
    }

    private val LEADING_DIGITS = Regex("""^\d+""")

    /**
     * Orders a name's printings so the first is its representative: a booster printing before a
     * non-booster one, then the lowest collector number. That's the Play Booster art and number a
     * card generator wants — not whichever reprint Scryfall happens to order first.
     */
    private val PRINTING_ORDER: Comparator<JsonObject> = compareBy(
        { if (it["booster"] == JsonPrimitive(true)) 0 else 1 },
        { LEADING_DIGITS.find(it["collector_number"].asStr() ?: "")?.value?.toIntOrNull() ?: Int.MAX_VALUE },
        { it["collector_number"].asStr() ?: "" },
    )

    /**
     * Searches `unique=prints`, not `unique=cards`: one card can have several printings *inside* a
     * single set (a Play Booster printing plus a Beginner Box or set-extension reprint), and the
     * per-card row Scryfall serves is an arbitrary one of them. Reading `booster` off that arbitrary
     * row files a genuine booster card as an extra whenever the pick lands on the non-booster
     * printing — 14 Foundations cards did exactly that, shrinking FDN's booster denominator from 276
     * to 262. So the flag is OR-ed across a name's printings instead, and the printing whose metadata
     * we keep is chosen deliberately ([PRINTING_ORDER]) rather than inherited from result order.
     */
    private fun fetchFromScryfall(code: String): JsonObject {
        val setMeta = scryfallGet("$SCRYFALL_BASE/sets/${code.lowercase()}")
        val printings = linkedMapOf<String, MutableList<JsonObject>>()  // card name -> its printings in this set
        val q = URLEncoder.encode("set:${code.lowercase()} -is:rebalanced", StandardCharsets.UTF_8).replace("+", "%20")
        var url: String? = "$SCRYFALL_BASE/cards/search?q=$q&unique=prints&order=name"
        while (url != null) {
            val data = scryfallGet(url)
            for (cardEl in data["data"].asArr ?: JsonArray(emptyList())) {
                val card = cardEl as JsonObject
                val name = card["name"].asStr() ?: continue
                printings.getOrPut(name) { mutableListOf() }.add(card)
            }
            url = if (data["has_more"] == JsonPrimitive(true)) data["next_page"].asStr() else null
        }

        val draftNames = mutableListOf<String>()
        val extraNames = mutableListOf<String>()
        // Which product an extra came from (Scryfall `promo_types`), so the Set Completion view can
        // break the extras out into Scryfall-style groups (Starter Decks, Promos, …) instead of one lump.
        val extraProducts = linkedMapOf<String, List<String>>()
        var standardLegalCount = 0
        val cards = linkedMapOf<String, JsonObject>()
        for ((name, group) in printings) {
            if (group.any { it["booster"] == JsonPrimitive(true) }) {
                draftNames.add(name)
            } else {
                extraNames.add(name)
                val tags = group.flatMap { p -> (p["promo_types"].asArr ?: JsonArray(emptyList())).mapNotNull { it.asStr() } }
                extraProducts[name] = tags.distinct().sorted().ifEmpty { listOf("other") }
            }
            val representative = group.minWith(PRINTING_ORDER)
            // Format legality is a property of the card, not the printing — read it off the one.
            if (representative["legalities"].field("standard").asStr() == "legal") standardLegalCount++
            cards.putIfAbsent(frontFace(name), cardMetadata(representative))
        }
        return buildJsonObject {
            put("_v", CACHE_SCHEMA_VERSION)
            put("released_at", setMeta["released_at"].asStr())
            put("set_type", setMeta["set_type"].asStr())
            put("draft_names", buildJsonArray { draftNames.forEach { add(it) } })
            put("extra_names", buildJsonArray { extraNames.forEach { add(it) } })
            put("extra_products", buildJsonObject {
                extraProducts.forEach { (name, tags) -> put(name, buildJsonArray { tags.forEach { add(it) } }) }
            })
            put("standard_legal_count", standardLegalCount)
            put("cards", JsonObject(cards))
        }
    }

    /** Last resort after a failed refresh: the set's stale cache if we have one, else nothing. */
    private fun staleCacheOrNull(code: String, path: File, e: Exception): JsonObject? {
        if (path.isFile) {
            System.err.println("warning: refresh for $code failed ($e); using stale cache")
            return J.parseToJsonElement(path.readText()) as JsonObject
        }
        System.err.println("warning: failed to fetch $code: $e")
        return null
    }

    fun loadCanonical(code: String, forceRefresh: Boolean = false): JsonObject? {
        val path = cachePath(code)
        if (!forceRefresh && path.isFile) {
            val cached = runCatching { J.parseToJsonElement(path.readText()) as JsonObject }.getOrNull()
            if (cached != null && isCacheFresh(cached)) return cached
        }
        val payload = try {
            fetchFromScryfall(code)
        } catch (e: ScryfallHttpError) {
            return staleCacheOrNull(code, path, e)
        } catch (e: IOException) {
            // A timeout or connection blip that outlived the retries degrades to the stale cache for
            // this one set rather than aborting a whole multi-set sweep.
            return staleCacheOrNull(code, path, e)
        }
        CACHE_ROOT.mkdirs()
        path.writeText(PRETTY.encodeToString(JsonElement.serializer(), payload))
        return payload
    }

    /** Every booster-draftable set code Scryfall knows, newest-first (core/expansion/draft_innovation). */
    private var allSetCodesCache: List<String>? = null
    fun allSetCodes(): List<String> {
        allSetCodesCache?.let { return it }
        val payload = scryfallGet("$SCRYFALL_BASE/sets")
        val sets = (payload["data"].asArr ?: JsonArray(emptyList()))
            .filterIsInstance<JsonObject>()
            .filter { it["set_type"].asStr() in STANDARD_SET_TYPES && it["digital"] != JsonPrimitive(true) }
            .sortedByDescending { it["released_at"].asStr() ?: "" }
        return sets.map { (it["code"].asStr() ?: "").uppercase() }.also { allSetCodesCache = it }
    }

    private val PRETTY = kotlinx.serialization.json.Json { prettyPrint = true; prettyPrintIndent = "  " }
}

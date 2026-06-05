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
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.time.format.DateTimeParseException

/**
 * Self-contained Scryfall layer — the Kotlin port of the bits of `scripts/card-status` that
 * `probe.py` imported (set discovery from source, implementation scanning, and the Scryfall fetch +
 * `~/.cache/scryfall/<code>.json` schema-v6 cache). It reads and writes the *same* cache files as
 * the Python `card-status`, so the two tools share state and never duplicate fetches.
 */
object Scryfall {
    private const val SCRYFALL_BASE = "https://api.scryfall.com"
    private const val USER_AGENT = "argentum-engine-card-status/1.0"
    private const val REQUEST_DELAY_MS = 150L
    private const val REFRESH_WINDOW_DAYS = 30L
    private const val CACHE_SCHEMA_VERSION = 6
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

    fun discoverSets(): List<SetInfo> {
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
        return out
    }

    fun scanImplementations(cardsDir: File): Set<String> {
        if (!cardsDir.isDirectory) return emptySet()
        val names = mutableSetOf<String>()
        cardsDir.listFiles { f -> f.name.endsWith(".kt") }?.forEach { kt ->
            val text = kt.readText()
            CARD_DSL_RE.findAll(text).forEach { names.add(it.groupValues[1]) }
            PRINTING_NAME_RE.findAll(text).forEach { names.add(it.groupValues[1]) }
        }
        return names
    }

    private fun cachePath(code: String): File = File(CACHE_ROOT, "${code.lowercase()}.json")

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

    /** GET a Scryfall URL with polite pacing and 429-aware exponential backoff. */
    fun scryfallGet(url: String, maxRetries: Int = 5): JsonObject {
        for (attempt in 0 until maxRetries) {
            Thread.sleep(REQUEST_DELAY_MS)
            val conn = URI(url).toURL().openConnection() as HttpURLConnection
            conn.setRequestProperty("User-Agent", USER_AGENT)
            conn.setRequestProperty("Accept", "application/json")
            val code = conn.responseCode
            if (code == 429 && attempt < maxRetries - 1) {
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

    private fun fetchFromScryfall(code: String): JsonObject {
        val setMeta = scryfallGet("$SCRYFALL_BASE/sets/${code.lowercase()}")
        val draftNames = mutableListOf<String>()
        val extraNames = mutableListOf<String>()
        var standardLegalCount = 0
        val cards = linkedMapOf<String, JsonObject>()
        val q = URLEncoder.encode("set:${code.lowercase()} -is:rebalanced", StandardCharsets.UTF_8).replace("+", "%20")
        var url: String? = "$SCRYFALL_BASE/cards/search?q=$q&unique=cards&order=name"
        while (url != null) {
            val data = scryfallGet(url)
            for (cardEl in data["data"].asArr ?: JsonArray(emptyList())) {
                val card = cardEl as JsonObject
                val name = card["name"].asStr() ?: continue
                if (card["booster"] == JsonPrimitive(true)) draftNames.add(name) else extraNames.add(name)
                if (card["legalities"].field("standard").asStr() == "legal") standardLegalCount++
                cards.putIfAbsent(frontFace(name), cardMetadata(card))
            }
            url = if (data["has_more"] == JsonPrimitive(true)) data["next_page"].asStr() else null
        }
        return buildJsonObject {
            put("_v", CACHE_SCHEMA_VERSION)
            put("released_at", setMeta["released_at"].asStr())
            put("set_type", setMeta["set_type"].asStr())
            put("draft_names", buildJsonArray { draftNames.forEach { add(it) } })
            put("extra_names", buildJsonArray { extraNames.forEach { add(it) } })
            put("standard_legal_count", standardLegalCount)
            put("cards", JsonObject(cards))
        }
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
            if (path.isFile) {
                System.err.println("warning: refresh for $code failed ($e); using stale cache")
                return J.parseToJsonElement(path.readText()) as JsonObject
            }
            System.err.println("warning: failed to fetch $code: $e")
            return null
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

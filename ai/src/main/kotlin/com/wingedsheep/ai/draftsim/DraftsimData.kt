package com.wingedsheep.ai.draftsim

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import org.slf4j.LoggerFactory
import java.text.Normalizer
import java.util.concurrent.ConcurrentHashMap

private val logger = LoggerFactory.getLogger(DraftsimData::class.java)

/** One archetype tag on a card: `{archetype, role}` where role ∈ enabler|payoff|member. */
@Serializable
data class DraftsimArchetypeTag(val archetype: String, val role: String)

/** Per-card archetype record (`iA()` output): tags + fixing colors + splashable flag. */
@Serializable
data class DraftsimArchetypeRecord(
    val archetypes: List<DraftsimArchetypeTag> = emptyList(),
    val fixing: List<String> = emptyList(),
    val splashable: Boolean = false,
)

/**
 * The three lookup tables the scorer/builder consume for a pool, already joined and keyed.
 *
 *  - [ratings]: `nameKey → rating` (0–5). Empty for sets we have no file for — the scorer then
 *    falls back to the rarity ladder (`SPEC_scoring.md` §3 `ratingFallback`/`ratingOrDefault`).
 *  - [removal]: lowercased card names curated as removal (plus split front-face aliases). Membership
 *    is tested with `card.name.lowercase()` — a plain lowercase, **not** [nameKey] (bundle quirk).
 *  - [archetypes]: `nameKey → record`. Non-empty only for FDN/SOS/SOSSPG/TMT; drives the `jm` path.
 */
data class DraftsimSetTables(
    val ratings: Map<String, Double>,
    val removal: Set<String>,
    val archetypes: Map<String, DraftsimArchetypeRecord>,
)

/**
 * Loads and joins the vendored Draftsim tables (`ai/src/main/resources/draftai/`). Tables are
 * loaded once per set and cached; a multi-set pool (Chaos/Commander mixing several sets) gets the
 * union of all its sets' tables.
 *
 * Ported pieces: `gt` (nameKey), the removal resolver `j$`/`WU` (lowercase + split-card alias +
 * `_overrides.json` merge), and the ratings/archetype loaders (`oA`/`iA`).
 */
object DraftsimData {

    private val json = Json { ignoreUnknownKeys = true }

    private val perSet = ConcurrentHashMap<String, DraftsimSetTables>()
    private val mergedByKey = ConcurrentHashMap<String, DraftsimSetTables>()

    /**
     * `nameKey(name)` (`gt`): substring before `//`, NFD-normalize, strip combining diacritics
     * (U+0300–U+036F), `_`→space, trim, lowercase. Every ratings/archetype lookup uses this.
     */
    fun nameKey(name: String): String {
        val front = name.substringBefore("//")
        val nfd = Normalizer.normalize(front, Normalizer.Form.NFD)
        val noDiacritics = nfd.replace(DIACRITICS, "")
        return noDiacritics.replace('_', ' ').trim().lowercase()
    }

    /** Joined tables for a pool spanning [setCodes]. Order-independent; cached by the set of codes. */
    fun tablesFor(setCodes: List<String>): DraftsimSetTables {
        val codes = setCodes.map { it.uppercase() }.filter { it.isNotBlank() }.toSortedSet()
        if (codes.isEmpty()) return EMPTY
        val cacheKey = codes.joinToString(",")
        return mergedByKey.getOrPut(cacheKey) {
            if (codes.size == 1) loadSet(codes.first()) else mergeSets(codes.map { loadSet(it) })
        }
    }

    private fun mergeSets(tables: List<DraftsimSetTables>): DraftsimSetTables {
        // First-set-wins for ratings/archetypes (sorted order ⇒ deterministic); removal is a union.
        val ratings = LinkedHashMap<String, Double>()
        val archetypes = LinkedHashMap<String, DraftsimArchetypeRecord>()
        val removal = LinkedHashSet<String>()
        for (t in tables) {
            t.ratings.forEach { (k, v) -> ratings.putIfAbsent(k, v) }
            t.archetypes.forEach { (k, v) -> archetypes.putIfAbsent(k, v) }
            removal += t.removal
        }
        return DraftsimSetTables(ratings, removal, archetypes)
    }

    private fun loadSet(code: String): DraftsimSetTables = perSet.getOrPut(code) {
        val tables = DraftsimSetTables(
            ratings = loadRatings(code),
            removal = loadRemoval(code),
            archetypes = loadArchetypes(code),
        )
        // A set with no ratings file means we never shipped a table for this code (typically a
        // misspelled/unsupported set). The scorer still works via the rarity-ladder fallback, but
        // every card is then rated off rarity alone with no removal/archetype awareness — warn so a
        // typo'd set code isn't mistaken for "this set legitimately has no data".
        if (tables.ratings.isEmpty()) {
            logger.warn("Draftsim: no ratings table for set '{}' — scoring falls back to the rarity ladder", code)
        }
        tables
    }

    private fun loadRatings(code: String): Map<String, Double> {
        val text = readResource("/draftai/ratings/$code.json") ?: return emptyMap()
        // Keys are already in nameKey form; re-key defensively so any stray "//" name still resolves.
        return json.decodeFromString<Map<String, Double>>(text)
            .mapKeys { (k, _) -> nameKey(k) }
    }

    private fun loadArchetypes(code: String): Map<String, DraftsimArchetypeRecord> {
        val text = readResource("/draftai/archetypes/$code.json") ?: return emptyMap()
        return json.decodeFromString<Map<String, DraftsimArchetypeRecord>>(text)
            .mapKeys { (k, _) -> nameKey(k) }
    }

    private fun loadRemoval(code: String): Set<String> {
        val out = LinkedHashSet<String>()
        readResource("/draftai/removal/$code.json")?.let { text ->
            json.decodeFromString<Map<String, List<String>>>(text).keys.forEach { addRemovalName(out, it) }
        }
        overridesFor(code).forEach { addRemovalName(out, it) }
        return out
    }

    /** `WU`: lowercase the name; also alias the front face of a `"A // B"` split card. */
    private fun addRemovalName(out: MutableSet<String>, name: String) {
        out += name.lowercase()
        if ("//" in name) out += name.substringBefore("//").trim().lowercase()
    }

    /** Raw override card names for a set, from `removal/_overrides.json` (`P$`/`cB`). */
    private fun overridesFor(code: String): List<String> = overrides[code].orEmpty()

    private val overrides: Map<String, List<String>> by lazy {
        val text = readResource("/draftai/removal/_overrides.json") ?: return@lazy emptyMap()
        val root: JsonObject = json.parseToJsonElement(text).jsonObject
        root.entries
            .filter { it.key != "_comment" && it.value is JsonObject }
            .associate { (set, value) -> set.uppercase() to value.jsonObject.keys.toList() }
    }

    private fun readResource(path: String): String? =
        DraftsimData::class.java.getResourceAsStream(path)?.bufferedReader()?.use { it.readText() }

    private val DIACRITICS = Regex("[\\u0300-\\u036F]")
    private val EMPTY = DraftsimSetTables(emptyMap(), emptySet(), emptyMap())
}

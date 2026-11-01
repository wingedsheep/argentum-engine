package com.wingedsheep.mtg.sets.legality

import com.wingedsheep.sdk.core.DeckFormat
import com.wingedsheep.sdk.model.CardDefinition
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap

/**
 * Resolves [DeckFormat] legality for cards by name from bundled JSON resources.
 *
 * Storage layout: per-letter files in `src/main/resources/`, one per first-letter bucket of the
 * card name. `legalities_a.json` covers cards starting with A/a, `legalities_b.json` B/b, …,
 * `legalities_other.json` the long tail (digits, accented letters such as Æ, etc.). Each file
 * is a `name → [FORMAT, …]` map identical in shape to the old monolithic `legalities.json`.
 *
 * The split keeps individual JSON resources small even after the syncer captures every Scryfall
 * card. Buckets are loaded lazily on first access — the deckbuilder typically only needs a few
 * letters per session, and tests that touch a single card pay for one bucket. We also fall back
 * to the legacy monolithic `legalities.json` when a per-letter file is absent so older workspaces
 * keep working until they re-sync.
 *
 * Cards not present anywhere resolve to an empty set; callers should treat that as "format
 * restrictions cannot be enforced for this card" and let it through unless a format is set.
 */
object LegalityData {

    /**
     * Suffix the sync writes for a Scryfall `restricted` status (`VINTAGE_RESTRICTED`): legal as a
     * single copy. Not a [DeckFormat] of its own — it only feeds [derivedFormats].
     */
    const val RESTRICTED_SUFFIX = "_RESTRICTED"

    private const val OTHER_BUCKET = "other"
    private const val NOT_FOUND_MARKER = "__NOT_ON_SCRYFALL__"

    // Lazily-loaded per-bucket maps. `ConcurrentHashMap` so first-touch from multiple threads
    // (Spring beans wiring during startup) doesn't double-load.
    private val byBucket = ConcurrentHashMap<String, Map<String, Set<DeckFormat>>>()

    // Single-shot lazy load of the legacy monolithic file. Provides a fallback for buckets not
    // yet split into per-letter files. Empty map if the resource is missing.
    private val monolith: Map<String, Set<DeckFormat>> by lazy { loadMonolith() }

    private val parser = Json { ignoreUnknownKeys = true }
    private val mapSerializer = MapSerializer(String.serializer(), ListSerializer(String.serializer()))

    /** Returns the formats in which the named card is currently legal. */
    fun forCard(name: String): Set<DeckFormat> {
        if (name.isEmpty()) return emptySet()
        val bucket = bucketFor(name)
        val map = byBucket.computeIfAbsent(bucket, ::loadBucket)
        return map[name] ?: monolith[name] ?: emptySet()
    }

    /**
     * Returns a copy of [card] with [CardDefinition.legalFormats] populated from the bundled
     * data. Existing legalFormats on the card take precedence — this only fills the gap when
     * the field is empty (the default).
     */
    fun stamp(card: CardDefinition): CardDefinition =
        if (card.legalFormats.isNotEmpty()) card
        else card.copy(legalFormats = forCard(card.name))

    // -------------------------------------------------------------------------------------------
    // Internals
    // -------------------------------------------------------------------------------------------

    /**
     * The on-disk bucket key for [name]. Uses the lowercase ASCII first letter (`a`..`z`) when
     * possible; everything else (digits, Æ, ©, …) falls into the `other` bucket. Public so the
     * sync scripts can write into the same buckets without re-implementing the mapping.
     */
    fun bucketFor(name: String): String {
        if (name.isEmpty()) return OTHER_BUCKET
        val ch = name.first().lowercaseChar()
        return if (ch in 'a'..'z') ch.toString() else OTHER_BUCKET
    }

    private fun loadBucket(bucket: String): Map<String, Set<DeckFormat>> {
        val resource = "legalities_$bucket.json"
        val raw = readResource(resource) ?: return emptyMap()
        return parseRaw(raw)
    }

    private fun loadMonolith(): Map<String, Set<DeckFormat>> {
        val raw = readResource("legalities.json") ?: return emptyMap()
        return parseRaw(raw)
    }

    private fun readResource(name: String): String? {
        val stream = LegalityData::class.java.classLoader.getResourceAsStream(name) ?: return null
        return stream.use { it.readBytes().toString(Charsets.UTF_8) }
    }

    /**
     * The sync writes each Scryfall key upper-cased (`legalities.standardbrawl` → `STANDARDBRAWL`),
     * which is the enum name for every format except the ones whose [DeckFormat.scryfallKey] drops
     * the underscore. Keying by both spellings keeps those formats from silently reading as legal
     * nowhere — Standard Brawl did, so no card qualified and no deck could be built or validated.
     */
    private val formatsByName: Map<String, DeckFormat> = DeckFormat.entries.flatMap { format ->
        listOfNotNull(format.name, format.scryfallKey?.uppercase()).map { it to format }
    }.toMap()

    private fun parseRaw(text: String): Map<String, Set<DeckFormat>> {
        val raw: Map<String, List<String>> = parser.decodeFromString(mapSerializer, text)
        return raw.mapValues { (_, names) ->
            // The sync scripts write a sentinel for cards Scryfall returns 404 on (custom test
            // cards, tokens). It is not a real format — drop it. Scryfall formats not represented in
            // our DeckFormat enum (e.g. brawl, alchemy) miss the lookup and are dropped too — a map
            // lookup rather than valueOf(), whose exception per miss per card added up at startup.
            val formats = names.asSequence()
                .filter { it != NOT_FOUND_MARKER }
                .mapNotNullTo(mutableSetOf()) { formatsByName[it] }
            formats += derivedFormats(names)
            formats
        }
    }

    private const val VINTAGE_RESTRICTED = "VINTAGE$RESTRICTED_SUFFIX"

    /**
     * The formats Scryfall publishes no legality for, read off the keys it does publish. Both
     * Highlander formats play the Vintage card pool — every card Vintage allows, restricted ones
     * included, since a singleton deck can't break the restricted list's one copy. Canadian
     * Highlander's points are a deck-level rule, not a per-card one, so they don't enter here.
     */
    private fun derivedFormats(keys: List<String>): Set<DeckFormat> =
        if ("VINTAGE" in keys || VINTAGE_RESTRICTED in keys) {
            setOf(DeckFormat.HIGHLANDER, DeckFormat.CANADIAN_HIGHLANDER)
        } else {
            emptySet()
        }
}

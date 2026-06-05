package com.wingedsheep.tooling.coverage

import kotlinx.serialization.json.JsonObject

/**
 * Card-data wiring (port of probe.py's name/set plumbing): bridges Scryfall's canonical lists, the
 * repo's implemented names, and per-printing Scryfall metadata, all keyed by front-face name.
 */
object Cards {
    fun front(name: String): String = Scryfall.frontFace(name)

    /** Front-faced names implemented in one set's folder. */
    fun implementedNames(setCode: String): Set<String> {
        val info = Scryfall.discoverSets().firstOrNull { it.code == setCode.uppercase() } ?: return emptySet()
        return Scryfall.scanImplementations(info.cardsDir).map { front(it) }.toSet()
    }

    /** Every card front-face implemented anywhere in the repo (for net-new dedup across sets). */
    fun allImplementedNames(): Set<String> {
        val names = mutableSetOf<String>()
        for (s in Scryfall.discoverSets()) Scryfall.scanImplementations(s.cardsDir).forEach { names.add(front(it)) }
        return names
    }

    fun allSetCodes(): List<String> = Scryfall.allSetCodes()

    /** (draft, extra) front-faced canonical names from the Scryfall cache; (null, null) if no data. */
    fun canonicalNames(setCode: String, refresh: Boolean = false): Pair<Set<String>?, Set<String>?> {
        val payload = Scryfall.loadCanonical(setCode.uppercase(), forceRefresh = refresh) ?: return null to null
        val draft = (payload["draft_names"].asArr ?: return null to null).mapNotNull { it.asStr() }.map { front(it) }.toSet()
        val extra = (payload["extra_names"].asArr ?: return null to null).mapNotNull { it.asStr() }.map { front(it) }.toSet() - draft
        return draft to extra
    }

    private val scryfallMetaCache = HashMap<String, JsonObject>()

    /** Per-printing Scryfall metadata for one card (front-faced key), or null if absent from cache. */
    fun scryfallCard(setCode: String, name: String): JsonObject? {
        val code = setCode.uppercase()
        val cards = scryfallMetaCache.getOrPut(code) {
            (Scryfall.loadCanonical(code, forceRefresh = false)?.get("cards").asObj) ?: JsonObject(emptyMap())
        }
        return (cards[front(name)] ?: cards[name]).asObj
    }

    /** Which set codes already implement this card name. */
    fun implementedNamesForCard(name: String): Set<String> {
        val fn = front(name)
        val hits = mutableSetOf<String>()
        for (s in Scryfall.discoverSets()) {
            if (fn in Scryfall.scanImplementations(s.cardsDir).map { front(it) }.toSet()) hits.add(s.code)
        }
        return hits
    }
}

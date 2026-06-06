package com.wingedsheep.tooling.coverage

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/**
 * A small order-preserving multiset: insertion order is kept and [mostCommon] is stable on ties, so
 * leaderboards and reports are deterministic.
 */
class Counter<K> {
    private val map = LinkedHashMap<K, Int>()
    fun add(key: K, n: Int = 1) { map[key] = (map[key] ?: 0) + n }
    operator fun get(key: K): Int = map[key] ?: 0
    val keys: Set<K> get() = map.keys
    val isEmpty: Boolean get() = map.isEmpty()
    fun mostCommon(limit: Int? = null): List<Pair<K, Int>> {
        val sorted = map.entries.withIndex()
            .sortedWith(compareByDescending<IndexedValue<Map.Entry<K, Int>>> { it.value.value }.thenBy { it.index })
            .map { it.value.key to it.value.value }
        return if (limit != null) sorted.take(limit) else sorted
    }
}

/** mtgish IR access: download, the capability-bearing discriminators, the tag extractor, the index. */
object Mtgish {
    val CAPABILITY_DISCRIMINATORS = listOf(
        "_Rule", "_Action", "_Trigger", "_Cost", "_LayerEffect", "_StaticLayerEffect",
        "_ReplacementActionWouldEnter",
        // The replacement actions of a "next time you would draw …" effect (the Words cycle) reuse the
        // ordinary action vocabulary under this discriminator, so surface it to score their capability.
        "_ReplacementActionWouldDraw",
    )

    fun ensureData() {
        if (MTGISH_LINES.exists()) return
        MTGISH_LINES.parentFile.mkdirs()
        System.err.println("downloading mtgish IR (~29MB) -> $MTGISH_LINES ...")
        java.net.URI(MTGISH_URL).toURL().openStream().use { input ->
            MTGISH_LINES.outputStream().use { input.copyTo(it) }
        }
    }

    /** `extract_tags` — collect capability-bearing (discriminator, value) tags from a subtree. */
    fun extractTags(node: JsonElement?, out: Counter<Pair<String, String>>) {
        when (node) {
            is JsonObject -> {
                for (disc in CAPABILITY_DISCRIMINATORS) {
                    node[disc].asStr()?.let { out.add(disc to it) }
                }
                node.values.forEach { extractTags(it, out) }
            }
            is JsonArray -> node.forEach { extractTags(it, out) }
            else -> {}
        }
    }

    /** Map front-faced name -> mtgish card, for the requested names only (streams the 29MB IR). */
    fun loadMtgishIndex(names: Set<String>): Map<String, JsonObject> {
        ensureData()
        val want = names.map { it.lowercase() }.toSet()
        val found = LinkedHashMap<String, JsonObject>()
        MTGISH_LINES.bufferedReader().useLines { lines ->
            for (line in lines) {
                if (!line.contains("\"Name\":\"")) continue
                val card = runCatching { J.parseToJsonElement(line) as JsonObject }.getOrNull() ?: continue
                val fn = Scryfall.frontFace(card["Name"].asStr() ?: "")
                if (fn.lowercase() in want) found[fn] = card
            }
        }
        return found
    }
}

package com.wingedsheep.tooling.coverage

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * Typed, scope-correct queries over the dynamic mtgish IR tree.
 *
 * The IR is a tree of *discriminated nodes* — each a `JsonObject` shaped `{ "_Disc": "<tag>", "args":
 * <payload> }`, where the `_`-prefixed key names the node kind and its value is the *tag* (e.g.
 * `{"_Permanents":"IsCreatureType","args":"Goblin"}`). Handlers historically read these by serialising a
 * subtree with [compact] and running a regex/substring over the blob. That works but leaks scope: a blob
 * match (`"Flying" in blob`, `Regex(""""IsCreatureType",…"""")`) sees the *whole* flattened subtree, so a
 * marker meant for one clause can be picked up from an unrelated sibling or a nested action — the bug the
 * scattered "scope this to the trigger, not the whole rule" comments keep fighting by hand.
 *
 * These accessors walk the parsed tree instead, so a query is bounded by the node you ask it on. Each is
 * documented with the exact `compact()`+regex idiom it replaces, and every one preserves that idiom's
 * result on the IR corpus (proven byte-identical by the emitter-regression net) — the `(\w+)` captures
 * become a [WORD] filter, document (left-to-right blob) order is the pre-order traversal in [objects].
 *
 * Shared by the emitter handlers (filters / triggers / amounts) and available to the bridge.
 */

private const val ARGS = "args"
private val WORD = Regex("""\w+""")

/** Self + every descendant `JsonObject`, pre-order — i.e. the order a left-to-right scan of [compact] would
 *  visit them (parent before children, keys in insertion order). */
fun JsonElement?.objects(): Sequence<JsonObject> = sequence {
    when (val n = this@objects) {
        is JsonObject -> { yield(n); n.values.forEach { yieldAll(it.objects()) } }
        is JsonArray -> n.forEach { yieldAll(it.objects()) }
        else -> {}
    }
}

/** True iff this object is a node discriminated by [tag] — carries a `"_key": "tag"` entry (the `args`
 *  payload excluded). Key-agnostic, matching the regexes that keyed off the discriminator *value*. */
fun JsonObject.discriminatedBy(tag: String): Boolean =
    entries.any { (k, v) -> k != ARGS && v.asStr() == tag }

/** Every node in the subtree discriminated by [tag], in document order. */
fun JsonElement?.nodesTagged(tag: String): List<JsonObject> =
    objects().filter { it.discriminatedBy(tag) }.toList()

/** True iff any node in the subtree is discriminated by [tag] — the typed form of `"tag" in compact(node)`
 *  for a `tag` that is a discriminator value (e.g. `"IsTapped" in blob`). */
fun JsonElement?.hasTag(tag: String): Boolean = objects().any { it.discriminatedBy(tag) }

/** The single-word `args` string of every node discriminated by [tag], document order — the typed
 *  replacement for `Regex(""""tag","args":"(\w+)"""").findAll(compact(node))`. Non-word args (multi-token
 *  strings, arrays, objects) are skipped, mirroring the `(\w+)` capture's failure to match them. */
fun JsonElement?.argWordsTagged(tag: String): List<String> =
    nodesTagged(tag).mapNotNull { it[ARGS].asStr()?.takeIf(WORD::matches) }

/** The first single-word `args` of a node discriminated by [tag], or null. The `.find()`-then-group-1
 *  form of [argWordsTagged]. */
fun JsonElement?.firstArgWordTagged(tag: String): String? = argWordsTagged(tag).firstOrNull()

/** The full `args` string of the first node discriminated by [tag] (any string, mirroring a `[^"]+`
 *  capture rather than `(\w+)`), or null. */
fun JsonElement?.firstArgStringTagged(tag: String): String? =
    nodesTagged(tag).firstOrNull()?.get(ARGS).asStr()

/** Every single-word string value at key [key] in the subtree, document order — the typed form of
 *  `Regex(""""key":"(\w+)"""").findAll(compact(node))` (e.g. all `_Color` values). */
fun JsonElement?.wordsAtKey(key: String): List<String> =
    objects().mapNotNull { it[key].asStr()?.takeIf(WORD::matches) }.toList()

/** The first single-word value at [key] anywhere in the subtree, or null. */
fun JsonElement?.firstWordAtKey(key: String): String? = wordsAtKey(key).firstOrNull()

/** The first single-word `_Color`-keyed value inside the first node discriminated by [tag] (e.g. the
 *  colour carried by an `IsColor` / `IsNonColor` clause), or null — the scoped, node-bounded form of
 *  `Regex(""""tag".*?"_Color":\s*"(\w+)"""")`. */
fun JsonElement?.firstColorOf(tag: String): String? =
    nodesTagged(tag).firstOrNull()?.firstWordAtKey("_Color")

/** True iff any string-primitive *value* in the subtree equals [value] — the typed form of
 *  `"\"value\"" in compact(node)` (keys are `_`-prefixed / `args`, so only values can match). */
fun JsonElement?.hasStringValue(value: String): Boolean = anyStringValue { it == value }

private fun JsonElement?.anyStringValue(pred: (String) -> Boolean): Boolean = when (this) {
    is JsonObject -> values.any { it.anyStringValue(pred) }
    is JsonArray -> any { it.anyStringValue(pred) }
    is JsonPrimitive -> isString && pred(content)
    else -> false
}

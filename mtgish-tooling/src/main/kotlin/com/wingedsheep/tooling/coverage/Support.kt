package com.wingedsheep.tooling.coverage

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.io.File

/**
 * Shared infrastructure for the mtgish coverage tooling (the Kotlin port of
 * `spike/mtgish-coverage/{probe,emitter,fidelity,autogen}.py`).
 *
 * The mtgish IR and the Scryfall payloads are deeply dynamic JSON (discriminator keys like
 * `_Action`/`_Rule` plus an `args` of any shape), so — exactly like the Python — we work on a
 * parsed [JsonElement] tree and a set of small "walk the tree" helpers that mirror Python's
 * `dict.get` / list iteration. Several Python helpers `json.dumps(node)` then regex the string;
 * [compact] reproduces that (kotlinx default encoding is compact and order-preserving, matching
 * Python's insertion-ordered `dict`).
 */

/** Compact, order-preserving JSON. Parsing keeps key order (LinkedHashMap), re-encoding preserves it. */
val J: Json = Json

/** Repo root, discovered by walking up from the working dir until `settings.gradle.kts` is found. */
val REPO_ROOT: File by lazy {
    var dir: File? = File(System.getProperty("user.dir")).absoluteFile
    while (dir != null) {
        if (File(dir, "settings.gradle.kts").isFile && File(dir, "mtg-sdk").isDirectory) return@lazy dir
        dir = dir.parentFile
    }
    error("could not locate repo root (settings.gradle.kts) from ${System.getProperty("user.dir")}")
}

// --- canonical paths, matching the Python spike's module constants -------------------------------
val SDK_EFFECTS: File get() = File(REPO_ROOT, "mtg-sdk/src/main/kotlin/com/wingedsheep/sdk/scripting/effects")
val SDK_ROOT: File get() = File(REPO_ROOT, "mtg-sdk/src/main/kotlin")
val KEYWORD_KT: File get() = File(REPO_ROOT, "mtg-sdk/src/main/kotlin/com/wingedsheep/sdk/core/Keyword.kt")
val DEFINITIONS_ROOT: File get() = File(REPO_ROOT, "mtg-sets/src/main/kotlin/com/wingedsheep/mtg/sets/definitions")
val SNAP_DIR: File get() = File(REPO_ROOT, "mtg-sets/src/test/resources/snapshots/cards")
val GEN_DIR: File get() = File(REPO_ROOT, "mtg-sets/build/generated-cards")

// The bridge + the 29MB mtgish IR still live in the spike dir during the keep-both parity phase, so
// the Kotlin tool reads the exact same inputs the Python does (identical input -> exact parity).
val SPIKE_DIR: File get() = File(REPO_ROOT, "spike/mtgish-coverage")
val MAPPING_JSON: File get() = File(SPIKE_DIR, "mapping.json")
val MTGISH_LINES: File get() = File(SPIKE_DIR, "data/mtgish.lines.json")
const val MTGISH_URL = "https://raw.githubusercontent.com/i5jb/mtgish/main/data/mtgish.lines.json"
val DEFAULT_GENERATED_ROOT: File get() = File(SPIKE_DIR, "generated")

// ---------------------------------------------------------------------------
// JsonElement accessors (the dict/list/scalar lens onto the dynamic IR).
// ---------------------------------------------------------------------------
val JsonElement?.asObj: JsonObject? get() = this as? JsonObject
val JsonElement?.asArr: JsonArray? get() = this as? JsonArray

/** String content iff this is a JSON string primitive (mirrors Python truthiness on `str`). */
fun JsonElement?.asStr(): String? = (this as? JsonPrimitive)?.takeIf { it.isString }?.content

/** Int iff this is a numeric primitive. */
fun JsonElement?.asInt(): Int? = (this as? JsonPrimitive)?.takeIf { !it.isString }?.content?.toIntOrNull()

/** `node.get(key)` for an object, else null. */
fun JsonElement?.field(key: String): JsonElement? = this.asObj?.get(key)

/** `node.get(key)` returning its string value, else null — the most common IR read. */
fun JsonElement?.strField(key: String): String? = field(key).asStr()

/** Compact `json.dumps(node)` equivalent for the regex-on-blob helpers. */
fun compact(node: JsonElement?): String = if (node == null) "null" else J.encodeToString(JsonElement.serializer(), node)

// ---------------------------------------------------------------------------
// Tree walkers — faithful ports of the recursive helpers in probe.py / emitter.py.
// ---------------------------------------------------------------------------

/** `_contains(node, key, val)` — does the subtree carry a `{key: val}` (string) pair anywhere? */
fun jsonContains(node: JsonElement?, key: String, value: String): Boolean = when (node) {
    is JsonObject -> node.strField(key) == value || node.values.any { jsonContains(it, key, value) }
    is JsonArray -> node.any { jsonContains(it, key, value) }
    else -> false
}

/** `_find_integer` — first Integer/XValue under the subtree. Returns Int, the string "X", or null. */
fun findInteger(node: JsonElement?): Any? {
    when (node) {
        is JsonObject -> {
            when (node.strField("_GameNumber")) {
                "Integer" -> return node["args"].asInt()
                "XValue", "X", "ValueX" -> return "X"
            }
            for (v in node.values) findInteger(v)?.let { return it }
        }
        is JsonArray -> for (v in node) findInteger(v)?.let { return it }
        else -> {}
    }
    return null
}

/** `_find_adjust_pt` — the args (a [p, t] array) of the first AdjustPT layer effect. */
fun findAdjustPt(node: JsonElement?): JsonElement? {
    when (node) {
        is JsonObject -> {
            if (node.strField("_LayerEffect") == "AdjustPT") node["args"]?.let { return it }
            for (v in node.values) findAdjustPt(v)?.let { return it }
        }
        is JsonArray -> for (v in node) findAdjustPt(v)?.let { return it }
        else -> {}
    }
    return null
}

/** `_subtypes` — IsLandType / IsCardSubtype / library IsLandType argument strings in a filter subtree. */
fun subtypes(node: JsonElement?): List<String> {
    val out = mutableListOf<String>()
    fun walk(n: JsonElement?) {
        when (n) {
            is JsonObject -> {
                if (n.strField("_Permanents") in setOf("IsLandType", "IsCardSubtype")) n["args"].asStr()?.let { out.add(it) }
                if (n.strField("_CardsInLibrary") == "IsLandType") n["args"].asStr()?.let { out.add(it) }
                n.values.forEach { walk(it) }
            }
            is JsonArray -> n.forEach { walk(it) }
            else -> {}
        }
    }
    walk(node)
    return out
}

/** `_find_ref` — first _Permanent / _Player / _GraveyardCard reference string in a subtree. */
fun findRef(node: JsonElement?): String? {
    var found: String? = null
    fun walk(n: JsonElement?) {
        if (found != null) return
        when (n) {
            is JsonObject -> {
                for (k in listOf("_Permanent", "_Player", "_GraveyardCard")) {
                    if (found == null) n.strField(k)?.let { found = it }
                }
                n.values.forEach { walk(it) }
            }
            is JsonArray -> n.forEach { walk(it) }
            else -> {}
        }
    }
    walk(node)
    return found
}

/** First reference under the object marked by [markerKey], e.g. a `_DamageRecipient` node. */
fun findRefIn(node: JsonElement?, markerKey: String): String? {
    var found: String? = null
    fun walk(n: JsonElement?) {
        if (found != null) return
        when (n) {
            is JsonObject -> {
                if (n.containsKey(markerKey)) {
                    found = findRef(n[markerKey]) ?: findRef(n["args"])
                    if (found != null) return
                }
                n.values.forEach { walk(it) }
            }
            is JsonArray -> n.forEach { walk(it) }
            else -> {}
        }
    }
    walk(node)
    return found
}

/** `_amount_node` — first subtree object carrying a `_GameNumber` (an amount expression). */
fun amountNode(args: JsonElement?): JsonElement? {
    var found: JsonElement? = null
    fun walk(n: JsonElement?) {
        if (found != null) return
        when (n) {
            is JsonObject -> {
                if (found == null && n.containsKey("_GameNumber")) found = n
                n.values.forEach { walk(it) }
            }
            is JsonArray -> n.forEach { walk(it) }
            else -> {}
        }
    }
    walk(args)
    return found
}

/** `pascal_to_upper_snake` — FirstStrike -> FIRST_STRIKE, Shadow -> SHADOW. */
private val SNAKE_BOUNDARY = Regex("(?<=[a-z0-9])(?=[A-Z])")
fun pascalToUpperSnake(s: String): String = SNAKE_BOUNDARY.replace(s, "_").uppercase()

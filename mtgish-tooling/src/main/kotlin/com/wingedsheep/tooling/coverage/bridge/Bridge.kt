package com.wingedsheep.tooling.coverage.bridge

/**
 * The hand-authored mtgish→Argentum bridge: a mtgish IR tag mapped to the Argentum capability that
 * realises it. THIS IS THE MAINTENANCE COST the spike measures — and now it's typed, modular Kotlin
 * instead of a 500-line JSON wall.
 *
 * The five kinds (see [MappingEntry]):
 *  - [keyword]   — maps to a `Keyword` enum member; the tag is validated against the scanned registry
 *                  (a wrong guess surfaces as a MISSING gap = the mechanism catching its own error).
 *  - [effect]    — maps to a single Effect `@SerialName`; likewise registry-validated.
 *  - [composed]  — Argentum builds it from existing primitives (no leaf SerialName, e.g. destroy =
 *                  `MoveToZone`→graveyard). `composes` names the concrete primitives, used by fidelity.
 *  - [envelope]  — a structural mtgish wrapper that carries no capability itself; the real capability
 *                  lives in its nested nodes ("ignore" in the old JSON).
 *  - [supported] — a trigger/cost accepted without registry validation (a `Triggers.*`/`Costs.*`
 *                  facade scan would harden this later).
 *
 * To EXTEND: add a line to the relevant module file under `bridge/`, or add a whole new module file
 * with a `BridgeBuilder.() -> Unit` function and register it in [entries] below. Each entry is one
 * readable line; one tag helps every set (the bridge is shared infrastructure).
 */
object Bridge {
    // Every bridge module, in load order (order is irrelevant to results — keys must be unique).
    private val entries: Map<String, MappingEntry> by lazy {
        BridgeBuilder().apply {
            keywords()
            structuralEnvelopes()
            damageLifeAndCards()
            zoneMovement()
            manaCountersAndState()
            triggersCostsAndContinuous()
        }.build()
    }

    /** `mapping.get("disc:value", mapping.get(value))` — the entry for a tag, or null if unmapped. */
    fun entry(disc: String, value: String): MappingEntry? = entries["$disc:$value"] ?: entries[value]

    /** Bare-key lookup (the emitter's rule-name keyword check). */
    operator fun get(value: String): MappingEntry? = entries[value]

    /** Exposed for the parity test that diffs this bridge against the legacy mapping.json. */
    val all: Map<String, MappingEntry> get() = entries
}

/** A bridge entry. `kind` is the verdict label the probe prints; `composes` are the primitives a
 *  composed/envelope entry lowers to (read by the fidelity scorer). */
sealed class MappingEntry(val kind: String, val note: String?, val composes: List<String>) {
    /** The single Effect/Keyword SerialName this validates against the registry, or null. */
    open val tag: String? get() = null

    class Keyword(override val tag: String, note: String? = null) : MappingEntry("keyword", note, emptyList())
    class Effect(override val tag: String, note: String? = null) : MappingEntry("effect", note, emptyList())
    class Composed(note: String?, composes: List<String> = emptyList()) : MappingEntry("composed", note, composes)
    class Envelope(note: String?, composes: List<String> = emptyList()) : MappingEntry("ignore", note, composes)
    class Supported(note: String? = null) : MappingEntry("supported", note, emptyList())
}

/** Fluent builder shared by every module file. Keys are the bare mtgish value (the probe also accepts
 *  a `disc:value` key). A duplicate key is a bug, so we reject it loudly. */
class BridgeBuilder {
    private val map = LinkedHashMap<String, MappingEntry>()

    private fun add(value: String, entry: MappingEntry) {
        require(value !in map) { "duplicate bridge entry for '$value'" }
        map[value] = entry
    }

    fun keyword(value: String, tag: String, note: String? = null) = add(value, MappingEntry.Keyword(tag, note))
    fun effect(value: String, tag: String, note: String? = null) = add(value, MappingEntry.Effect(tag, note))

    /** Several mtgish verbs that map to the SAME effect (e.g. spell/permanent deals damage). */
    fun effects(vararg values: String, tag: String, note: String? = null) =
        values.forEach { add(it, MappingEntry.Effect(tag, note)) }

    fun composed(value: String, note: String? = null, composes: List<String> = emptyList()) =
        add(value, MappingEntry.Composed(note, composes))

    fun envelope(value: String, note: String? = null, composes: List<String> = emptyList()) =
        add(value, MappingEntry.Envelope(note, composes))

    /** Several envelopes that share a note/composes (e.g. the `May` gate cluster). */
    fun envelopes(vararg values: String, note: String? = null, composes: List<String> = emptyList()) =
        values.forEach { add(it, MappingEntry.Envelope(note, composes)) }

    fun supported(value: String, note: String? = null) = add(value, MappingEntry.Supported(note))

    fun build(): Map<String, MappingEntry> = map
}

/** Notes that recur verbatim, kept as constants so the intent reads cleanly at each call site. */
internal const val UNIVERSAL = "universal (cross-set calibration)"

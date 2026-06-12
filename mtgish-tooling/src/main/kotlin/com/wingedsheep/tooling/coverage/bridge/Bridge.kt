package com.wingedsheep.tooling.coverage.bridge

import com.wingedsheep.tooling.coverage.pascalToUpperSnake

/**
 * The hand-authored mtgish→Argentum bridge: a mtgish IR tag mapped to the Argentum capability that
 * realises it. This is the CAPABILITY dictionary — "can Argentum express this tag?" Its sibling, the
 * RENDERING dictionary ("what Kotlin DSL does it emit?"), lives in `emitter/` (see `ACTION_HANDLERS`).
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
 *  - [unsupported] — a tag the SDK *appears* to express but the rules engine does not actually
 *                  execute (e.g. a `Keyword` enum member with no engine handling). Pinning it
 *                  blocks the card instead of letting the PascalCase→enum auto-resolve emit a
 *                  silent no-op; the note names the engine work that would unlock it.
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

    /**
     * Resolve one mtgish tag against the bridge map + the scanned SDK registries, in ONE place so the
     * coverage [com.wingedsheep.tooling.coverage.Probe] (which renders [Resolution.status]/[Resolution.detail])
     * and the [com.wingedsheep.tooling.coverage.Fidelity] scorer (which collects the contributed
     * [Resolution.effectTag]/[Resolution.composedEffects]/[Resolution.keyword]) can't drift. Mirrors the
     * per-call logic both used to inline:
     *  - an unmapped tag whose PascalCase IS a `Keyword` enum member → auto keyword (`ok`),
     *  - an Effect/Keyword entry → `ok` iff its SerialName is in the registry, else `MISSING`,
     *  - a composed/envelope/supported entry → its own [MappingEntry.kind] label (never blocking),
     *  - any other unmapped tag → `UNMAPPED` (blocking).
     */
    fun resolve(disc: String, value: String, effects: Set<String>, keywords: Set<String>): Resolution {
        val entry = entry(disc, value)
        if (entry == null) {
            val auto = pascalToUpperSnake(value)
            return if (auto in keywords) Resolution(disc, value, "ok", "$auto (keyword auto)", keyword = auto)
            else Resolution(disc, value, "UNMAPPED", "")
        }
        return when (entry) {
            is MappingEntry.Effect -> {
                val ok = entry.tag in effects
                Resolution(disc, value, if (ok) "ok" else "MISSING", entry.tag, effectTag = entry.tag.takeIf { ok })
            }
            is MappingEntry.Keyword -> {
                val ok = entry.tag in keywords
                Resolution(disc, value, if (ok) "ok" else "MISSING", entry.tag, keyword = entry.tag.takeIf { ok })
            }
            // Composed / Envelope / Supported: the verdict is the entry's own kind label and never blocks;
            // its capability (if any) is the registry-present subset of `composes`, read by fidelity.
            else -> Resolution(disc, value, entry.kind, entry.note ?: "",
                composedEffects = entry.composes.filter { it in effects }.toSet())
        }
    }

    /**
     * One resolved tag, shared by the coverage probe and the fidelity scorer. [status]/[detail] are what
     * the probe prints; [effectTag]/[composedEffects]/[keyword] are the capabilities fidelity collects.
     */
    data class Resolution(
        val disc: String,
        val value: String,
        /** Probe verdict label: `ok` | `MISSING` | `UNMAPPED` | `composed` | `supported` | `ignore`. */
        val status: String,
        /** The probe's `-> detail` text (the resolved tag, the auto-keyword note, or the entry note). */
        val detail: String,
        /** Leaf Effect SerialName this tag contributes, present in the registry — else null. */
        val effectTag: String? = null,
        /** Composed primitives this tag lowers to (registry-present subset of `composes`). */
        val composedEffects: Set<String> = emptySet(),
        /** Keyword (mapped or PascalCase-auto) this tag contributes, present in the registry — else null. */
        val keyword: String? = null,
    ) {
        /** A tag that gates coverage: an absent capability (`MISSING`) or an unrecognised tag (`UNMAPPED`). */
        val blocking: Boolean get() = status == "MISSING" || status == "UNMAPPED"
    }
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

    /** SDK-visible but engine-inert (kind = "MISSING" so [Bridge.Resolution.blocking] is true): pins a
     *  tag that would otherwise auto-resolve to a no-op, e.g. a `Keyword` enum member with no handling. */
    class Unsupported(note: String? = null) : MappingEntry("MISSING", note, emptyList())
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

    /** Pin a tag as engine-unsupported (blocking) — see [MappingEntry.Unsupported]. */
    fun unsupported(value: String, note: String? = null) = add(value, MappingEntry.Unsupported(note))

    fun build(): Map<String, MappingEntry> = map
}

/** Notes that recur verbatim, kept as constants so the intent reads cleanly at each call site. */
internal const val UNIVERSAL = "universal (cross-set calibration)"

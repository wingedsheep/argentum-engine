package com.wingedsheep.tooling.coverage.emitter

import com.wingedsheep.tooling.coverage.Assign
import com.wingedsheep.tooling.coverage.Block
import com.wingedsheep.tooling.coverage.Eval
import com.wingedsheep.tooling.coverage.Stmt
import com.wingedsheep.tooling.coverage.Sub
import com.wingedsheep.tooling.coverage.arg
import com.wingedsheep.tooling.coverage.asInt
import com.wingedsheep.tooling.coverage.call
import com.wingedsheep.tooling.coverage.holesIn
import com.wingedsheep.tooling.coverage.jsonContains
import com.wingedsheep.tooling.coverage.render
import com.wingedsheep.tooling.coverage.strField
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject

/**
 * Speed rendering (Aetherdrift, CR 702.178–702.179) — the `_Rule: MaxSpeed` envelope.
 *
 * `_Rule: StartYourEngines` renders as the bare `startYourEngines()` builder call straight from
 * [Emitter]'s rule loop (like `station()` / `increment()`): the CR 704.5z state-based action does the
 * work, so there is nothing else to emit.
 *
 * `MaxSpeed` is the interesting one. It wraps **one nested `_Rule`** whose payload is an ordinary
 * ability, and Argentum's `maxSpeed { }` block accepts exactly the same three builders as `cardDef`
 * itself (`staticAbility` / `activatedAbility` / `triggeredAbility`). So rendering is delegation: run
 * the nested rule through the emitter's normal per-rule builder and re-parent the resulting block(s)
 * under `maxSpeed { }`.
 */

/** The builders `MaxSpeedBuilder` exposes — the nested render must consist only of these. */
private val MAX_SPEED_BUILDERS = setOf("staticAbility", "activatedAbility", "triggeredAbility")

/**
 * Render `Max speed — [Ability]` as `maxSpeed { <nested ability> }`, or null to SCAFFOLD.
 *
 * The nested rule is dispatched to the same builder [Emitter] would use for it at card level, which is
 * what keeps max-speed abilities exactly as faithful as their ungated equivalents — a shape the
 * activated/trigger/static emitters render well renders well here too, and one they decline declines
 * here too, with no second rendering path to drift.
 *
 * Declines (→ SCAFFOLD) in three cases, all deliberate:
 *  - the nested render produced anything `maxSpeed { }` can't hold (a `replacementEffect` line, a
 *    card-level assignment). This is what catches `ReplaceWouldDraw` / `ReplaceWouldDealDamage`
 *    (Vnwxt, Verbose Host; Far Fortune, End Boss) — a max-speed-gated *replacement* effect isn't
 *    expressible at all yet, so emitting the ungated replacement would silently drop the gate and make
 *    the card wrong-on-by-default rather than merely unimplemented;
 *  - the nested render left a located hole — a partial ability behind a gate is worse than none;
 *  - the nested rule is a kind the emitter has no builder for.
 */
internal fun EmitCtx.maxSpeedBlock(rule: JsonObject): List<Stmt>? {
    val inner = rule["args"] as? JsonObject ?: return null
    val innerName = inner.strField("_Rule") ?: return null

    val nested: List<Stmt>? = when (innerName) {
        // "Max speed — This creature has double strike." / "… gets +1/+2." — a static on the source.
        // Rendered here rather than through `staticHostBlock`, which only speaks the *aura* shape
        // (subject `HostPermanent`); a self-subject buff is exactly what `maxSpeed { }` sugars.
        "PermanentLayerEffect" -> selfStaticBlock(inner) ?: staticHostBlock(inner)
        // "Max speed — Other creatures you control have first strike." (Tsagan, Raider Warlord)
        "EachPermanentLayerEffect" -> staticLordBlock(inner)
        // "Max speed — {T}: Add {R}{R}." / "… {1}{B}, Sacrifice this: Search …"
        "Activated", "ActivatedWithModifiers" -> activatedBlock(inner)
        // "Max speed — Whenever you draw a card, …" / "… At the beginning of your end step, …"
        "TriggerA" -> triggerBlock(inner)
        // "Max speed — Spells you cast cost {1} less to cast." (Racers' Scoreboard)
        "PlayerEffect" -> playerEffectBlock(inner)
        // ReplaceWouldDraw / ReplaceWouldDealDamage and anything new: no builder, so scaffold.
        else -> null
    }
    if (nested.isNullOrEmpty()) return null
    if (holesIn(nested).isNotEmpty()) return null
    // Only re-parent statements the `maxSpeed { }` scope actually accepts; a `replacementEffect(…)`
    // Eval or a bare card-level assignment would compile as an unresolved reference inside the block.
    if (!nested.all(::acceptedByMaxSpeedBlock)) return null

    return listOf(Sub(Block("maxSpeed", nested)))
}

/** Whether `maxSpeed { }` can hold this statement — one of its three ability builders, or `keywords(…)`. */
private fun acceptedByMaxSpeedBlock(stmt: Stmt): Boolean = when (stmt) {
    is Sub -> stmt.block.header.substringBefore(' ') in MAX_SPEED_BUILDERS
    is Eval -> render(stmt.value).startsWith("keywords(")
    else -> false
}

/**
 * `PermanentLayerEffect(ThisPermanent, [...])` — a continuous effect on the source itself, as
 * `staticAbility { ability = … }` rows scoped with `GroupFilter.source()`.
 *
 * Deliberately narrow: only the two layer effects the max-speed corpus actually prints on a source —
 * `AdjustPT` (Walking Sarcophagus "+1/+2", Gastal Raider "+1/+1 and has menace") and `AddAbility` with a
 * *bare* keyword (Burnout Bashtronaut's double strike, Streaking Oilgorger's lifelink). Anything richer
 * (protection scopes, ward costs, a granted activated ability, an identity rewrite) returns null so the
 * card scaffolds, exactly as it does at card level today. It adds no reason of its own — the caller's
 * fallback to [staticHostBlock] records the `PermanentLayerEffect` gap.
 *
 * Returns null (not a decline reason) unless the subject really is `ThisPermanent`, so a nested aura
 * shape still routes to [staticHostBlock].
 */
private fun EmitCtx.selfStaticBlock(rule: JsonObject): List<Stmt>? {
    val args = rule["args"] as? JsonArray ?: return null
    if (!jsonContains(args.getOrNull(0), "_Permanent", "ThisPermanent")) return null
    val layerEffects = (args.getOrNull(1) as? JsonArray)?.filterIsInstance<JsonObject>()
    if (layerEffects.isNullOrEmpty()) return null

    val stmts = mutableListOf<Stmt>()
    for (le in layerEffects) {
        when (le.strField("_StaticLayerEffect")) {
            "AdjustPT" -> {
                val pt = le["args"] as? JsonArray ?: return null
                if (pt.size != 2) return null
                val p = pt[0].asInt() ?: return null
                val t = pt[1].asInt() ?: return null
                stmts.add(
                    Sub(
                        Block(
                            "staticAbility",
                            listOf(
                                Assign(
                                    "ability",
                                    call("ModifyStats", arg("$p"), arg("$t"), arg("GroupFilter.source()"))
                                )
                            )
                        )
                    )
                )
            }
            "AddAbility" -> {
                val granted = (le["args"] as? JsonArray)?.getOrNull(0) as? JsonObject ?: return null
                // Only a bare keyword rule. A parameterized or composite grant (Ward {N}, protection
                // from X, a nested activated/triggered ability) carries payload `keywords(...)` can't
                // express, and `keywordOf` would flatten it into a wrong bare keyword.
                if (granted["args"] != null) return null
                val kw = keywordOf(le) ?: return null
                // Prowess needs its +1/+1 trigger, not just the tag (see staticHostBlock).
                if (kw == "PROWESS") return null
                stmts.add(Eval(call("keywords", arg("Keyword.$kw"))))
            }
            else -> return null
        }
    }
    return stmts
}

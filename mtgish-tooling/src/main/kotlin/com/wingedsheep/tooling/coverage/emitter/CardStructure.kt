package com.wingedsheep.tooling.coverage.emitter

import com.wingedsheep.tooling.coverage.Assign
import com.wingedsheep.tooling.coverage.Block
import com.wingedsheep.tooling.coverage.Arg
import com.wingedsheep.tooling.coverage.Call
import com.wingedsheep.tooling.coverage.Composite
import com.wingedsheep.tooling.coverage.Dsl
import com.wingedsheep.tooling.coverage.Eval
import com.wingedsheep.tooling.coverage.Lit
import com.wingedsheep.tooling.coverage.Local
import com.wingedsheep.tooling.coverage.RawLine
import com.wingedsheep.tooling.coverage.Stmt
import com.wingedsheep.tooling.coverage.Sub
import com.wingedsheep.tooling.coverage.arg
import com.wingedsheep.tooling.coverage.argWordsTagged
import com.wingedsheep.tooling.coverage.asArr
import com.wingedsheep.tooling.coverage.asInt
import com.wingedsheep.tooling.coverage.asStr
import com.wingedsheep.tooling.coverage.call
import com.wingedsheep.tooling.coverage.compact
import com.wingedsheep.tooling.coverage.field
import com.wingedsheep.tooling.coverage.findInteger
import com.wingedsheep.tooling.coverage.jsonContains
import com.wingedsheep.tooling.coverage.nodesTagged
import com.wingedsheep.tooling.coverage.render
import com.wingedsheep.tooling.coverage.renderBlock
import com.wingedsheep.tooling.coverage.strField
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** (targets|null, actions|null) from the first Targeted / ActionList envelope in a subtree.
 *  Shared by spells (SpellActions) and triggered abilities (TriggerA). */
internal fun extractEnvelope(node: JsonElement?): Pair<List<JsonObject>?, List<JsonObject>?> {
    var foundTargets: List<JsonObject>? = null
    var foundActions: List<JsonObject>? = null
    fun walk(n: JsonElement?) {
        when (n) {
            is JsonObject -> {
                val actionsKind = n.strField("_Actions")
                val args = n["args"].asArr
                if (actionsKind == "Targeted" && args != null && args.size >= 2) {
                    if (foundActions == null) {
                        foundTargets = (args[0].asArr)?.filterIsInstance<JsonObject>() ?: emptyList()
                        foundActions = (args[1] as? JsonObject)?.get("args").asArr?.filterIsInstance<JsonObject>() ?: emptyList()
                    }
                } else if (actionsKind == "ActionList" && args != null && foundActions == null) {
                    foundActions = args.filterIsInstance<JsonObject>()
                }
                n.values.forEach { walk(it) }
            }
            is JsonArray -> n.forEach { walk(it) }
            else -> {}
        }
    }
    walk(node)
    return foundTargets to foundActions
}

/** (targetNode, tvar) for an envelope's targets; null if unrenderable (bail); (null, null) if none. */
internal fun EmitCtx.spellTargetExpr(targets: List<JsonObject>?, actions: List<JsonObject>? = null): Pair<Dsl?, String?>? {
    if (targets.isNullOrEmpty()) return null to null
    if (targets.size > 1) { reasons.add("multi-target"); return null }
    val t = targetExpr(targets[0], actions) ?: run { reasons.add("target:${targets[0].strField("_Target")}"); return null }
    return t to "t"
}

/** `val t = target("target", <node>)` — the bound-target local statement. */
private fun targetLocal(node: Dsl): Stmt = Local("t", call("target", arg("\"target\""), arg(node)))

/** `val <varName> = target("<targetName>", <node>)` — a named bound-target local (multi-target spells). */
private fun targetLocalNamed(varName: String, targetName: String, node: Dsl): Stmt =
    Local(varName, call("target", arg("\"$targetName\""), arg(node)))

/**
 * The (target-local statements, per-target var list) for a MULTI-target spell envelope — one
 * `target("tN", …)` local per chosen target, with stable `t1`/`t2`/… var names that
 * [EmitCtx.refTargetFromRef] resolves the suffixed `Ref_TargetPermanentN` refs against. Null if any
 * target can't be rendered exactly (-> SCAFFOLD). Each target is rendered in isolation by [targetExpr],
 * exactly as the single-target path does (Skulduggery: "target creature you control … and target
 * creature an opponent controls …").
 */
private data class MultiTargetLocals(
    val statements: List<Stmt>,
    val vars: List<String>,
    val refVars: Map<String, String>,
    val refVarsByKind: Map<String, List<String>>,
)

private fun EmitCtx.multiTargetLocals(targets: List<JsonObject>, actions: List<JsonObject>?): MultiTargetLocals? {
    val stmts = mutableListOf<Stmt>()
    val vars = mutableListOf<String>()
    val refsByKind = mutableMapOf<String, MutableList<String>>()
    // The mtgish IR sometimes drops the "up to one/N target" optionality on a multi-target spell's later
    // target (Burrog Barrage's "up to one target creature an opponent controls" arrives as a plain
    // mandatory TargetPermanent). The oracle text is authoritative, so recover per-target optionality
    // from the ordered "target …" clauses there and inject `optional = true` when the IR omitted it.
    val optionalByOrdinal = oracleOptionalTargetOrdinals(targets.size)
    // The mtgish IR occasionally mistypes one slot of a multi-graveyard-target spell. Badlands Revival's
    // second slot is "target permanent card from your graveyard", but the IR encodes it as `IsCardtype
    // Creature` like the first slot. The oracle text is authoritative, so recover the intended
    // graveyard-card noun ("permanent"/"creature"/…) per ordinal and rebuild the target when the IR
    // disagrees — rendering correctly rather than emitting the IR's lossy (too-narrow) filter.
    val graveyardNounByOrdinal = oracleGraveyardTargetNouns(targets.size)
    targets.forEachIndexed { i, t ->
        var node = targetExpr(t, actions) ?: run { reasons.add("target:${t.strField("_Target")}"); return null }
        graveyardTargetCorrection(t, graveyardNounByOrdinal.getOrNull(i))?.let { node = it }
        if (optionalByOrdinal.getOrNull(i) == true) node = node.withOptionalArg()
        val varName = "t${i + 1}"
        stmts.add(targetLocalNamed(varName, varName, node))
        vars.add(varName)
        refKindForTarget(t)?.let { refsByKind.getOrPut(it) { mutableListOf() }.add(varName) }
    }
    val refVars = refsByKind.mapNotNull { (ref, refVars) ->
        if (refVars.size == 1) ref to refVars.single() else null
    }.toMap()
    return MultiTargetLocals(stmts, vars, refVars, refsByKind.mapValues { it.value.toList() })
}

/**
 * For a spell whose IR declares [targetCount] targets in oracle order, returns a per-ordinal flag of
 * which are "up to one/N target …" (optional). Reads the oracle text — the IR occasionally drops the
 * "up to" qualifier on a later target (Burrog Barrage). Each ordered "(up to one|up to N )?target …"
 * mention maps to one declared target in order; only the explicit "up to" ones are optional. If the
 * count of "target" mentions doesn't line up with [targetCount], returns all-false (no over-claiming
 * optionality — better a mandatory mismatch caught by the gate than a silently wrong optional flag).
 */
/** Add `optional = true` to a `Target…(…)` Call (idempotent). Non-Call nodes (or ones already carrying
 *  the flag) are returned unchanged — recovering optionality only ever ADDS the flag, never overrides. */
private fun Dsl.withOptionalArg(): Dsl = when {
    this is Call && this.args.none { it.name == "optional" } ->
        this.copy(args = this.args + Arg(Lit("true"), "optional"))
    else -> this
}

private fun EmitCtx.oracleOptionalTargetOrdinals(targetCount: Int): List<Boolean> {
    val text = oracleText?.lowercase() ?: return List(targetCount) { false }
    // Match each "target" mention, capturing an immediately-preceding "up to <word/number> " qualifier.
    val mentions = Regex("""(up to (?:one|two|three|four|\d+)\s+)?target\b""")
        .findAll(text).map { it.groupValues[1].isNotBlank() }.toList()
    if (mentions.size != targetCount) return List(targetCount) { false }
    return mentions
}

/**
 * For each declared target ordinal (oracle order), the noun in a "target <noun> card from … graveyard"
 * mention, or null if the ordinal's mention isn't a graveyard-card target. Used to repair an IR slot
 * the mtgish data mistyped (Badlands Revival's "permanent card" slot arriving as `IsCardtype Creature`).
 * If the count of graveyard-card mentions doesn't line up with [targetCount], returns all-null (no
 * over-claiming — better an IR-faithful mismatch caught by the gate than a wrongly-recovered noun).
 */
private fun EmitCtx.oracleGraveyardTargetNouns(targetCount: Int): List<String?> {
    val text = oracleText?.lowercase() ?: return List(targetCount) { null }
    // Each ordered "target <noun> card from [a|an|your|its owner's|…] graveyard" mention -> its noun.
    val nouns = Regex("""target (\w+) card from [^.]*?graveyard""")
        .findAll(text).map { it.groupValues[1] }.toList()
    if (nouns.size != targetCount) return List(targetCount) { null }
    return nouns
}

/**
 * If [target] is a graveyard-card target and the oracle's recovered [oracleNoun] for this slot names
 * a card type the IR-rendered filter would contradict, returns the oracle-correct target node;
 * otherwise null (keep the IR-derived node). Only the "permanent card from your graveyard" repair is
 * modelled (Badlands Revival) — it maps to the same `GameObjectFilter.Permanent.ownedByYou()` in the
 * GRAVEYARD zone that the single-target path emits for Shepherd of the Clouds.
 */
private fun EmitCtx.graveyardTargetCorrection(target: JsonObject, oracleNoun: String?): Dsl? {
    val ttype = target.strField("_Target")
    if (ttype != "TargetGraveyardCard" && ttype != "UptoOneTargetGraveyardCard") return null
    val blob = compact(target)
    return when (oracleNoun) {
        // IR says creature, oracle says permanent -> the broader permanent-card-in-graveyard target.
        "permanent" -> {
            if ("IsPermanent" in blob) return null // already correct in the IR
            val owner = when {
                "\"You\"" in blob -> ".ownedByYou()"
                "\"Opponent\"" in blob -> ".ownedByOpponent()"
                else -> ""
            }
            val filt = Call(
                "TargetFilter",
                listOf(arg(Lit("GameObjectFilter.Permanent$owner")), arg("zone", "Zone.GRAVEYARD")),
            )
            Call("TargetObject", listOf(arg("filter", filt)))
        }
        else -> null
    }
}

private fun refKindForTarget(target: JsonObject): String? = when (target.strField("_Target")) {
    "TargetPlayer" -> "Ref_TargetPlayer"
    "TargetPermanent", "NumberTargetPermanents", "UptoNumberTargetPermanents", "OneOrTwoTargetPermanents", "UptoOneTargetPermanent" -> "Ref_TargetPermanent"
    "TargetGraveyardCard", "UptoOneTargetGraveyardCard" -> "Ref_TargetGraveyardCard"
    else -> null
}

private fun EmitCtx.conditionDsl(ifNode: JsonElement?): String? {
    val blob = compact(ifNode)
    if ("ControlsMorePermanentThanPlayer" in blob && "\"Land\"" in blob) return "Conditions.OpponentControlsMoreLands"
    if ("ControlsMorePermanentThanPlayer" in blob && "\"Creature\"" in blob) return "Conditions.OpponentControlsMoreCreatures"
    return null
}

internal fun EmitCtx.castEffectHandled(rule: JsonObject): Boolean {
    val node = rule["args"] as? JsonObject ?: return false
    return when (node.strField("_CastEffect")) {
        "CantBeCastUnless" -> castRestrictionLines(listOf(rule)) != null
        "AdditionalCastingCost" -> additionalCostLine(rule) != null
        "ReduceCastingCostIf" -> costReductionStaticLines(rule) != null
        "ReduceCastingCostIfItTargetsASpell" -> targetSpellCostReductionLines(rule) != null
        "MayCastAsThoughItHadFlashForAdditionalCost" -> flashKickerLine(rule) != null
        else -> false
    }
}

/**
 * `CastEffect(MayCastAsThoughItHadFlashForAdditionalCost(PayMana([symbols])))` -> a
 * `keywordAbility(KeywordAbility.flashKicker("{cost}"))` line. "You may cast this spell as though it
 * had flash if you pay {N} more to cast it." (Mystical Tether) — the Rout / Ghitu Fire rider: paying
 * the extra mana unlocks instant-speed casting without otherwise changing the spell. Only a pure-mana
 * additional cost renders; any other cost shape declines (-> SCAFFOLD) rather than approximate it.
 */
private fun EmitCtx.flashKickerLine(rule: JsonObject): String? {
    val node = rule["args"] as? JsonObject ?: return null
    if (node.strField("_CastEffect") != "MayCastAsThoughItHadFlashForAdditionalCost") return null
    val cost = node["args"] as? JsonObject ?: return null
    if (cost.strField("_Cost") != "PayMana") return null
    val mana = renderMana(cost.field("args"))
    if (mana.isEmpty()) return null
    return "    keywordAbility(KeywordAbility.flashKicker(\"$mana\"))"
}

internal fun EmitCtx.cardLevelCastEffectLines(card: JsonObject): List<String>? {
    val lines = mutableListOf<String>()
    for (rule in (card["Rules"].asArr ?: JsonArray(emptyList())).filterIsInstance<JsonObject>()) {
        if (rule.strField("_Rule") != "CastEffect") continue
        val line = additionalCostLine(rule)
        if (line != null) { lines.add(line); continue }
        val reduction = costReductionStaticLines(rule)
        if (reduction != null) { lines.addAll(reduction); continue }
        val targetReduction = targetSpellCostReductionLines(rule)
        if (targetReduction != null) { lines.addAll(targetReduction); continue }
        val flash = flashKickerLine(rule)
        if (flash != null) { lines.add(flash); continue }
        if (!castEffectHandled(rule)) return null
    }
    return lines
}

/**
 * `CastEffect(ReduceCastingCostIfItTargetsASpell([<cost symbols>], <spell filter>))` -> one
 * `staticAbility { ability = ModifySpellCost(SelfCast, …) }` per cost symbol, each gated on the spell
 * targeting an object matching the filter. Brush Off ("This spell costs {1}{U} less to cast if it
 * targets an instant or sorcery spell"):
 *  - `{1}` -> `ReduceGenericBy(FixedIfAnyTargetMatches(1, <filter>))`.
 *  - `{U}` -> `ReduceColoredIfAnyTargetMatches("{U}", <filter>)`.
 *
 * Only the instant-or-sorcery spell filter renders today (the only target-gated colored reduction in
 * the corpus); any other spell filter, or a cost symbol we don't model (generic-per-unit, life, …),
 * declines -> SCAFFOLD rather than emit a wrong gate.
 */
private fun EmitCtx.targetSpellCostReductionLines(rule: JsonObject): List<String>? {
    val node = rule["args"] as? JsonObject ?: return null
    if (node.strField("_CastEffect") != "ReduceCastingCostIfItTargetsASpell") return null
    val args = node["args"].asArr ?: return null
    val symbols = (args.getOrNull(0) as? JsonArray)?.filterIsInstance<JsonObject>() ?: return null
    if (symbols.isEmpty()) return null
    val spellFilter = args.getOrNull(1) as? JsonObject ?: return null
    // Only the "instant or sorcery spell" filter (Or(IsCardtype Instant, IsCardtype Sorcery)) is modeled.
    val filterDsl = if (spellFilter.strField("_Spells") == "Or") {
        val blob = compact(spellFilter)
        if (blob.contains("\"Instant\"") && blob.contains("\"Sorcery\"") && "IsCreatureType" !in blob) {
            "GameObjectFilter.InstantOrSorcery"
        } else return null
    } else return null

    val abilities = symbols.map { sym ->
        when (sym.strField("_CostReductionSymbol")) {
            "CostReduceGeneric" -> {
                val amount = sym["args"].asInt() ?: return null
                call(
                    "ModifySpellCost",
                    arg("target", Lit("SpellCostTarget.SelfCast")),
                    arg("modification", call(
                        "CostModification.ReduceGenericBy",
                        arg(call(
                            "CostReductionSource.FixedIfAnyTargetMatches",
                            arg("amount", "$amount"), arg("filter", Lit(filterDsl)),
                        )),
                    )),
                )
            }
            // A single colored pip (CostReduceW/U/B/R/G) -> the colored target-gated reduction.
            "CostReduceW", "CostReduceU", "CostReduceB", "CostReduceR", "CostReduceG" -> {
                val pip = sym.strField("_CostReductionSymbol")!!.removePrefix("CostReduce")
                call(
                    "ModifySpellCost",
                    arg("target", Lit("SpellCostTarget.SelfCast")),
                    arg("modification", call(
                        "CostModification.ReduceColoredIfAnyTargetMatches",
                        arg("symbols", Lit("\"{$pip}\"")), arg("filter", Lit(filterDsl)),
                    )),
                )
            }
            else -> return null
        }
    }
    return abilities.flatMap { renderBlock(Block("staticAbility", listOf(Assign("ability", it))), "    ") }
}

/**
 * `CastEffect(ReduceCastingCostIf([CostReduceGeneric N], <condition>))` -> a
 * `staticAbility { ability = ModifySpellCost(SelfCast, …) }` block. Renders only a single bare generic
 * reduction, and only for the exact conditions we can express faithfully:
 *
 *  - `PlayerPassesFilter(You, ControlsA(filter))` ("if you control a [filter]") ->
 *    `ReduceGenericBy(FixedIfControlFilter(N, <filter>))` (Cactarantula: "costs {1} less if you control
 *    a Desert").
 *  - `PlayerPassesFilter(You, CommitedACrimeThisTurn)` ("if you've committed a crime this turn") ->
 *    `ReduceGeneric(N)` gated by `CostGating.OnlyIf(Conditions.YouCommittedCrimeThisTurn)` (Seize the
 *    Secrets).
 *
 * Any other condition, or a colored/dynamic reduction, declines -> SCAFFOLD rather than widening.
 */
private fun EmitCtx.costReductionStaticLines(rule: JsonObject): List<String>? {
    val node = rule["args"] as? JsonObject ?: return null
    if (node.strField("_CastEffect") != "ReduceCastingCostIf") return null
    val args = node["args"].asArr ?: return null
    val symbols = (args.getOrNull(0) as? JsonArray)?.filterIsInstance<JsonObject>() ?: return null
    // Only a single bare generic reduction renders.
    if (symbols.size != 1 || symbols[0].strField("_CostReductionSymbol") != "CostReduceGeneric") return null
    val amount = symbols[0]["args"].asInt() ?: return null
    val cond = args.getOrNull(1) as? JsonObject ?: return null
    if (cond.strField("_Condition") != "PlayerPassesFilter") return null
    val condArgs = cond["args"].asArr ?: return null
    if ((condArgs.getOrNull(0) as? JsonObject)?.strField("_Player") != "You") return null
    val predicate = condArgs.getOrNull(1) as? JsonObject ?: return null

    val ability = when (predicate.strField("_Players")) {
        "ControlsA" -> {
            val filter = gameObjectFilterDsl(predicate["args"]) ?: return null
            call(
                "ModifySpellCost",
                arg("target", Lit("SpellCostTarget.SelfCast")),
                arg("modification", call(
                    "CostModification.ReduceGenericBy",
                    arg(call("CostReductionSource.FixedIfControlFilter", arg("amount", "$amount"), arg("filter", Lit(filter)))),
                )),
            )
        }
        "CommitedACrimeThisTurn" -> call(
            "ModifySpellCost",
            arg("target", Lit("SpellCostTarget.SelfCast")),
            arg("modification", call("CostModification.ReduceGeneric", arg("$amount"))),
            arg("gating", call("CostGating.OnlyIf", arg("Conditions.YouCommittedCrimeThisTurn"))),
        )
        else -> return null
    }
    return renderBlock(Block("staticAbility", listOf(Assign("ability", ability))), "    ")
}

/**
 * An `Affinity(<group filter>)` rule -> a self-cast `ModifySpellCost` whose generic reduction counts
 * the permanents you control matching the group ("Affinity for outlaws", "Affinity for artifacts").
 * The reduction reuses [CostReductionSource.PermanentsYouControlMatching], the general per-permanent
 * primitive. Only the group filters [gameObjectFilterDsl] can render exactly produce a block; any
 * other affinity group declines -> SCAFFOLD rather than guess (Hellspur Brute: "Affinity for outlaws").
 */
internal fun EmitCtx.affinityBlock(rule: JsonObject): List<Stmt>? {
    val filter = gameObjectFilterDsl(rule["args"]) ?: run { reasons.add("Affinity"); return null }
    val ability = call(
        "ModifySpellCost",
        arg("target", Lit("SpellCostTarget.SelfCast")),
        arg("modification", call(
            "CostModification.ReduceGenericBy",
            arg(call("CostReductionSource.PermanentsYouControlMatching", arg(Lit(filter)))),
        )),
    )
    return listOf(staticAbilityStmt(ability))
}

private fun EmitCtx.additionalCostLine(rule: JsonObject): String? {
    val node = rule["args"] as? JsonObject ?: return null
    if (node.strField("_CastEffect") != "AdditionalCastingCost") return null
    val cost = node["args"] as? JsonObject ?: return null
    if (cost.strField("_Cost") != "SacrificeAPermanent") return null
    val filter = gameObjectFilterDsl(cost["args"]) ?: return null
    return "    additionalCost(Costs.additional.SacrificePermanent($filter))"
}

private fun EmitCtx.castRestrictionLines(rules: List<JsonObject>): List<String>? {
    val lines = mutableListOf<String>()
    for (rule in rules) {
        val node = rule["args"] as? JsonObject ?: return null
        if (node.strField("_CastEffect") != "CantBeCastUnless") continue
        val blob = compact(node)
        if ("IsDuringDeclareAttackersStep" in blob && "IsAttacked" in blob) {
            lines.add("        castOnlyDuring(Step.DECLARE_ATTACKERS)")
            lines.add("        castOnlyIf(YouWereAttackedThisStep)")
        } else {
            return null
        }
    }
    return lines
}

/** Top-level `If{cond}[effect]` -> spell `condition =` gate + the inner effect (Gift of Estates). */
private fun EmitCtx.conditionalSpell(card: JsonObject): List<Stmt>? {
    val (_, actions) = extractEnvelope(card["Rules"])
    if (actions == null || actions.size != 1 || actions[0].strField("_Action") != "If") return null
    val ifNode = actions[0]
    val cond = conditionDsl(ifNode) ?: return null
    val body = ifNode["args"].asArr
    val inner = if (body != null && body.size > 1 && body[1] is JsonArray) (body[1] as JsonArray).filterIsInstance<JsonObject>() else null
    if (inner == null) return null
    val edsl = renderEffectList(inner, null) ?: return null
    return listOf(Sub(Block("spell", listOf(Assign("condition", Lit(cond)), Assign("effect", edsl)))))
}

/**
 * "you control a [filter]" condition (`PlayerPassesFilter(You, ControlsA(filter))`) -> the
 * `Conditions.YouControl(<filter>)` DSL string, or null when the player isn't You / the filter isn't
 * a single `ControlsA` we can render. Used by the [ifRuleBlock] static gates (Bristlepack Sentry's
 * "as long as you control a creature with power 4 or greater").
 */
private fun EmitCtx.youControlConditionDsl(condNode: JsonElement?): String? {
    val cond = condNode as? JsonObject ?: return null
    if (cond.strField("_Condition") != "PlayerPassesFilter") return null
    val args = cond["args"].asArr ?: return null
    if ((args.getOrNull(0) as? JsonObject)?.strField("_Player") != "You") return null
    val controls = args.getOrNull(1) as? JsonObject ?: return null
    if (controls.strField("_Players") != "ControlsA") return null
    val filter = gameObjectFilterDsl(controls["args"]) ?: return null
    return render(call("Conditions.YouControl", arg(Lit(filter))))
}

/**
 * "you control N or more [filter]" condition (`PlayerPassesFilter(You, ControlsNum([Comparison
 * GreaterThanOrEqualTo Integer N], <filter>))`) -> `Conditions.YouControlAtLeast(N, <filter>)`, or null
 * when the player isn't You, the comparison isn't `>= N` (a fixed integer), or the filter doesn't render
 * exactly. Used by Dust Animus's "if you control five or more untapped lands" enters-with-counters gate.
 * Only the `GreaterThanOrEqualTo` comparison maps to the at-least facade; any other comparator declines
 * -> SCAFFOLD rather than miscount.
 */
private fun EmitCtx.youControlAtLeastConditionDsl(condNode: JsonElement?): String? {
    val cond = condNode as? JsonObject ?: return null
    if (cond.strField("_Condition") != "PlayerPassesFilter") return null
    val args = cond["args"].asArr ?: return null
    if ((args.getOrNull(0) as? JsonObject)?.strField("_Player") != "You") return null
    val controls = args.getOrNull(1) as? JsonObject ?: return null
    if (controls.strField("_Players") != "ControlsNum") return null
    val cArgs = controls["args"].asArr ?: return null
    val comparison = cArgs.getOrNull(0) as? JsonObject ?: return null
    if (comparison.strField("_Comparison") != "GreaterThanOrEqualTo") return null
    val n = (comparison["args"] as? JsonObject)?.takeIf { it.strField("_GameNumber") == "Integer" }
        ?.get("args").asInt() ?: return null
    val filter = gameObjectFilterDsl(cArgs.getOrNull(1)) ?: return null
    return render(call("Conditions.YouControlAtLeast", arg("$n"), arg(Lit(filter))))
}

/**
 * A `FlashForCasters { Condition }` rule -> the card-level `conditionalFlash = <condition>`
 * assignment ("<this> has flash as long as <condition>", CR 702.8 / Colossal Rattlewurm). Only the
 * "you control a [filter]" condition the shared [youControlConditionDsl] renders exactly produces a
 * line; any other flash condition declines (null -> SCAFFOLD) so the emitter never grants flash on a
 * condition it can't reproduce.
 */
internal fun EmitCtx.conditionalFlashLines(rule: JsonObject): List<Stmt>? {
    val cond = youControlConditionDsl(rule["args"]) ?: run { reasons.add("FlashForCasters"); return null }
    return listOf(Assign("conditionalFlash", Lit(cond)))
}

/**
 * "you've committed a crime this turn" (`PlayerPassesFilter(You, CommitedACrimeThisTurn)`, OTJ) ->
 * the `Conditions.YouCommittedCrimeThisTurn` DSL string, or null when the player isn't You / the
 * filter isn't the bare crime predicate. Used by the [ifRuleBlock] static gates (Nimble Brigand's
 * conditional unblockable, Omenport Vigilante's conditional double strike). The predicate reads the
 * per-turn crime tracker each time the projected state is computed, so the gated ability appears the
 * moment a crime is committed and reverts at cleanup.
 */
private fun EmitCtx.youCommittedCrimeConditionDsl(condNode: JsonElement?): String? {
    val cond = condNode as? JsonObject ?: return null
    if (cond.strField("_Condition") != "PlayerPassesFilter") return null
    val args = cond["args"].asArr ?: return null
    if ((args.getOrNull(0) as? JsonObject)?.strField("_Player") != "You") return null
    val crime = args.getOrNull(1) as? JsonObject ?: return null
    // The bare crime predicate carries no further args; a parameterized variant would change meaning.
    if (crime.strField("_Players") != "CommitedACrimeThisTurn") return null
    return "Conditions.YouCommittedCrimeThisTurn"
}

/**
 * `If{cond}[rules]` permanent rule -> one or more `staticAbility { ability = ... }` blocks gating a
 * continuous effect on a condition. Renders only the exact shapes we can express faithfully:
 *
 *  - `If(IsAPlayersTurn(Other You))[PlayerEffect(You, DecreaseSpellCost(AnySpell, CostReduceGeneric N))]`
 *    -> `ModifySpellCost(YouCast(Any), ReduceGeneric(N), gating = OnlyIf(IsNotYourTurn))` (Geyser Drake).
 *  - `If(PlayerPassesFilter(You, ControlsA(filter)))[PermanentRuleEffect(ThisPermanent,
 *    CanAttackAsThoughItDidntHaveDefender)]`
 *    -> `CanAttackDespiteDefender(condition = YouControl(filter))` (Bristlepack Sentry).
 *
 * Any other condition/inner-effect combination declines -> SCAFFOLD rather than widening.
 */
internal fun EmitCtx.ifRuleBlock(rule: JsonObject): List<Stmt>? {
    val args = rule["args"].asArr ?: run { reasons.add("If"); return null }
    val cond = args.getOrNull(0) as? JsonObject ?: run { reasons.add("If"); return null }
    val inner = (args.getOrNull(1) as? JsonArray)?.filterIsInstance<JsonObject>()
        ?: run { reasons.add("If"); return null }
    if (inner.size != 1) { reasons.add("If"); return null }
    val innerRule = inner[0]

    // Geyser Drake: "During turns other than yours, spells you cast cost {1} less to cast."
    if (innerRule.strField("_Rule") == "PlayerEffect" &&
        cond.strField("_Condition") == "IsAPlayersTurn" &&
        jsonContains(cond, "_Players", "Other") && jsonContains(cond, "_Player", "You")
    ) {
        val pe = (innerRule["args"].asArr?.getOrNull(1) as? JsonArray)?.filterIsInstance<JsonObject>()?.singleOrNull()
        if (pe != null && pe.strField("_PlayerEffect") == "DecreaseSpellCost" &&
            jsonContains(pe, "_Spells", "AnySpell")
        ) {
            val amount = (pe as JsonElement?).nodesTagged("CostReduceGeneric").firstOrNull()?.get("args").asInt()
            // Only a single bare generic reduction (no colored symbols) renders; otherwise decline.
            val blob = compact(pe)
            val onlyGeneric = (pe as JsonElement?).nodesTagged("CostReduceGeneric").size == 1 &&
                !Regex("CostReduce(?!Generic)").containsMatchIn(blob)
            if (amount != null && onlyGeneric) {
                return listOf(staticAbilityStmt(call(
                    "ModifySpellCost",
                    arg("target", call("SpellCostTarget.YouCast", arg("GameObjectFilter.Any"))),
                    arg("modification", call("CostModification.ReduceGeneric", arg("$amount"))),
                    arg("gating", call("CostGating.OnlyIf", arg("Conditions.IsNotYourTurn"))),
                )))
            }
        }
        reasons.add("If"); return null
    }

    // Bristlepack Sentry: "As long as you control a creature with power 4 or greater, this creature
    // can attack as though it didn't have defender."
    if (innerRule.strField("_Rule") == "PermanentRuleEffect" &&
        jsonContains(innerRule, "_PermanentRule", "CanAttackAsThoughItDidntHaveDefender") &&
        jsonContains(innerRule, "_Permanent", "ThisPermanent")
    ) {
        val condDsl = youControlConditionDsl(cond) ?: run { reasons.add("If"); return null }
        return listOf(staticAbilityStmt(call("CanAttackDespiteDefender", arg("condition", Lit(condDsl)))))
    }

    // Nimble Brigand: "This creature can't be blocked if you've committed a crime this turn."
    //   If(PlayerPassesFilter(You, CommitedACrimeThisTurn))
    //     [ PermanentRuleEffect(ThisPermanent, [ CantBeBlocked ]) ]
    // -> ConditionalStaticAbility(CantBeBlocked(), Conditions.YouCommittedCrimeThisTurn). Only the
    // SELF (ThisPermanent), bare CantBeBlocked inner rule gated on the crime predicate renders; any
    // other permanent rule or condition declines -> SCAFFOLD. The conditional evasion (CR 509.1b is
    // checked as blocks are declared) is left to the engine's projected-state read of the gate.
    if (innerRule.strField("_Rule") == "PermanentRuleEffect" &&
        jsonContains(innerRule, "_PermanentRule", "CantBeBlocked") &&
        jsonContains(innerRule, "_Permanent", "ThisPermanent")
    ) {
        // A CantBeBlocked carrying any further args ("can't be blocked except by …") isn't the bare
        // unblockable rule — decline rather than drop the exception.
        val pr = (innerRule["args"].asArr?.getOrNull(1) as? JsonArray)?.filterIsInstance<JsonObject>()?.singleOrNull()
        if (pr == null || pr.strField("_PermanentRule") != "CantBeBlocked" || pr.size > 1) { reasons.add("If"); return null }
        val condDsl = youCommittedCrimeConditionDsl(cond) ?: run { reasons.add("If"); return null }
        return listOf(staticAbilityStmt(call(
            "ConditionalStaticAbility",
            arg("ability", call("CantBeBlocked")),
            arg("condition", Lit(condDsl)),
        )))
    }

    // Stoic Sphinx: "This creature has hexproof as long as you haven't cast a spell this turn."
    //   If(PlayerPassesFilter(You, HasntCastASpellThisTurn(AnySpell)))
    //     [ PermanentLayerEffect(ThisPermanent, [ AddAbility(<keyword>) ]) ]
    // -> ConditionalStaticAbility(GrantKeyword(Keyword.X, Filters.Self), Not(YouCastSpellsThisTurn(1))).
    // Omenport Vigilante: "This creature has double strike as long as you've committed a crime this
    //   turn." — same shape with a `CommitedACrimeThisTurn` gate.
    // Only the SELF "grant a single keyword to this permanent" inner shape renders; any other layer
    // effect (stat change, multi-keyword, group filter) declines -> SCAFFOLD.
    if (innerRule.strField("_Rule") == "PermanentLayerEffect" &&
        jsonContains(innerRule, "_Permanent", "ThisPermanent")
    ) {
        val condDsl = youHaventCastASpellConditionDsl(cond)
            ?: youCommittedCrimeConditionDsl(cond)
            ?: run { reasons.add("If"); return null }
        val layerEffects = (innerRule["args"].asArr?.getOrNull(1) as? JsonArray)?.filterIsInstance<JsonObject>()
        val le = layerEffects?.singleOrNull()
        if (le == null || le.strField("_StaticLayerEffect") != "AddAbility") { reasons.add("If"); return null }
        // An AddAbility whose granted rule is anything richer than a plain keyword (an activated ability,
        // landwalk, protection-from-color) isn't a bare keyword grant — decline rather than mis-render.
        val granted = (le["args"] as? JsonArray)?.singleOrNull() as? JsonObject
        if (granted?.strField("_Rule") != null && granted.size > 1) { reasons.add("If"); return null }
        val kw = keywordOf(le) ?: run { reasons.add("If"); return null }
        return listOf(staticAbilityStmt(call(
            "ConditionalStaticAbility",
            arg("ability", call("GrantKeyword", arg("Keyword.$kw"), arg("Filters.Self"))),
            arg("condition", Lit(condDsl)),
        )))
    }

    // Dust Animus: "If you control five or more untapped lands, this creature enters with two +1/+1
    // counters and a lifelink counter on it."
    //   If(PlayerPassesFilter(You, ControlsNum(>= 5, And(IsUntapped, IsCardtype Land))))
    //     [ AsPermanentEnters(ThisPermanent, [ EntersWithNumberCounters, EntersWithACounter ]) ]
    // -> the EntersWithCounters replacement(s) each carry a `condition = Conditions.YouControlAtLeast(N,
    // <filter>)` evaluated as the permanent enters. Only the "you control N or more [filter]" gate the
    // shared youControlAtLeastConditionDsl renders exactly produces a block; the enters-with rendering
    // (asEntersBlock) still declines any replacement it can't reproduce, so the whole card scaffolds
    // rather than dropping the condition or a counter.
    if (innerRule.strField("_Rule") == "AsPermanentEnters") {
        val condDsl = youControlAtLeastConditionDsl(cond) ?: run { reasons.add("If"); return null }
        return asEntersBlock(innerRule, condition = condDsl)
    }

    reasons.add("If"); return null
}

/**
 * "you haven't cast a spell this turn" (`PlayerPassesFilter(You, HasntCastASpellThisTurn(AnySpell))`)
 * -> the `Conditions.Not(Conditions.YouCastSpellsThisTurn(1))` DSL string. Only the unfiltered
 * any-spell, You-scoped shape renders here (the from-hand variant is handled by [interveningIfDsl]);
 * anything else declines.
 */
private fun EmitCtx.youHaventCastASpellConditionDsl(cond: JsonObject?): String? {
    if (cond == null) return null
    if (cond.strField("_Condition") == "PlayerPassesFilter" &&
        jsonContains(cond, "_Player", "You") &&
        jsonContains(cond, "_Players", "HasntCastASpellThisTurn") &&
        jsonContains(cond, "_Spells", "AnySpell")
    ) {
        return "Conditions.Not(Conditions.YouCastSpellsThisTurn(1))"
    }
    return null
}

/**
 * The (target list, inner action list) of a mtgish `Targeted` action node, or null if it isn't a
 * Targeted envelope. Mirrors [extractEnvelope]'s Targeted unpacking for a single known node.
 */
private fun targetedArms(node: JsonObject): Pair<List<JsonObject>?, List<JsonObject>>? {
    if (node.strField("_Actions") != "Targeted") return null
    val args = node["args"].asArr ?: return null
    if (args.size < 2) return null
    val targets = (args[0].asArr)?.filterIsInstance<JsonObject>()
    val actions = (args[1] as? JsonObject)?.get("args").asArr?.filterIsInstance<JsonObject>() ?: return null
    return targets to actions
}

/** A stable capture-flag name derived from a `ControlsA(IsCreatureType X)` condition — "controlledMount"
 *  for Steer Clear — so the emitted `captureAtCast` / `CapturedAtCast` pair agrees on one name. */
private fun castCaptureFlagName(cond: JsonObject): String {
    val sub = Regex(""""IsCreatureType",\s*"args":\s*"(\w+)"""").find(compact(cond))?.groupValues?.get(1)
    return if (sub != null) "controlled$sub" else "controlledMatchingPermanent"
}

/**
 * "As you cast this spell" condition-capture spell (CR 601.2i): mtgish models it as a
 * `SpellActions { Modal_IfElse(PlayerPassesFilter(You, ControlsA(filter)), <then Targeted>, <else
 * Targeted>) }`. In this corpus that envelope is *always* an "as you cast this spell" capture (Steer
 * Clear / Faerie Fencing / Flame Discharge), so it renders to the engine's cast-time capture
 * (`captureAtCast` + `Conditions.CapturedAtCast`) — NOT a resolution-time `ConditionalEffect` over the
 * current board, which would resolve the Mount/Faerie/modified test at the wrong time.
 *
 * Renders only the shapes we can express exactly: a `You + ControlsA(filter)` condition, one shared
 * target across both arms, and both arms a renderable effect list. The {X}-valued arms (Faerie
 * Fencing's -X/-X, Flame Discharge's X damage) decline through the inner [renderEffectList] ->
 * SCAFFOLD, in step with the engine's still-sloppy cast-time-X handling. Steer Clear's fixed 2/4
 * damage renders whole.
 */
private fun EmitCtx.castTimeCaptureSpell(card: JsonObject): List<Stmt>? {
    val modal = (card["Rules"].asArr ?: return null).filterIsInstance<JsonObject>()
        .firstNotNullOfOrNull { rule ->
            (rule["args"] as? JsonObject)?.takeIf { it.strField("_Actions") == "Modal_IfElse" }
        } ?: return null
    val margs = modal["args"].asArr ?: return null
    if (margs.size < 3) return null
    val cond = margs[0] as? JsonObject ?: return null
    val thenArm = margs[1] as? JsonObject ?: return null
    val elseArm = margs[2] as? JsonObject ?: return null

    // Condition: "you control a [filter]" — declines (-> SCAFFOLD) for anything else.
    val condDsl = youControlConditionDsl(cond) ?: return null
    val flag = castCaptureFlagName(cond)

    val (thenTargets, thenActions) = targetedArms(thenArm) ?: return null
    val (_, elseActions) = targetedArms(elseArm) ?: return null
    // One shared target (both arms hit the same creature); render it once from the then-arm.
    val (tnode, tvar) = spellTargetExpr(thenTargets, thenActions) ?: return null
    if (tvar == null || tnode == null) return null
    val thenEffect = renderEffectList(thenActions, tvar) ?: return null
    val elseEffect = renderEffectList(elseActions, tvar) ?: return null

    val stmts = mutableListOf<Stmt>()
    stmts.add(RawLine("        captureAtCast(\"$flag\", $condDsl)"))
    stmts.add(targetLocal(tnode))
    stmts.add(Assign("effect", call(
        "ConditionalEffect",
        arg("condition", Lit("Conditions.CapturedAtCast(\"$flag\")")),
        arg("effect", thenEffect),
        arg("elseEffect", elseEffect),
    )))
    return listOf(Sub(Block("spell", stmts)))
}

/**
 * Split a modal spell's oracle text into its per-mode bullet strings. mtgish carries no per-mode
 * label, but the engine's `mode("…")` wants the printed bullet text, so derive it from Scryfall's
 * oracle text: drop the "Choose one —" (or "Choose up to N —") header line, then split on the `•`
 * bullet marker. Returns null if the oracle text is absent or has no bullets — the renderer then
 * declines (→ SCAFFOLD) rather than invent labels.
 */
private fun EmitCtx.modeBullets(): List<String>? {
    val oracle = oracleText ?: return null
    if ("•" !in oracle) return null
    return oracle.substringAfter("•").let { "•$it" }
        .split("•")
        .map { it.trim().removeSuffix(".").trim() }
        .filter { it.isNotEmpty() }
        .takeIf { it.isNotEmpty() }
}

/**
 * One arm of a `Modal_ChooseOne` spell → the body statements of a `mode("<bullet>") { … }` block.
 *
 * Handles the arm shapes seen in the charm corpus, reusing the same machinery the non-modal spell path
 * uses so a mode renders exactly as the equivalent stand-alone spell would:
 *  - `ActionList` (no target) → `effect = <renderEffectList>`.
 *  - `Targeted` with a single conventional target (`TargetPermanent`/`TargetGraveyardCard`/`TargetPlayer`/
 *    …) → `val t = target("target", <node>)` + `effect = <renderEffectList(…, "t")>`.
 *  - `Targeted` with `BetweenOneAndNumberAnyTargets` + a fixed `SpellDealsDamage` to `Ref_AnyTargets`
 *    ("deals N damage to each of one or two targets") → `target = AnyTarget(count, minCount = 1)` +
 *    `effect = ForEachTargetEffect(listOf(Effects.DealDamage(N, ContextTarget(0))))`.
 *
 * Returns null for any arm shape it can't render exactly, so the whole card declines to SCAFFOLD
 * rather than emit a partial modal.
 */
private fun EmitCtx.modalArmBody(arm: JsonObject): List<Stmt>? {
    val kind = arm.strField("_Actions")
    if (kind == "ActionList") {
        val actions = arm["args"].asArr?.filterIsInstance<JsonObject>() ?: return null
        val effect = renderEffectList(actions, null) ?: return null
        return listOf(Assign("effect", effect))
    }
    if (kind != "Targeted") return null
    val (targets, actions) = targetedArms(arm) ?: return null
    if (targets == null || actions == null) return null
    if (targets.size != 1) return null
    val target = targets[0]

    // "deals N damage to each of one or two targets": a `BetweenOneAndNumberAnyTargets` target whose only
    // action is a fixed `SpellDealsDamage` to `Ref_AnyTargets`. The engine models this as a fixed-amount
    // `ForEachTargetEffect`, NOT the bound-single-target `DealDamageEffect` the generic SpellDealsDamage
    // handler would emit — so render it here and decline anything else on this target shape.
    if (target.strField("_Target") == "BetweenOneAndNumberAnyTargets") {
        val max = findInteger(target) as? Int ?: return null
        val damage = actions.singleOrNull()?.takeIf { it.strField("_Action") == "SpellDealsDamage" } ?: return null
        val amt = (findInteger(damage["args"]) as? Int) ?: return null
        if (!jsonContains(damage, "_DamageRecipient", "Ref_AnyTargets")) return null
        val forEach = call(
            "ForEachTargetEffect",
            arg(call("listOf", arg(call("Effects.DealDamage", arg("$amt"), arg("EffectTarget.ContextTarget(0)"))))),
        )
        return listOf(
            Assign("target", call("AnyTarget", arg("count", "$max"), arg("minCount", "1"))),
            Assign("effect", forEach),
        )
    }

    val tnode = targetExpr(target, actions) ?: run { reasons.add("target:${target.strField("_Target")}"); return null }
    val effect = renderEffectList(actions, "t") ?: return null
    return listOf(
        Local("t", call("target", arg("\"target\""), arg(tnode))),
        Assign("effect", effect),
    )
}

/**
 * "Choose one —" modal spell (`SpellActions { Modal_ChooseOne([arm, arm, …]) }`) → a
 * `spell { modal(chooseCount = 1) { mode("…") { … } … } }` block. Each arm is rendered by
 * [modalArmBody]; the mode labels come from the oracle-text bullets ([modeBullets]). Declines (→
 * SCAFFOLD) unless every arm renders AND the bullet count matches the arm count, so a card never emits
 * with a missing or mislabeled mode.
 */
internal fun EmitCtx.modalChooseOneSpell(card: JsonObject): List<Stmt>? {
    val spellActions = (card["Rules"].asArr ?: return null).filterIsInstance<JsonObject>()
        .firstNotNullOfOrNull { rule ->
            (rule["args"] as? JsonObject)?.takeIf { it.strField("_Actions") == "Modal_ChooseOne" }
        } ?: return null
    val arms = spellActions["args"].asArr?.filterIsInstance<JsonObject>() ?: return null
    if (arms.isEmpty()) return null

    val bullets = modeBullets() ?: run { reasons.add("modal-spell"); return null }
    if (bullets.size != arms.size) { reasons.add("modal-spell"); return null }

    val modeBlocks = arms.mapIndexed { i, arm ->
        val body = modalArmBody(arm) ?: run { reasons.add("modal-spell"); return null }
        Sub(Block("mode(\"${ktStr(bullets[i])}\")", body))
    }
    return listOf(Sub(Block("spell", listOf(Sub(Block("modal(chooseCount = 1)", modeBlocks))))))
}

/**
 * Split a Spree spell's oracle text into its per-mode bullet strings. Spree's bullets are introduced
 * by a leading `+` on each option line (`+ {2} — Explosive Derailment deals 4 damage to target
 * creature.`), unlike "Choose one —" modal's `•` markers. Drop the "Spree (…)" reminder header, then
 * take every line whose first non-space character is `+`. Returns null if no bullet lines are found —
 * the renderer then declines (-> SCAFFOLD) rather than invent labels.
 */
private fun EmitCtx.spreeBullets(): List<String>? {
    val oracle = oracleText ?: return null
    return oracle.lines()
        .map { it.trim() }
        .filter { it.startsWith("+") }
        .takeIf { it.isNotEmpty() }
}

/**
 * `SpellActions_Spree` (CR 702.166, Outlaws of Thunder Junction) -> a `spell { effect = ModalEffect(
 * modes = listOf(Mode(…, additionalManaCost = "{N}"), …), chooseCount = modes.size, minChooseCount = 1) }`
 * block — the hand-authored Spree idiom (Jailbreak Scheme). Each `SpreeAction` carries a `_Cost: PayMana`
 * (-> the mode's `additionalManaCost`) and an `_Actions` arm (Targeted / ActionList). The arm is rendered
 * by the same machinery a stand-alone spell uses; a targeted arm binds `EffectTarget.ContextTarget(0)`
 * (the modal target slot) and carries its target requirement under `targetRequirements`.
 *
 * Declines (-> SCAFFOLD) unless every arm renders exactly, the per-mode bullet labels are recoverable
 * and match the arm count, no targeted arm has more than one target, and no arm's effect is a
 * multi-line `Composite` (which the single-line `Mode(...)` call can't host) — so a Spree card never
 * emits with a missing mode, a wrong label, or a dropped target.
 */
internal fun EmitCtx.spreeSpellBlock(rule: JsonObject): List<Stmt>? {
    val spreeActions = rule["args"].asArr?.filterIsInstance<JsonObject>()
        ?.filter { it.strField("_SpreeAction") == "SpreeAction" } ?: return null
    if (spreeActions.isEmpty()) { reasons.add("Spree"); return null }

    val bullets = spreeBullets() ?: run { reasons.add("Spree"); return null }
    if (bullets.size != spreeActions.size) { reasons.add("Spree"); return null }

    val modeCalls = spreeActions.mapIndexed { i, spree ->
        val args = spree["args"].asArr ?: run { reasons.add("Spree"); return null }
        val costNode = args.firstOrNull { (it as? JsonObject)?.containsKey("_Cost") == true } as? JsonObject
        if (costNode?.strField("_Cost") != "PayMana") { reasons.add("Spree"); return null }
        val cost = renderMana(costNode.field("args")).ifEmpty { null } ?: run { reasons.add("Spree"); return null }
        val actionsNode = args.firstOrNull { (it as? JsonObject)?.containsKey("_Actions") == true } as? JsonObject
            ?: run { reasons.add("Spree"); return null }

        val modeArgs = mutableListOf<Arg>()
        when (actionsNode.strField("_Actions")) {
            "ActionList" -> {
                val actions = actionsNode["args"].asArr?.filterIsInstance<JsonObject>() ?: run { reasons.add("Spree"); return null }
                val effect = renderEffectList(actions, null) ?: return null
                if (effect is Composite) { reasons.add("Spree"); return null }
                modeArgs.add(arg("effect", effect))
            }
            "Targeted" -> {
                val (targets, actions) = targetedArms(actionsNode) ?: run { reasons.add("Spree"); return null }
                if (targets == null || actions == null || targets.size != 1) { reasons.add("Spree"); return null }
                val tnode = targetExpr(targets[0], actions)
                    ?: run { reasons.add("target:${targets[0].strField("_Target")}"); return null }
                // The targeted arm spends the modal target slot — bind the effect's target ref to it.
                val effect = renderEffectList(actions, "EffectTarget.ContextTarget(0)") ?: return null
                if (effect is Composite) { reasons.add("Spree"); return null }
                modeArgs.add(arg("effect", effect))
                modeArgs.add(arg("targetRequirements", call("listOf", arg(tnode))))
            }
            else -> { reasons.add("Spree"); return null }
        }
        modeArgs.add(arg("description", Lit("\"${ktStr(bullets[i])}\"")))
        modeArgs.add(arg("additionalManaCost", Lit("\"$cost\"")))
        Call("Mode", modeArgs)
    }

    val modal = call(
        "ModalEffect",
        arg("modes", call("listOf", *modeCalls.map { arg(it) }.toTypedArray())),
        arg("chooseCount", "${modeCalls.size}"),
        arg("minChooseCount", "1"),
    )
    return listOf(Sub(Block("spell", listOf(Assign("effect", modal)))))
}

internal fun EmitCtx.spellBlock(card: JsonObject): List<Stmt>? {
    // "As you cast this spell" cast-time captures arrive as a `Modal_IfElse` envelope; render them
    // (the cast-time form) before the generic modal guard below scaffolds everything `Modal_*`.
    castTimeCaptureSpell(card)?.let { return it }
    // "Choose one —" modal spells (`Modal_ChooseOne`) render to the engine's modal DSL — one
    // `mode("<bullet>") { … }` per arm. Render them before the generic `Modal_*` scaffold guard.
    modalChooseOneSpell(card)?.let { return it }
    // Other modal spells ("Choose up to four", entwine, escalate, …) carry a `Modal_*` envelope whose
    // children are the individual modes. The generic envelope path below would grab only the FIRST mode
    // and silently drop the rest, so scaffold the whole card rather than emit one arm of a modal spell.
    if ("\"Modal_" in compact(card["Rules"])) { reasons.add("modal-spell"); return null }
    // One-line `effect =` shortcuts, then whole-block shortcuts, then the generic envelope path.
    eachplayerMaydraw(card)?.let { return spellOf(it) }
    fluxEffect(card)?.let { return spellOf(it) }
    windsEffect(card)?.let { return spellOf(it) }
    extraTurnEffect(card)?.let { return spellOf(it) }
    distributedSpell(card)?.let { return it }
    balanceEffect(card)?.let { return it }
    conditionalSpell(card)?.let { return it }

    val (targets, actions) = extractEnvelope(card["Rules"])
    if (actions == null) return null

    // MULTI-target spell (two or more chosen targets, e.g. Skulduggery's "target creature you control …
    // and target creature an opponent controls …"). Render one `target("tN", …)` local per chosen
    // target and thread the per-target var list so the effects' suffixed `Ref_TargetPermanentN` refs
    // resolve to the right local. Any target the renderer can't express declines -> SCAFFOLD.
    if (targets != null && targets.size > 1) {
        val multi = multiTargetLocals(targets, actions) ?: run { reasons.add("multi-target"); return null }
        val targetStmts = multi.statements
        val vars = multi.vars
        targetVars = vars
        targetRefVars = multi.refVars
        targetRefVarsByKind = multi.refVarsByKind
        try {
            val edsl = renderEffectList(actions, vars.firstOrNull()) ?: run { reasons.add("multi-target"); return null }
            val restrictions = castRestrictionLines((card["Rules"].asArr ?: JsonArray(emptyList())).filterIsInstance<JsonObject>()) ?: return null
            val stmts = mutableListOf<Stmt>()
            restrictions.forEach { stmts.add(RawLine(it)) }
            targetStmts.forEach { stmts.add(it) }
            stmts.add(Assign("effect", edsl))
            return listOf(Sub(Block("spell", stmts)))
        } finally {
            targetVars = emptyList()
            targetRefVars = emptyMap()
            targetRefVarsByKind = emptyMap()
        }
    }

    val (tnode, tvar) = spellTargetExpr(targets, actions) ?: return null
    val edsl = renderEffectList(actions, tvar) ?: return null
    val restrictions = castRestrictionLines((card["Rules"].asArr ?: JsonArray(emptyList())).filterIsInstance<JsonObject>()) ?: return null
    val stmts = mutableListOf<Stmt>()
    restrictions.forEach { stmts.add(RawLine(it)) }
    if (tvar != null) stmts.add(targetLocal(tnode!!))
    stmts.add(Assign("effect", edsl))
    return listOf(Sub(Block("spell", stmts)))
}

private fun spellOf(effect: Dsl): List<Stmt> = listOf(Sub(Block("spell", listOf(Assign("effect", effect)))))

/** mtgish actions whose Argentum rendering already embeds the "you may" choice (so a wrapping
 *  MayAction must NOT also set the ability's `optional = true`). */
private val SELF_OPTIONAL_ACTIONS = setOf("PutACardFromHandOnBattlefield")

/** The IR "<Keyword>Counter" kinds the emitter renders as a Named CounterTypeFilter, each mapped to its
 *  `Counters` string constant. Restricted to the keyword counters the engine grants as a keyword
 *  (mirrors StateProjector.KEYWORD_COUNTER_MAP), which all have a known constant. Any other counter kind
 *  declines -> SCAFFOLD rather than emit a non-compiling `Counters.X`. */
private val KEYWORD_COUNTER_CONSTANT = mapOf(
    "FlyingCounter" to "FLYING",
    "FirstStrikeCounter" to "FIRST_STRIKE",
    "LifelinkCounter" to "LIFELINK",
    "IndestructibleCounter" to "INDESTRUCTIBLE",
    "DeathtouchCounter" to "DEATHTOUCH",
    "TrampleCounter" to "TRAMPLE",
    "HexproofCounter" to "HEXPROOF",
    "ReachCounter" to "REACH",
)

/**
 * True when the card carries an `ExilePermanentUntil … UntilPermanentLeavesBattlefield(ThisPermanent)`
 * action — the Banishing Light / O-Ring shape (Mystical Tether, Lassoed by the Law). mtgish encodes
 * only the ETB exile half; the linked return is implicit in the expiration, so the emitter synthesizes
 * the matching leaves-battlefield trigger (see [linkedExileReturnTrigger]).
 */
internal fun hasLinkedExileUntilLeaves(card: JsonObject): Boolean {
    val nodes = (card as JsonElement?).nodesTagged("ExilePermanentUntil")
    return nodes.any { node ->
        val a = node["args"].asArr ?: return@any false
        val expiration = a.getOrNull(1) as? JsonObject ?: return@any false
        expiration.strField("_Expiration") == "UntilPermanentLeavesBattlefield" &&
            jsonContains(expiration, "_Permanent", "ThisPermanent")
    }
}

/**
 * The synthesized "when this leaves the battlefield, return the linked exiled card" trigger that pairs
 * with an `Effects.ExileUntilLeaves` exile (Banishing Light shape). mtgish carries no explicit rule for
 * the return — it's implied by the `UntilPermanentLeavesBattlefield` expiration — so the emitter appends
 * this fixed block once per card (guarded by [hasLinkedExileUntilLeaves]).
 */
internal fun linkedExileReturnTrigger(): List<Stmt> = listOf(
    Sub(Block("triggeredAbility", listOf(
        Assign("trigger", Lit("Triggers.LeavesBattlefield")),
        Assign("effect", call("Effects.ReturnLinkedExileUnderOwnersControl")),
    ))),
)

private val TRIGGER_SPEC = mapOf(
    "WhenAPermanentEntersTheBattlefield" to "Triggers.EntersBattlefield",
    "WhenACreatureOrPlaneswalkerDies" to "Triggers.Dies",
    "WhenACreatureAttacks" to "Triggers.Attacks",
    "WhenACreatureBlocks" to "Triggers.Blocks",
    "WhenACreatureDealsCombatDamageToAPlayer" to "Triggers.DealsCombatDamageToPlayer",
    "WhenACreatureBecomesBlocked" to "Triggers.BecomesBlocked",
)

/** A TriggerA rule (self-triggered) -> triggeredAbility { trigger; [triggerCondition]; [target]; effect }.
 *  [oncePerTurn] is set by the `TriggerOnceEachTurn` rule envelope, whose body is otherwise shaped
 *  identically to a TriggerA. [triggerCondition] is an optional intervening-if condition DSL (CR 603.4)
 *  supplied by an enclosing gate (e.g. the "while saddled" `If` wrapper passes
 *  `Conditions.SourceIsSaddled`); it renders as a `triggerCondition = …` line. */
internal fun EmitCtx.triggerBlock(rule: JsonObject, oncePerTurn: Boolean = false, triggerCondition: String? = null): List<Stmt>? {
    // A modal triggered ability ("When this enters, choose one — …") carries a `Modal_*` envelope; the
    // generic action path would render only the first mode and silently drop the rest, so decline the
    // whole ability rather than emit one arm (mirrors the spell-side modal guard).
    if ("\"Modal_" in compact(rule)) { reasons.add("modal-trigger"); return null }

    // A Mount's "while saddled" attack trigger nests the intervening-if (CR 603.4) *inside* the
    // TriggerA's trigger slot: `args[0]` is an `If { <gate> } <realTrigger>` node rather than a bare
    // `_Trigger`. Recognise the exact "this permanent is saddled" gate, unwrap it to the inner real
    // trigger, and thread `Conditions.SourceIsSaddled` as the triggerCondition (Drover Grizzly, Giant
    // Beaver). Any other `If` gate leaves the rule untouched so triggerSpecFor declines -> SCAFFOLD; we
    // never silently drop or widen the gate.
    val (effRule, effTriggerCondition) = unwrapIfGatedTrigger(rule, triggerCondition)
    val spec = triggerSpecFor(effRule) ?: run { reasons.add("trigger-shape"); return null }
    val (targets, actions) = extractEnvelope(rule)
    if (actions == null) { reasons.add("trigger-actions"); return null }
    val (tnode, tvar) = spellTargetExpr(targets, actions) ?: return null

    // "you may [do X]" on a triggered ability is an OPTIONAL ability (declined at announcement /
    // by choosing no targets), not a resolution-time MayEffect. Unwrap a lone MayAction so the
    // ability carries `optional = true` and a plain effect — the engine's idiom for "may [target]".
    val mayWrapped = actions.singleOrNull()?.strField("_Action") == "MayAction"
    val mayInner = if (mayWrapped) innerAction(actions.single()) ?: return null else null
    val effectActions = if (mayWrapped) listOf(mayInner!!) else actions
    // Some effects already carry their own "you may" choice (putFromHand prompts whether to put the
    // card), so the MayAction wrapper is absorbed by the effect, not re-expressed as `optional = true`
    // (which would double-wrap vs the golden — Elvish Pioneer).
    val selfOptional = mayInner?.strField("_Action") in SELF_OPTIONAL_ACTIONS

    // An intervening-if written in the trigger's effect — "Whenever ~ attacks, create a token IF you
    // control a creature with power 4 or greater" (Scalestorm Summoner) — is modeled in mtgish as a lone
    // `If{cond}[then]` action, not a TriggerI. Per CR 603.4 this IS an intervening-if ability: the
    // condition is checked when the trigger would fire and again on resolution. Lift it to a
    // `triggerCondition = …` gate over the then-branch, but ONLY when the condition renders via the same
    // strict [interveningIfDsl] used by TriggerI (no else-branch, single If). Any other shape falls
    // through to the normal action path (where `on("If")` handles the static gates or declines).
    val lifted = if (effTriggerCondition == null) liftInterveningIfAction(effectActions) else null
    val condFromIf = lifted?.first
    // A spell-cast trigger's triggering entity is the spell, so "that player" in the body is the caster
    // (ControllerOfTriggeringEntity), not the triggering player — see [EmitCtx.triggeringEntityIsSpell].
    val prevTriggeringSpell = triggeringEntityIsSpell
    triggeringEntityIsSpell = jsonContains(effRule["args"].asArr?.firstOrNull(), "_Trigger", "WhenAPlayerCastsASpell")
    val edsl = renderEffectList(lifted?.second ?: effectActions, tvar).also { triggeringEntityIsSpell = prevTriggeringSpell } ?: return null

    val stmts = mutableListOf<Stmt>(Assign("trigger", Lit(spec)))
    val triggerCond = effTriggerCondition ?: condFromIf
    if (triggerCond != null) stmts.add(Assign("triggerCondition", Lit(triggerCond)))
    if (oncePerTurn) stmts.add(Assign("oncePerTurn", Lit("true")))
    if (mayWrapped && !selfOptional) stmts.add(Assign("optional", Lit("true")))
    if (tvar != null) stmts.add(targetLocal(tnode!!))
    stmts.add(Assign("effect", edsl))
    return listOf(Sub(Block("triggeredAbility", stmts)))
}

/**
 * A `TriggerA` rule granted to a *token* ("Create a … token with '<trigger>'", Send in the Pest's Pest
 * with "Whenever this token attacks, you gain 1 life.") -> a `TriggeredAbility.create(trigger = …,
 * binding = …, effect = …)` constructor expression for `CreateTokenEffect.triggeredAbilities`. The
 * sibling of [grantedActivatedAbilityExpr] for the granted-*triggered* shape.
 *
 * Only the SELF-bound, untargeted, plain shape renders — the trigger spec comes from [triggerSpecFor]
 * (so the token trigger's binding is the same SELF binding the card-body path uses), and the effect
 * list is rendered with `EffectTarget.Self`. Any complication the inline `TriggeredAbility.create`
 * form can't carry — a chosen target, an intervening-if / modal / "you may" wrapper — declines
 * (-> the caller scaffolds the whole token) rather than emit a lossy granted ability.
 */
internal fun EmitCtx.grantedTriggeredAbilityExpr(rule: JsonObject): Dsl? {
    if ("\"Modal_" in compact(rule)) return null
    val spec = triggerSpecFor(rule) ?: return null
    val (targets, actions) = extractEnvelope(rule)
    if (actions == null) return null
    if (targets != null && targets.isNotEmpty()) return null  // a targeted granted trigger -> SCAFFOLD
    // No "you may" / intervening-if rendering on the inline create() form — decline those shapes.
    if (actions.any { it.strField("_Action") == "MayAction" || it.strField("_Action") == "If" }) return null
    val effect = renderEffectList(actions, tvar = "EffectTarget.Self") ?: return null
    return call(
        "TriggeredAbility.create",
        arg("trigger", "$spec.event"),
        arg("binding", "$spec.binding"),
        arg("effect", effect),
    )
}

/**
 * Some triggers nest an intervening-if (CR 603.4) *inside* the TriggerA's trigger slot: `args[0]` is an
 * `If` node whose `args` are `[<gate condition>, <real _Trigger>]` rather than a bare `_Trigger`. When
 * the gate is one we can render exactly, unwrap to the inner real trigger and thread the matching
 * `triggerCondition`; the actions in `args[1..]` are untouched. Two recognised gates:
 *  - "this permanent is saddled" (`PermanentPassesFilter(ThisPermanent, IsSaddled)`) ->
 *    `Conditions.SourceIsSaddled` (Drover Grizzly, Giant Beaver).
 *  - any gate `interveningIfDsl` renders, e.g. "during your turn" (`IsPlayersTurn(You)`) ->
 *    `Conditions.IsYourTurn` (Overzealous Muscle).
 * Any other gate (or no `If`) returns the rule unchanged with the caller's existing condition, so
 * `triggerSpecFor` then declines an unrecognised `If` -> SCAFFOLD. Never widens or drops the gate.
 */
private fun EmitCtx.unwrapIfGatedTrigger(rule: JsonObject, existing: String?): Pair<JsonObject, String?> {
    val args = rule["args"].asArr ?: return rule to existing
    val trig = args.getOrNull(0) as? JsonObject ?: return rule to existing
    if (trig.strField("_Trigger") != "If") return rule to existing
    val ifArgs = trig["args"].asArr ?: return rule to existing
    val gate = ifArgs.getOrNull(0) as? JsonObject
    val innerTrigger = ifArgs.getOrNull(1) as? JsonObject ?: return rule to existing
    val isSaddledGate = gate?.strField("_Condition") == "PermanentPassesFilter" &&
        jsonContains(gate, "_Permanent", "ThisPermanent") &&
        jsonContains(gate, "_Permanents", "IsSaddled")
    val condDsl = if (isSaddledGate) "Conditions.SourceIsSaddled" else interveningIfDsl(gate)
        ?: return rule to existing
    val rewritten = buildJsonObject {
        rule.forEach { (k, v) -> if (k != "args") put(k, v) }
        put("args", JsonArray(listOf<JsonElement>(innerTrigger) + args.drop(1)))
    }
    return rewritten to (existing ?: condDsl)
}

/**
 * A `TriggerI` rule (triggered ability with an intervening-if) -> triggeredAbility { trigger;
 * triggerCondition; [target]; effect }. The rule's args are [trigger, condition, actions]: the
 * trigger reuses [triggerSpecFor], the condition is rendered by [interveningIfDsl] (only the exact
 * shapes we can express render; anything else declines to a scaffold), and the actions reuse the
 * normal trigger-body path. Canyon Crab: "At the beginning of your end step, if you haven't cast a
 * spell from your hand this turn, draw a card, then discard a card."
 */
internal fun EmitCtx.triggerIBlock(rule: JsonObject): List<Stmt>? {
    val args = rule["args"].asArr ?: run { reasons.add("TriggerI"); return null }
    val cond = args.getOrNull(1) as? JsonObject
    val condDsl = interveningIfDsl(cond) ?: run { reasons.add("TriggerI"); return null }
    // Re-key the TriggerI rule's [trigger, condition, actions] into a TriggerA-shaped [trigger, actions]
    // so the shared triggerBlock recovers the spec + body; the condition is threaded separately.
    val triggerA = buildJsonObject {
        put("_Rule", JsonPrimitive("TriggerA"))
        put("args", JsonArray(listOfNotNull(args.getOrNull(0)) + listOfNotNull(args.getOrNull(2))))
    }
    return triggerBlock(triggerA, triggerCondition = condDsl)
}

/**
 * A trigger effect that is a single `If{cond}[then]` action -> the intervening-if condition DSL plus
 * the then-branch actions, or null when it isn't a lone liftable `If`. Used by [triggerBlock] to model
 * "Whenever ~ attacks, create a token if you control …" (Scalestorm Summoner) as a `triggerCondition`
 * gate (CR 603.4). Declines (returns null, leaving the normal action path to handle/decline the `If`)
 * when: there's more than one action, the action isn't an `If`, the condition doesn't render via the
 * strict [interveningIfDsl], or the `If` carries an else-branch (which a single triggerCondition can't
 * express). Never drops the condition or an else-branch.
 */
private fun EmitCtx.liftInterveningIfAction(actions: List<JsonObject>): Pair<String, List<JsonObject>>? {
    val only = actions.singleOrNull() ?: return null
    if (only.strField("_Action") != "If") return null
    val args = only["args"].asArr ?: return null
    val cond = args.getOrNull(0) as? JsonObject ?: return null
    val thenActions = (args.getOrNull(1) as? JsonArray)?.filterIsInstance<JsonObject>() ?: return null
    if (thenActions.isEmpty()) return null
    // An else-branch (args[2]) can't be folded into a single intervening-if gate — decline.
    if (args.getOrNull(2) != null) return null
    val condDsl = interveningIfDsl(cond) ?: return null
    return condDsl to thenActions
}

/**
 * An intervening-if condition node -> a `Conditions.*` DSL string, or null (-> SCAFFOLD) for any
 * condition shape we can't express exactly. Only the shapes our calibrated cards need are
 * recognised; declining beats widening. A top-level `And` composes its arms with `Conditions.All`
 * (each arm must itself render). The single-condition shapes:
 *  - `PlayerPassesFilter(You, HasntCastASpellThisTurn(WasCastFromTheirHand))` ("you haven't cast a
 *    spell from your hand this turn") -> `Conditions.Not(Conditions.YouCastSpellsThisTurn(1, fromZone = Zone.HAND))`
 *    (Canyon Crab).
 *  - `PermanentPassesFilter(ThisPermanent, HasNoCountersOfType(<counter>))` ("this creature doesn't
 *    have a <counter> counter on it") -> `Conditions.Not(Conditions.SourceHasCounter(CounterTypeFilter.Named("<counter>")))`
 *    (Inventive Wingsmith).
 *  - `PlayerPassesFilter(You, ControlsA(And(Other(ThatEnteringPermanent), IsAnOutlaw)))` ("you
 *    control another outlaw") -> `Conditions.YouControlAtLeast(2, <outlaw filter>)` — the entering
 *    permanent is itself an outlaw, so "another outlaw" = "two or more outlaws" (Mine Raider).
 *  - `PlayerPassesFilter(You, ControlsA(<filter>))` ("you control a <filter>") ->
 *    `Conditions.YouControl(<filter>)` (Beastbond Outcaster's "a creature with power 4 or greater").
 */
internal fun EmitCtx.interveningIfDsl(cond: JsonObject?): String? {
    if (cond == null) return null
    // Top-level And -> Conditions.All(arm, arm, ...); every arm must render or the whole declines.
    if (cond.strField("_Condition") == "And") {
        val arms = cond["args"].asArr?.filterIsInstance<JsonObject>() ?: return null
        if (arms.size < 2) return null
        val rendered = arms.map { interveningIfDsl(it) ?: return null }
        return "Conditions.All(${rendered.joinToString(", ")})"
    }
    return singleInterveningIfDsl(cond)
}

/** One non-And intervening-if condition node -> its `Conditions.*` DSL, or null (-> SCAFFOLD). */
private fun EmitCtx.singleInterveningIfDsl(cond: JsonObject): String? {
    // "during your turn" — IsPlayersTurn(You) -> Conditions.IsYourTurn (Overzealous Muscle's
    // "Whenever you commit a crime during your turn, …"). Only the You scope renders; any other
    // player scope (an opponent's turn) has no calibrated DSL constant yet, so it declines -> SCAFFOLD.
    if (cond.strField("_Condition") == "IsPlayersTurn" &&
        jsonContains(cond, "_Player", "You")
    ) {
        return "Conditions.IsYourTurn"
    }
    // "if it's your turn" — PlayerPassesFilter(You, IsTheirTurn): the player "you" passes the
    // "it's their turn" filter, i.e. it's your turn (Rapier Wit's "If it's your turn, put a stun
    // counter on it"). Only the You scope renders; an opponent scope has no calibrated DSL constant.
    if (cond.strField("_Condition") == "PlayerPassesFilter" &&
        jsonContains(cond, "_Player", "You") &&
        jsonContains(cond, "_Players", "IsTheirTurn")
    ) {
        return "Conditions.IsYourTurn"
    }
    if (cond.strField("_Condition") == "PlayerPassesFilter" &&
        jsonContains(cond, "_Player", "You") &&
        jsonContains(cond, "_Players", "HasntCastASpellThisTurn") &&
        jsonContains(cond, "_Spells", "WasCastFromTheirHand")
    ) {
        return "Conditions.Not(Conditions.YouCastSpellsThisTurn(1, fromZone = Zone.HAND))"
    }
    // "this creature doesn't have a <counter> counter on it" — PermanentPassesFilter(ThisPermanent,
    // HasNoCountersOfType(<counter>)). Only the keyword/named counters we can name render; the source
    // ref must be ThisPermanent.
    if (cond.strField("_Condition") == "PermanentPassesFilter" &&
        jsonContains(cond, "_Permanent", "ThisPermanent") &&
        jsonContains(cond, "_Permanents", "HasNoCountersOfType")
    ) {
        val counter = counterNameForFilter(cond) ?: return null
        return "Conditions.Not(Conditions.SourceHasCounter(CounterTypeFilter.Named(\"$counter\")))"
    }
    // "if a creature died this turn" — ACreatureOrPlaneswalkerDiedThisTurn over a bare creature-cardtype
    // filter (Rictus Robber). Only the unrestricted "a creature" shape (no controller / subtype / count
    // clause) maps to Conditions.CreatureDiedThisTurn; anything more specific declines -> SCAFFOLD.
    if (cond.strField("_Condition") == "ACreatureOrPlaneswalkerDiedThisTurn") {
        val filter = cond["args"] as? JsonObject
        val bareCreature = filter?.strField("_Permanents") == "IsCardtype" &&
            filter.field("args").asStr() == "Creature"
        return if (bareCreature) "Conditions.CreatureDiedThisTurn" else null
    }
    // "if you put a counter on this creature this turn" — PlayerPassesFilter(You,
    // HasPutACounterOnAPermanentThisTurn(SinglePermanent(ThisPermanent))) (Fractal Tender's end-step
    // gate). Only the exact ThisPermanent subject maps to Conditions.SourceReceivedCounterThisTurn;
    // a wider permanent filter (any permanent, a group) declines -> SCAFFOLD rather than over-fire.
    if (cond.strField("_Condition") == "PlayerPassesFilter" &&
        jsonContains(cond, "_Player", "You") &&
        jsonContains(cond, "_Players", "HasPutACounterOnAPermanentThisTurn")
    ) {
        val perms = ((cond["args"].asArr?.getOrNull(1)) as? JsonObject)?.get("args") as? JsonObject
        val isThisPermanent = perms?.strField("_Permanents") == "SinglePermanent" &&
            perms.field("args").strField("_Permanent") == "ThisPermanent"
        return if (isThisPermanent) "Conditions.SourceReceivedCounterThisTurn" else null
    }
    // "if you gained life this turn" — PlayerPassesFilter(You, GainedLifeThisTurn) (Foolish Fate's
    // Infusion clause). No count/amount sub-clause, so the bare "you gained life this turn" maps to
    // Conditions.YouGainedLifeThisTurn.
    if (cond.strField("_Condition") == "PlayerPassesFilter" &&
        jsonContains(cond, "_Player", "You") &&
        jsonContains(cond, "_Players", "GainedLifeThisTurn")
    ) {
        return "Conditions.YouGainedLifeThisTurn"
    }
    // "if you've cast another instant or sorcery spell this turn" — PlayerPassesFilter(You,
    // CastASpellThisTurn(And(Other(ThisSpell), Or(IsCardtype Instant, IsCardtype Sorcery)))) (Burrog
    // Barrage). The Other(ThisSpell) self-exclusion means "another", and this spell is itself an
    // instant/sorcery, so "another instant or sorcery" = "two or more instant/sorcery spells this turn"
    // -> Conditions.YouCastSpellsThisTurn(2, GameObjectFilter.InstantOrSorcery). Only the exact
    // Other(ThisSpell) + instant-or-sorcery shape renders; anything else declines -> SCAFFOLD.
    youCastAnotherInstantOrSorceryDsl(cond)?.let { return it }
    // "you control another outlaw" — ControlsA over And(Other(ThatEnteringPermanent), IsAnOutlaw). The
    // entering permanent is itself an outlaw, so this is exactly "two or more outlaws you control".
    youControlAnotherOutlawDsl(cond)?.let { return it }
    // "you control a <filter>" — reuse the static-gate renderer (PlayerPassesFilter(You, ControlsA(filter))).
    youControlConditionDsl(cond)?.let { return it }
    return null
}

/** `PlayerPassesFilter(You, CastASpellThisTurn(And(Other(ThisSpell), Or(IsCardtype Instant, IsCardtype
 *  Sorcery))))` ("you've cast another instant or sorcery spell this turn") ->
 *  `Conditions.YouCastSpellsThisTurn(2, GameObjectFilter.InstantOrSorcery)`, else null. The
 *  Other(ThisSpell) self-exclusion plus this spell itself being an instant/sorcery is what makes
 *  "another instant or sorcery" equal "two or more instant/sorcery spells cast this turn" — the spell
 *  on the stack is already counted in the cast tracker when it resolves (Burrog Barrage). */
private fun EmitCtx.youCastAnotherInstantOrSorceryDsl(cond: JsonObject): String? {
    if (cond.strField("_Condition") != "PlayerPassesFilter") return null
    val args = cond["args"].asArr ?: return null
    if ((args.getOrNull(0) as? JsonObject)?.strField("_Player") != "You") return null
    val cast = args.getOrNull(1) as? JsonObject ?: return null
    if (cast.strField("_Players") != "CastASpellThisTurn") return null
    val blob = compact(cast)
    // Must be the "another" (Other ThisSpell) self-exclusion over an instant-or-sorcery filter.
    if ("\"Other\"" !in blob || "ThisSpell" !in blob) return null
    if (!(blob.contains("\"Instant\"") && blob.contains("\"Sorcery\""))) return null
    return "Conditions.YouCastSpellsThisTurn(2, GameObjectFilter.InstantOrSorcery)"
}

/** The "<counter> counter" name for a `HasNoCountersOfType(<CounterType>)` node, mapped to the engine's
 *  `CounterTypeFilter.Named` string, or null for a counter kind we don't name. */
private fun counterNameForFilter(cond: JsonObject): String? {
    val noCounters = cond.nodesTagged("HasNoCountersOfType").firstOrNull() ?: return null
    return when ((noCounters["args"] as? JsonObject)?.strField("_CounterType")) {
        "FlyingCounter" -> "flying"
        else -> null
    }
}

/** `PlayerPassesFilter(You, ControlsA(And(Other(ThatEnteringPermanent), IsAnOutlaw)))` ("you control
 *  another outlaw") -> `Conditions.YouControlAtLeast(2, <outlaw filter>)`, else null. The
 *  Other(ThatEnteringPermanent) self-exclusion + the entering permanent being an outlaw is what makes
 *  "another outlaw" equivalent to "two or more outlaws you control" (Mine Raider). */
private fun EmitCtx.youControlAnotherOutlawDsl(cond: JsonObject): String? {
    if (cond.strField("_Condition") != "PlayerPassesFilter") return null
    val args = cond["args"].asArr ?: return null
    if ((args.getOrNull(0) as? JsonObject)?.strField("_Player") != "You") return null
    val controls = args.getOrNull(1) as? JsonObject ?: return null
    if (controls.strField("_Players") != "ControlsA") return null
    val blob = compact(controls)
    if ("IsAnOutlaw" !in blob) return null
    // Must be the "another" (Other) self-exclusion shape; a plain "an outlaw" would not be "another".
    if ("\"Other\"" !in blob) return null
    return "Conditions.YouControlAtLeast(2, GameObjectFilter.Creature.withAnyOfSubtypes(Subtype.OUTLAW_TYPES))"
}

/** The triggered-ability trigger spec for a TriggerA / TriggerOnceEachTurn rule, or null (-> SCAFFOLD)
 *  for a trigger shape we can't render exactly. The first arg of a TriggerA rule is the `_Trigger`
 *  node; scoping checks to it (not the whole rule) keeps an action's `You`/`ThisPermanent` markers from
 *  being mistaken for trigger scopes. */
private fun EmitCtx.triggerSpecFor(rule: JsonObject): String? {
    val trig = rule["args"].asArr?.firstOrNull() as? JsonObject ?: return null

    // A compound `Or` of two triggers ("Whenever this creature OR another creature you control with
    // flying enters") can't be expressed as one filter+binding — a nested-trigger scan would match the
    // inner enter-trigger and emit a single OTHER-binding trigger that drops the self arm. Decline.
    if (trig.strField("_Trigger") == "Or") return null

    // SELF self-triggers (this permanent enters / dies / attacks / deals combat damage to a player).
    // `isSelf` distinguishes "the subject IS this permanent" from an `Other(ThisPermanent)` clause
    // ("another …"), which contains ThisPermanent only as the exclusion reference.
    for ((mtTrigger, dsl) in TRIGGER_SPEC) {
        if (jsonContains(trig, "_Trigger", mtTrigger) && isSelf(trig)) return dsl
    }

    // Non-self "a creature you control dies" deliberately declines -> SCAFFOLD, and the boundary is
    // worth stating because it is tempting to "fix": the modern once-per-batch "whenever one or more
    // other creatures you control die" (Vengeful Townsfolk) and the per-creature "whenever another
    // creature you control dies" (Unruly Mob, Rot Shambler, Catacomb Sifter, Pitiless Plunderer, …)
    // flatten to the *identical* mtgish node — WhenACreatureOrPlaneswalkerDies over
    // And(Other(ThisPermanent), IsCardtype Creature, ControlledByAPlayer You). The IR carries no
    // "one or more" quantifier, so the two can't be told apart here, yet they behave differently (a
    // board wipe fires the batch trigger once but the per-each trigger once per creature). Rendering
    // either way would mis-author the ~55 per-each cards that share this node, so we decline. The
    // engine supports both shapes (Triggers.OneOrMoreCreaturesYouControlDie /
    // Triggers.YourCreatureDies) and the bridge still scores these cards coverable — choosing between
    // them is a human call (the add-card scenario test is the real gate).
    if (jsonContains(trig, "_Trigger", "WhenACreatureOrPlaneswalkerDies") && !isSelf(trig)) return null

    // "Whenever this creature becomes saddled for the first time each turn" (CR 702.171b — Stubborn
    // Burrowfiend). The IR tag bakes in the once-per-turn semantics, so it always renders the
    // first-time-each-turn trigger over the SELF (ThisPermanent) subject; a non-self subject has no
    // calibrated ANY-binding card yet, so it declines -> SCAFFOLD rather than guess a filter.
    if (jsonContains(trig, "_Trigger", "WhenAPermanentBecomesSaddledForTheFirstTimeInATurn") && isSelf(trig))
        return "Triggers.becomesSaddled(firstTimeEachTurn = true)"

    // "Whenever ~ deals damage" / "Whenever ~ is dealt damage" (SELF) — paired with a "that much"
    // gain/lose-life or token effect.
    if (jsonContains(trig, "_Trigger", "WhenAPermanentDealsDamage") && isSelf(trig))
        return "Triggers.DealsDamage"
    if (jsonContains(trig, "_Trigger", "WhenAPermanentIsDealtDamage") && isSelf(trig))
        return "Triggers.TakesDamage"

    // Phase/step triggers. "your upkeep" is scoped to You; "each upkeep" to any player; "each
    // opponent's upkeep" to Opponent. The host-relative scopes (HostController / HostPlayer, an Aura
    // granting an upkeep trigger to the enchanted permanent's controller) decline -> SCAFFOLD, as do
    // the niche dynamic scopes (TheChosenPlayer, ControllerOfSpell, ...).
    if (jsonContains(trig, "_Trigger", "AtTheBeginningOfAPlayersUpkeep")) {
        if (jsonContains(trig, "_Player", "You")) return "Triggers.YourUpkeep"
        if (jsonContains(trig, "_Players", "AnyPlayer")) return "Triggers.EachUpkeep"
        if (jsonContains(trig, "_Players", "Opponent")) return "Triggers.EachOpponentUpkeep"
    }
    // "your end step" is scoped to You (a SinglePlayer(You) subject); "each end step" to any player.
    // The opponent / host-relative end-step scopes have no matching Triggers.* constant yet, so they
    // decline -> SCAFFOLD, mirroring the upkeep block above (which has an EachOpponentUpkeep but no
    // end-step counterpart exists).
    if (jsonContains(trig, "_Trigger", "AtTheBeginningOfAPlayersEndStep")) {
        if (jsonContains(trig, "_Player", "You")) return "Triggers.YourEndStep"
        if (jsonContains(trig, "_Players", "AnyPlayer")) return "Triggers.EachEndStep"
    }

    // "Whenever a player cycles a card" (any player) — Fleeting Aven, Invigorating Boon.
    if (jsonContains(trig, "_Trigger", "WhenAPlayerCyclesACard") && jsonContains(trig, "_Players", "AnyPlayer"))
        return "Triggers.AnyPlayerCycles"

    // "Whenever this creature becomes the target of a spell or ability an opponent controls"
    // (Cactarantula). The trigger's args is a 2-tuple [subject, spell/ability-filter]; the subject must
    // be SinglePermanent(ThisPermanent) and the filter an opponent-controlled spell/ability. Only that
    // exact self + opponent shape maps to Triggers.BecomesTargetByOpponent; anything else (a filtered
    // permanent group, your own spells, a count clause) declines -> SCAFFOLD.
    if (jsonContains(trig, "_Trigger", "WhenAPermanentBecomesTheTargetOfASpellOrAbility")) {
        val targs = trig["args"].asArr ?: return null
        val subject = targs.getOrNull(0) as? JsonObject
        val selfSubject = subject?.strField("_Permanents") == "SinglePermanent" &&
            subject.field("args").strField("_Permanent") == "ThisPermanent"
        val filter = targs.getOrNull(1) as? JsonObject
        val opponentControlled = filter?.strField("_SpellsAndAbilities") == "ControlledByAPlayer" &&
            filter.field("args").strField("_Players") == "Opponent"
        if (selfSubject && opponentControlled) return "Triggers.BecomesTargetByOpponent"
        return null
    }

    // "When this <permanent> is put into a graveyard from the battlefield, …" — the self
    // leaves-to-graveyard trigger (Reach for the Sky's "draw a card"). The args are
    // [subject, players]; only the SELF subject (SinglePermanent(ThisPermanent)) maps to
    // Triggers.PutIntoGraveyardFromBattlefield. A filtered / other-permanent subject ("whenever
    // ANOTHER … is put into a graveyard") has no matching self-trigger constant, so it declines
    // -> SCAFFOLD rather than mis-bind the trigger to the wrong permanent.
    if (jsonContains(trig, "_Trigger", "WhenAPermanentIsPutIntoAPlayersGraveyard")) {
        return if (isSelf(trig)) "Triggers.PutIntoGraveyardFromBattlefield" else null
    }

    // "Whenever a creature attacks" — only the unrestricted any-creature shape (no subtype / controller /
    // count clause), which maps to a filterless ANY-binding attacks trigger (Righteous Cause).
    if (jsonContains(trig, "_Trigger", "WhenACreatureAttacks") && isPlainCreatureFilter(trig))
        return "Triggers.attacks(binding = TriggerBinding.ANY)"

    // "Whenever you attack with N or more creatures" — WhenAPlayerAttacksWithANumberOfCreatures scoped to
    // You + a `>= N` comparison + a plain creature filter (Overwhelming Instinct). Only the
    // greater-than-or-equal (N-or-more) shape maps to YouAttackEvent(minAttackers); any other comparison
    // or a typed/controlled attacker filter declines.
    if (jsonContains(trig, "_Trigger", "WhenAPlayerAttacksWithANumberOfCreatures") &&
        jsonContains(trig, "_Player", "You") && jsonContains(trig, "_Comparison", "GreaterThanOrEqualTo")
    ) {
        val blob = compact(trig)
        val plainCreature = "Creature" in trig.argWordsTagged("IsCardtype") &&
            "IsCreatureType" !in blob && "ControlledByAPlayer" !in blob && "\"Other\"" !in blob && "_Color" !in blob
        val n = findInteger(trig) as? Int
        if (plainCreature && n != null) return "TriggerSpec(EventPattern.YouAttackEvent(minAttackers = $n), TriggerBinding.ANY)"
    }

    // "Whenever a [filtered] permanent enters the battlefield" (the SELF case returned above): an
    // `Other(ThisPermanent)` clause means "another …" -> OTHER binding (Elvish Vanguard's "another
    // Elf", Wretched Anurid's "another creature"); otherwise "a …" -> ANY (Wirewood Savage's "a Beast").
    if (jsonContains(trig, "_Trigger", "WhenAPermanentEntersTheBattlefield")) {
        val binding = if (jsonContains(trig, "_Permanents", "Other")) "TriggerBinding.OTHER" else "TriggerBinding.ANY"
        val filter = gameObjectFilterDsl(trig) ?: return null
        return "Triggers.entersBattlefield(filter = $filter, binding = $binding)"
    }

    // "Whenever a [creature type] deals combat damage to a player, …" — non-self
    // WhenACreatureDealsCombatDamageToAPlayer whose source filter is purely a creature subtype, to any
    // player (Cabal Slaver's "a Goblin"). Anything beyond a bare subtype (controller / colour / count /
    // "another") declines so we never widen the source filter.
    if (jsonContains(trig, "_Trigger", "WhenACreatureDealsCombatDamageToAPlayer") && !isSelf(trig) &&
        jsonContains(trig, "_Players", "AnyPlayer")
    ) {
        val subtype = creatureTypeIn(trig)
        val blob = compact(trig)
        val bareSubtype = subtype != null && "ControlledByAPlayer" !in blob &&
            "_Color" !in blob && "_Comparison" !in blob && "\"Other\"" !in blob
        if (bareSubtype) return "TriggerSpec(EventPattern.DealsDamageEvent(damageType = DamageType.Combat, " +
            "recipient = RecipientFilter.AnyPlayer, sourceFilter = GameObjectFilter.Creature.withSubtype(\"$subtype\")), " +
            "TriggerBinding.ANY)"
    }

    // "Whenever you cast your Nth spell each turn" — WhenAPlayerCastsTheirNthSpellInATurn. The args are
    // [caster scope, an `== N` comparison, a spell filter]. Only the exact You + EqualTo + AnySpell shape
    // maps to Triggers.NthSpellCast(N, Player.You) (Rodeo Pyromancers' "first spell each turn"); any other
    // caster scope, comparison, or a typed/constrained spell filter declines -> SCAFFOLD rather than
    // silently widening it.
    if (jsonContains(trig, "_Trigger", "WhenAPlayerCastsTheirNthSpellInATurn")) {
        val argv = trig["args"].asArr
        val scope = castScope(argv?.getOrNull(0) as? JsonObject)
        val comparison = argv?.getOrNull(1) as? JsonObject
        val spells = argv?.getOrNull(2) as? JsonObject
        val n = (comparison?.field("args") as? JsonElement).let { findInteger(it) } as? Int
        if (scope == CastScope.YOU &&
            comparison?.strField("_Comparison") == "EqualTo" &&
            spells?.strField("_Spells") == "AnySpell" &&
            n != null
        ) {
            return "Triggers.NthSpellCast($n, Player.You)"
        }
        return null
    }

    // "Whenever {you / a player / an opponent} casts a [type] spell" — WhenAPlayerCastsASpell. The
    // first arg is the caster scope (You / AnyPlayer / Opponent — anything else, e.g. HostPlayer,
    // declines); the second is the spell filter, classified to an EXACT category. A filter carrying
    // any extra constraint (mana value, colour, targets, kicked, name, a mixed And/Or) yields a null
    // category and declines -> SCAFFOLD, rather than silently dropping the clause.
    if (jsonContains(trig, "_Trigger", "WhenAPlayerCastsASpell")) {
        val argv = trig["args"].asArr
        val scope = castScope(argv?.getOrNull(0) as? JsonObject) ?: return null
        val spellsNode = argv?.getOrNull(1) as? JsonObject
        // "an instant or sorcery spell that targets a creature" (Repartee — Forum Necroscribe,
        // Lecturing Scornmage): an And of the base spell-type filter + a `TargetsAPermanent`
        // clause. Recover the base category and a `targetsMatching(<filter>)` subfilter; the
        // whole thing renders only when BOTH render exactly (decline -> SCAFFOLD otherwise).
        spellCastTargetsMatching(spellsNode)?.let { (category, sub) ->
            return castTriggerDsl(scope, category, targetsMatching = sub)
        }
        val category = spellCastCategory(spellsNode) ?: return null
        return castTriggerDsl(scope, category)
    }

    // "Whenever you commit a crime" — WhenAPlayerCommitsACrime scoped to SinglePlayer(You). Only the
    // You scope maps to Triggers.YouCommitCrime; any other player scope (AnyPlayer / Opponent) has no
    // matching Triggers.* constant yet, so it declines -> SCAFFOLD. Pairs with the TriggerOnceEachTurn
    // envelope for "this ability triggers only once each turn" (Marauding Sphinx).
    if (jsonContains(trig, "_Trigger", "WhenAPlayerCommitsACrime") && jsonContains(trig, "_Player", "You"))
        return "Triggers.YouCommitCrime"

    // "Whenever one or more cards leave your graveyard" — WhenAnyNumberOfGraveyardCardsLeave over
    // InAPlayersGraveyard(SinglePlayer(You)). The batching leave-graveyard trigger fires once per
    // event batch. Only the You scope maps to Triggers.CardsLeaveYourGraveyard(); any other graveyard
    // owner has no matching Triggers.* constant yet, so it declines -> SCAFFOLD. The unfiltered shape
    // (no `_Cards`/`IsCardtype` constraint) renders the filterless any-card form (Owlin Historian,
    // Attuned Hunter). A constrained leave-graveyard (a typed card) declines rather than widening.
    if (jsonContains(trig, "_Trigger", "WhenAnyNumberOfGraveyardCardsLeave") &&
        jsonContains(trig, "_Player", "You")
    ) {
        val blob = compact(trig)
        val unfiltered = "IsCardtype" !in blob && "IsCreatureType" !in blob &&
            "_Color" !in blob && "IsCardname" !in blob
        if (unfiltered) return "Triggers.CardsLeaveYourGraveyard()"
        return null
    }

    return null
}

/** The caster scope of a WhenAPlayerCastsASpell trigger, or null for a scope we don't render
 *  (HostPlayer, EnchantedPlayer, …). Read from the trigger's own first arg, NOT a whole-trigger
 *  search — a `_Player: You` buried in the spell filter (e.g. WasCastFromAPlayersGraveyard(You))
 *  must not be mistaken for the caster. */
private enum class CastScope { YOU, ANY, OPPONENT }

private fun castScope(players: JsonObject?): CastScope? = when (players?.strField("_Players")) {
    "AnyPlayer" -> CastScope.ANY
    "Opponent" -> CastScope.OPPONENT
    "SinglePlayer" -> if (players.field("args").strField("_Player") == "You") CastScope.YOU else null
    else -> null
}

/** Canonical category for a WhenAPlayerCastsASpell spell-filter node, or null when the filter carries
 *  any constraint we can't render exactly. Strict by design: decline -> SCAFFOLD beats dropping a clause. */
private fun spellCastCategory(spells: JsonObject?): String? = when (spells?.strField("_Spells")) {
    "AnySpell" -> "any"
    "IsHistoric" -> "historic"
    "IsCardtype" -> when (spells.field("args").asStr()) {
        "Creature" -> "creature"
        "Enchantment" -> "enchantment"
        else -> null
    }
    "IsNonCardtype" -> if (spells.field("args").asStr() == "Creature") "noncreature" else null
    // "an instant or sorcery spell" — an Or of exactly the two cardtype clauses, nothing else.
    "Or" -> {
        val parts = spells["args"].asArr.orEmpty().map { it as? JsonObject }
        val types = parts.map { if (it?.strField("_Spells") == "IsCardtype") it.field("args").asStr() else null }
        if (types.none { it == null } && types.filterNotNull().toSet() == setOf("Instant", "Sorcery")) "instantOrSorcery" else null
    }
    else -> null
}

/** The base [GameObjectFilter] expression for a cast-trigger category, or null for "any" (no filter). */
private fun categoryFilter(category: String): String? = when (category) {
    "any" -> null
    "creature" -> "GameObjectFilter.Creature"
    "noncreature" -> "GameObjectFilter.Noncreature"
    "enchantment" -> "GameObjectFilter.Enchantment"
    "instantOrSorcery" -> "GameObjectFilter.InstantOrSorcery"
    "historic" -> "GameObjectFilter.Historic"
    else -> null
}

/**
 * "a [type] spell that targets a [permanent]" (Repartee — Forum Necroscribe, Lecturing Scornmage):
 * the spell filter is an `And` of a base spell-type filter + a `TargetsAPermanent(<perm filter>)`
 * clause. Returns `(baseCategory, targetsMatchingFilterExpr)` when BOTH halves render exactly, else
 * null so the trigger declines -> SCAFFOLD rather than dropping the "targets a creature" clause.
 */
private fun EmitCtx.spellCastTargetsMatching(spells: JsonObject?): Pair<String, String>? {
    if (spells?.strField("_Spells") != "And") return null
    val parts = spells["args"].asArr.orEmpty().filterIsInstance<JsonObject>()
    if (parts.size != 2) return null
    val targetsNode = parts.firstOrNull { it.strField("_Spells") == "TargetsAPermanent" } ?: return null
    val baseNode = parts.firstOrNull { it !== targetsNode } ?: return null
    val category = spellCastCategory(baseNode) ?: return null
    val sub = gameObjectFilterDsl(targetsNode["args"]) ?: return null
    return category to sub
}

/** (scope, category) -> the exact `Triggers.*` constant/factory. You has named constants; the
 *  any-player / opponent scopes use the `anyPlayerCasts` / `opponentCasts` factories with a
 *  [GameObjectFilter]. When [targetsMatching] is set ("... that targets a creature"), the base
 *  filter is narrowed with `.targetsMatching(<filter>)` and the factory form is always used (the
 *  bare `Triggers.YouCastInstantOrSorcery`-style constants carry no spell filter). */
private fun castTriggerDsl(scope: CastScope, category: String, targetsMatching: String? = null): String? {
    val baseFilter = categoryFilter(category)
    if (targetsMatching != null) {
        // No bare "any spell that targets …" shape appears in the corpus; require a typed base filter.
        val composed = (baseFilter ?: return null) + ".targetsMatching($targetsMatching)"
        return when (scope) {
            CastScope.YOU -> "Triggers.youCastSpell(spellFilter = $composed)"
            CastScope.ANY -> "Triggers.anyPlayerCasts($composed)"
            CastScope.OPPONENT -> "Triggers.opponentCasts($composed)"
        }
    }
    val filter = baseFilter
    return when (scope) {
        CastScope.YOU -> when (category) {
            "any" -> "Triggers.YouCastSpell"
            "creature" -> "Triggers.YouCastCreature"
            "noncreature" -> "Triggers.YouCastNoncreature"
            "enchantment" -> "Triggers.YouCastEnchantment"
            "instantOrSorcery" -> "Triggers.YouCastInstantOrSorcery"
            "historic" -> "Triggers.YouCastHistoric"
            else -> null
        }
        CastScope.ANY -> if (filter == null) "Triggers.AnyPlayerCastsSpell" else "Triggers.anyPlayerCasts($filter)"
        CastScope.OPPONENT -> if (filter == null) "Triggers.OpponentCastsSpell" else "Triggers.opponentCasts($filter)"
    }
}

/**
 * "Ward—<cost>" (CR 702.21) -> `keywordAbility(KeywordAbility.ward(...) / wardDiscard() / wardLife(N)
 * / wardSacrifice(filter))`. The rule's `args` is the ward cost; only the cost shapes the SDK exposes
 * render exactly — mana (`Ward {N}`), discard-a-card, pay-N-life, and sacrifice-a-<filter>. Any other
 * cost (compound `And`, dynamic life, sacrifice-N) declines -> SCAFFOLD rather than approximating the
 * ward cost. Forum Necroscribe ("Ward—Discard a card") + the broad Ward {N} / Ward—Pay N life corpus.
 */
internal fun EmitCtx.wardKeywordLine(rule: JsonObject): List<Stmt>? {
    val cost = rule["args"] as? JsonObject ?: return null
    val ability: Dsl = when (cost.strField("_Cost")) {
        // Pure-mana Ward keeps the existing `KeywordAbility.Ward(WardCost.Mana("{x}"))` rendering so the
        // large mana-Ward corpus golden stays byte-identical.
        "PayMana" -> {
            val mana = renderMana(cost.field("args"))
            if (mana.isEmpty()) return null
            call("KeywordAbility.Ward", arg(call("WardCost.Mana", arg("\"$mana\""))))
        }
        "DiscardACard" -> call("KeywordAbility.wardDiscard")
        "DiscardACardAtRandom" -> call("KeywordAbility.wardDiscard", arg("random", "true"))
        "PayLife" -> {
            // Only a fixed integer life cost renders; dynamic life (PowerOfPermanent, X) declines.
            val n = (cost["args"].asInt()) ?: ((cost["args"] as? JsonObject)?.get("args").asInt()) ?: return null
            call("KeywordAbility.wardLife", arg("$n"))
        }
        "SacrificeAPermanent" -> {
            val filter = gameObjectFilterDsl(cost.field("args")) ?: return null
            call("KeywordAbility.wardSacrifice", arg(Lit(filter)))
        }
        else -> return null  // compound / dynamic ward costs -> SCAFFOLD
    }
    return listOf(Eval(call("keywordAbility", arg(ability))))
}

/** True when a trigger's subject IS this permanent — ThisPermanent present, but NOT merely as the
 *  reference inside an `Other(ThisPermanent)` "another permanent" exclusion clause. */
private fun isSelf(trig: JsonObject): Boolean {
    // A self-trigger's subject is a *direct* SinglePermanent(ThisPermanent) reference. A `_Permanents`
    // filter (And / HasAbility / …) that merely mentions ThisPermanent inside a sub-predicate — e.g.
    // Trophy Hunter's WasDealtDamageByPermanentThisTurn(ThisPermanent), "a creature with flying dealt
    // damage by THIS dies" — is NOT a self-trigger, so scope the check to the trigger's own subject
    // rather than a deep search that any nested ThisPermanent reference would satisfy.
    // Some triggers carry their subject as the FIRST of several args (the combat-damage-to-a-player
    // trigger is `[SinglePermanent(ThisPermanent), AnyPlayer]`) — the subject is then args[0], not the
    // whole args node. A plain SinglePermanent object stays the subject directly.
    val subject = (trig["args"] as? JsonArray)?.firstOrNull() ?: trig["args"]
    return subject.strField("_Permanents") == "SinglePermanent" &&
        subject.field("args").strField("_Permanent") == "ThisPermanent"
}

/** True when a trigger's permanent filter is a plain "creature" with no subtype / controller / count
 *  restriction — the only attacks shape we can render as a filterless ANY-binding trigger. */
private fun isPlainCreatureFilter(trig: JsonObject): Boolean {
    if ("Creature" !in trig.argWordsTagged("IsCardtype")) return false
    val blob = compact(trig)
    return "IsCreatureType" !in blob && "ControlledByAPlayer" !in blob &&
        "\"Other\"" !in blob && "_Comparison" !in blob && "_Color" !in blob
}

/**
 * A characteristic-defining `CDA_Power` rule (with its matching `CDA_Toughness`) -> a single
 * `dynamicStats(...)` line, when both power and toughness are the same dynamic count (the
 * power-and-toughness-equal-to-the-number-of-X cycle). Differing power/toughness amounts scaffold.
 */
internal fun EmitCtx.cdaStatsBlock(card: JsonObject, rule: JsonObject): List<Stmt>? {
    val toughnessRule = (card["Rules"].asArr ?: JsonArray(emptyList()))
        .filterIsInstance<JsonObject>().firstOrNull { it.strField("_Rule") == "CDA_Toughness" }
    if (toughnessRule == null || compact(rule["args"]) != compact(toughnessRule["args"])) {
        reasons.add("CDA_Power"); return null
    }
    val amount = dynamicAmountExpr(rule["args"]) ?: run { reasons.add("CDA_Power"); return null }
    return listOf(Eval(call("dynamicStats", arg(amount))))
}

/**
 * An `AsPermanentEnters` rule -> `replacementEffect(...)` line(s). The rule's second arg is a list of
 * `_ReplacementActionWouldEnter` nodes (enters tapped, choose a creature type as it enters, ...).
 * Any replacement we can't render exactly downgrades the card to SCAFFOLD rather than guess.
 */
internal fun EmitCtx.asEntersBlock(rule: JsonObject, condition: String? = null): List<Stmt>? {
    val replacements = (rule["args"].asArr?.getOrNull(1) as? JsonArray)?.filterIsInstance<JsonObject>()
    if (replacements.isNullOrEmpty()) { reasons.add("AsPermanentEnters"); return null }
    // The rule's first arg is the permanent the replacement scopes to. We only render the self case
    // ("~ enters …"), where the counter/tap applies to THIS permanent (`selfOnly`); a group scope
    // ("creatures you control enter …") would need a different rendering, so it scaffolds.
    val onSelf = (rule["args"].asArr?.getOrNull(0) as? JsonObject)?.strField("_Permanent") == "ThisPermanent"
    val stmts = mutableListOf<Stmt>()
    for (rep in replacements) {
        val dsl: Dsl = when (rep.strField("_ReplacementActionWouldEnter")) {
            "EntersTapped" -> call("EntersTapped")
            "ChooseACreatureType" -> call("EntersWithChoice", arg("ChoiceType.CREATURE_TYPE"))
            "EntersWithACounter" -> {
                // "~ enters with a [counter] on it" — a single fixed counter on this permanent. The
                // self-scoped ±1/±1 counter renders as the default-filter EntersWithCounters; a keyword
                // counter (e.g. a lifelink counter, Dust Animus) renders with an explicit Named filter.
                // A group scope is unsupported and any unmappable counter kind scaffolds.
                if (!onSelf) { reasons.add("AsPermanentEnters"); return null }
                val counter = rep["args"] as? JsonObject
                val kind = counter?.strField("_CounterType")
                val ewArgs = mutableListOf<Arg>()
                when {
                    // ±1/±1 counter -> default-filter EntersWithCounters (the PlusOnePlusOne default).
                    kind == "PTCounter" -> {
                        val pt = counter["args"].asArr
                        if (pt?.getOrNull(0).asInt() != 1 || pt?.getOrNull(1).asInt() != 1) { reasons.add("AsPermanentEnters"); return null }
                    }
                    // A keyword counter (e.g. a lifelink counter, Dust Animus). The IR names it
                    // "<Keyword>Counter"; map to a Named CounterTypeFilter via the Counters string constant.
                    // Restricted to the keyword counters the engine grants as a keyword
                    // (StateProjector.KEYWORD_COUNTER_MAP) — these have a known `Counters` constant. Any
                    // other "*Counter" kind (a ShieldCounter, a homebrew marker) has no validated constant,
                    // so it declines -> SCAFFOLD rather than emit a non-compiling `Counters.X`.
                    kind in KEYWORD_COUNTER_CONSTANT -> {
                        ewArgs.add(arg("counterType", "CounterTypeFilter.Named(Counters.${KEYWORD_COUNTER_CONSTANT[kind]})"))
                    }
                    else -> { reasons.add("AsPermanentEnters"); return null }
                }
                ewArgs.add(arg("count", "1"))
                ewArgs.add(arg("selfOnly", "true"))
                if (condition != null) ewArgs.add(arg("condition", condition))
                Call("EntersWithCounters", ewArgs)
            }
            "EntersWithNumberCounters" -> {
                // "enters with N +1/+1 counters". A FIXED count (Integer) renders the static
                // EntersWithCounters(count = N); a dynamic count (Stag Beetle: number of other creatures —
                // as it enters, self isn't on the battlefield, so the plain count IS "other") renders
                // EntersWithDynamicCounters. Only the ±1/±1 counter with a recoverable amount renders.
                val a = rep["args"].asArr ?: run { reasons.add("AsPermanentEnters"); return null }
                val counter = a.getOrNull(1) as? JsonObject
                val pt = counter?.get("args").asArr
                if (counter?.strField("_CounterType") != "PTCounter" || pt?.getOrNull(0).asInt() != 1 || pt?.getOrNull(1).asInt() != 1) {
                    reasons.add("AsPermanentEnters"); return null
                }
                val countNode = a.getOrNull(0) as? JsonObject
                if (countNode?.strField("_GameNumber") == "Integer") {
                    val n = countNode["args"].asInt() ?: run { reasons.add("AsPermanentEnters"); return null }
                    val ewArgs = mutableListOf(arg("count", "$n"), arg("selfOnly", "true"))
                    if (condition != null) ewArgs.add(arg("condition", condition))
                    Call("EntersWithCounters", ewArgs)
                } else {
                    if (condition != null) { reasons.add("AsPermanentEnters"); return null } // no condition param on dynamic form
                    val amt = dynamicAmountExpr(a.getOrNull(0)) ?: run { reasons.add("AsPermanentEnters"); return null }
                    call("EntersWithDynamicCounters", arg("count", amt))
                }
            }
            else -> { reasons.add("AsPermanentEnters"); return null }
        }
        stmts.add(Eval(call("replacementEffect", arg(dsl))))
    }
    return stmts
}

/**
 * A `FromAnyZone { TriggerA { <trigger>(this) ... } }` rule -> a triggered ability. The two
 * self-on-this-card shapes recognised:
 *   - `WhenAPlayerCyclesACard(You, ThisCardInHand)` -> `Triggers.YouCycleThis` ("When you cycle this
 *     card, [bonus]").
 *   - `WhenACardBecomesPlotted(ThisCardInHand)` -> `Triggers.BecomesPlotted` ("When this card becomes
 *     plotted, [bonus]", OTJ Plot / CR 718 — Aloe Alchemist).
 * A lone `you may` bonus becomes `optional = true`, mirroring [triggerBlock].
 */
internal fun EmitCtx.fromAnyZoneBlock(rule: JsonObject): List<Stmt>? {
    val inner = rule["args"] as? JsonObject
    if (inner?.strField("_Rule") != "TriggerA" ||
        !jsonContains(inner, "_CardInHand", "ThisCardInHand")) { reasons.add("FromAnyZone"); return null }
    val triggerSpec = when {
        jsonContains(inner, "_Trigger", "WhenAPlayerCyclesACard") -> "Triggers.YouCycleThis"
        jsonContains(inner, "_Trigger", "WhenACardBecomesPlotted") -> "Triggers.BecomesPlotted"
        else -> { reasons.add("FromAnyZone"); return null }
    }
    val (targets, actions) = extractEnvelope(inner)
    if (actions == null) { reasons.add("FromAnyZone"); return null }
    val (tnode, tvar) = spellTargetExpr(targets, actions) ?: return null
    val mayWrapped = actions.singleOrNull()?.strField("_Action") == "MayAction"
    val effectActions = if (mayWrapped) listOf(innerAction(actions.single()) ?: return null) else actions
    val edsl = renderEffectList(effectActions, tvar) ?: return null
    val stmts = mutableListOf<Stmt>(Assign("trigger", Lit(triggerSpec)))
    if (mayWrapped) stmts.add(Assign("optional", Lit("true")))
    if (tvar != null) stmts.add(targetLocal(tnode!!))
    stmts.add(Assign("effect", edsl))
    return listOf(Sub(Block("triggeredAbility", stmts)))
}

/**
 * Preparation card (Secrets of Strixhaven). The IR shapes a preparation card as `_OracleCard:
 * "Preparer"` with a single top-level `AsPermanentEnters[EntersPrepared]` rule and a sibling
 * `Prepared` object holding the prepare spell (its own Name / Typeline / ManaCost / SpellActions).
 * Render the `prepare("<spell name>") { manaCost; typeLine; spell { … } }` block by running the
 * generic spell-action renderer ([spellBlock]) over the nested prepare card. Returns null ->
 * SCAFFOLD when the nested prepare spell's effect can't be rendered exactly (so we never emit a
 * preparation creature whose prepare spell silently differs from oracle).
 */
internal fun EmitCtx.prepareBlock(card: JsonObject): List<Stmt>? {
    val prepared = card["Prepared"] as? JsonObject ?: run { reasons.add("Prepared"); return null }
    val faceName = prepared["Name"].asStr() ?: run { reasons.add("Prepared"); return null }
    val spellStmts = spellBlock(prepared) ?: run { reasons.add("Prepared"); return null }
    val faceBody = mutableListOf<Stmt>(
        RawLine("        manaCost = \"${renderMana(prepared["ManaCost"])}\""),
        RawLine("        typeLine = \"${renderTypeline(prepared["Typeline"])}\""),
    )
    faceBody.addAll(spellStmts)
    return listOf(Sub(Block("prepare(\"${ktStr(faceName)}\")", faceBody)))
}

/**
 * A `FromGraveyard { Activated { … } }` rule -> the inner activated ability with
 * `activateFromZone = Zone.GRAVEYARD` ("{cost}, … : Return this card from your graveyard to your hand",
 * Gangrenous Goliath). Only a plain Activated inner renders; anything else scaffolds.
 */
internal fun EmitCtx.fromGraveyardBlock(rule: JsonObject): List<Stmt>? {
    val inner = rule["args"] as? JsonObject
    return when (inner?.strField("_Rule")) {
        "Activated", "ActivatedWithModifiers" -> activatedBlock(inner, activateFromZone = "Zone.GRAVEYARD")
        else -> { reasons.add("FromGraveyard"); return null }
    }
}

/**
 * `FromHand[Activated]` -> an `activatedAbility { … }` whose `activateFromZone = Zone.HAND` ("{cost},
 * Discard this card: …", Spinewoods Armadillo's land-fetch). The same shape as [fromGraveyardBlock] but
 * scoped to the hand zone; the self-discard cost renders via [abilityCostDsl]'s `DiscardCard`/
 * `Costs.DiscardSelf` case. Only a plain Activated inner renders; anything else scaffolds.
 */
internal fun EmitCtx.fromHandBlock(rule: JsonObject): List<Stmt>? {
    val inner = rule["args"] as? JsonObject
    return when (inner?.strField("_Rule")) {
        "Activated", "ActivatedWithModifiers" -> activatedBlock(inner, activateFromZone = "Zone.HAND")
        else -> { reasons.add("FromHand"); return null }
    }
}

/** An Activated / ActivatedWithModifiers rule -> activatedAbility { cost; [target]; effect }. */
internal fun EmitCtx.activatedBlock(rule: JsonObject, activateFromZone: String? = null): List<Stmt>? {
    val args = rule["args"].asArr
    val costNode = args?.firstOrNull() as? JsonObject
    // Recover the exact activation cost. Anything we can't render exactly -> SCAFFOLD (never guess Tap).
    val cost = costNode?.let { abilityCostDsl(it) }
    if (cost == null) { reasons.add("activated-cost"); return null }
    val (targets, actions) = extractEnvelope(rule)
    if (actions == null) { reasons.add("activated-actions"); return null }

    // "Add {U} or {R}." (Caldera Lake, Pine Barrens) is a single mtgish ability whose AddMana produces a
    // *choice* of color. Argentum (and Adarkar Wastes / Brushland) model this as one activatedAbility per
    // color, each carrying the same trailing actions (e.g. "This land deals 1 damage to you"). Expand the
    // Or into N abilities, each with that color's single-produce AddMana spliced in for the Or node. Only a
    // targetless mana ability whose Or is a flat list of single-color produces expands; anything else falls
    // through to the single-ability path (and scaffolds there if the Or can't render).
    manaChoiceExpansion(rule, cost, targets, actions, activateFromZone)?.let { return it }

    return activatedAbilityStmts(rule, cost, targets, actions, activateFromZone, hasWaterbend = containsWaterbendCost(costNode))
}

/** True when the activation cost is (or composes) a Waterbend cost (Avatar: The Last Airbender). */
private fun containsWaterbendCost(node: JsonObject?): Boolean {
    if (node == null) return false
    return when (node.strField("_Cost")) {
        "Waterbend" -> true
        "And" -> (node["args"].asArr ?: return false).any { containsWaterbendCost(it as? JsonObject) }
        else -> false
    }
}

/** Build one `activatedAbility { … }` block from already-recovered cost / target / action pieces. */
private fun EmitCtx.activatedAbilityStmts(
    rule: JsonObject,
    cost: String,
    targets: List<JsonObject>?,
    actions: List<JsonObject>,
    activateFromZone: String?,
    hasWaterbend: Boolean = false,
): List<Stmt>? {
    val (tnode, tvar) = spellTargetExpr(targets, actions) ?: return null
    val edsl = renderEffectList(actions, tvar) ?: return null
    val stmts = mutableListOf<Stmt>(Assign("cost", Lit(cost)))
    if (hasWaterbend) stmts.add(Assign("hasWaterbend", Lit("true")))
    activationRestrictionLines(rule)?.let { lines -> lines.forEach { stmts.add(RawLine(it)) } } ?: return null
    if (tvar != null) stmts.add(targetLocal(tnode!!))
    stmts.add(Assign("effect", edsl))
    // "Activate only as a sorcery." (Silver Deputy) -> sorcery-speed timing. The modifier is filtered out
    // of the restriction lines above (it's a timing rule, not an ActivationRestriction).
    if (hasSorcerySpeedModifier(rule)) stmts.add(Assign("timing", Lit("TimingRule.SorcerySpeed")))
    if (activateFromZone != null) stmts.add(Assign("activateFromZone", Lit(activateFromZone)))
    // A ReplaceNextDraw effect ("the next time you would draw … instead") prompts on the replaced draw,
    // not at activation — the activated-ability flag the Words cycle's golden carries.
    if (actions.any { it.strField("_Action") == "CreateFutureReplaceWouldDraw" }) stmts.add(Assign("promptOnDraw", Lit("true")))
    if (isManaAbility(tvar, actions)) {
        stmts.add(Assign("manaAbility", Lit("true")))
        stmts.add(Assign("timing", Lit("TimingRule.ManaAbility")))
    }
    return listOf(Sub(Block("activatedAbility", stmts)))
}

/**
 * `{T}: Add {U} or {R}. …` -> one `activatedAbility { }` per color. Returns null (fall through to the
 * single-ability path) unless the actions contain exactly one `AddMana` whose produce is an `Or` of
 * flat single-color/colorless symbols, the ability is targetless, and it carries no modifiers/zone — the
 * conservative shape the pain-/filter-land idiom uses. Each emitted ability reuses the same trailing
 * actions verbatim, so a damage rider ("This land deals 1 damage to you") is preserved per color.
 */
private fun EmitCtx.manaChoiceExpansion(
    rule: JsonObject,
    cost: String,
    targets: List<JsonObject>?,
    actions: List<JsonObject>,
    activateFromZone: String?,
): List<Stmt>? {
    if (targets != null) return null
    if (rule.strField("_Rule") == "ActivatedWithModifiers") return null
    val orAction = actions.singleOrNull { act ->
        act.strField("_Action") == "AddMana" &&
            (act["args"] as? JsonObject)?.strField("_ManaProduce") == "Or"
    } ?: return null
    val orProduce = orAction["args"] as? JsonObject ?: return null
    val choices = orProduce["args"].asArr ?: return null
    if (choices.size < 2) return null
    // Every Or child must itself be a single-color / colorless produce (no nested And/Or/choice).
    val singles = choices.map { it as? JsonObject ?: return null }
    if (singles.any { manaProduceDsl(it) == null }) return null

    val blocks = mutableListOf<Stmt>()
    for (single in singles) {
        // Rewrite the actions list: replace the Or AddMana with one whose produce is this single color.
        val rewritten = actions.map { act ->
            if (act === orAction) buildJsonObject {
                put("_Action", JsonPrimitive("AddMana"))
                put("args", single)
            } else act
        }
        val one = activatedAbilityStmts(rule, cost, null, rewritten, activateFromZone) ?: return null
        blocks.addAll(one)
    }
    return blocks
}

/**
 * mtgish activation-cost `_Cost` node -> the `Costs.*` AbilityCost DSL, or null (-> SCAFFOLD) for any
 * shape we can't render exactly. Recurses on `And` -> `Costs.Composite(...)`. Conservative by design:
 * a wrong cost is worse than a scaffold, so unknown atoms bail rather than approximate.
 */
internal fun EmitCtx.abilityCostDsl(node: JsonElement?): String? {
    val obj = node as? JsonObject ?: return null
    return when (obj.strField("_Cost")) {
        "And" -> {
            val parts = (obj["args"].asArr ?: return null).map { abilityCostDsl(it) ?: return null }
            if (parts.size < 2) return null
            "Costs.Composite(${parts.joinToString(", ")})"
        }
        "PayMana" -> renderMana(obj.field("args")).ifEmpty { null }?.let { "Costs.Mana(\"$it\")" }
        // Waterbend {N} (CR, Avatar: The Last Airbender): the cost is a plain mana cost; the
        // "may tap artifacts/creatures, each {1}" semantics are carried by `hasWaterbend = true`
        // (set in activatedBlock). Only the fixed-generic shape renders — WaterbendX/WaterbendCustomX
        // carry an X value and are declined (-> SCAFFOLD) per the no-X-guessing policy.
        "Waterbend" -> renderMana(obj.field("args")).ifEmpty { null }?.let { "Costs.Mana(\"$it\")" }
        // "{X}{G}{G}" activation cost — args are [symbol-list, the X game number]; render the symbol list
        // (ManaCostX -> "{X}") as the mana cost (Silklash Spider).
        "PayManaX" -> renderMana((obj["args"].asArr)?.getOrNull(0)).ifEmpty { null }?.let { "Costs.Mana(\"$it\")" }
        "DiscardACard" -> "Costs.DiscardCard"  // "Discard a card" (Patchwork Gnomes)
        // "Discard this card" — the self-discard cost of a from-hand activated ability (Spinewoods
        // Armadillo's land-fetch). Only the ThisCardInHand subject maps to Costs.DiscardSelf; any other
        // discarded card declines so we never render a self-discard as a generic one.
        "DiscardCard" ->
            if (jsonContains(obj.field("args"), "_CardInHand", "ThisCardInHand")) "Costs.DiscardSelf" else null
        "DiscardHand" -> "Costs.DiscardHand"  // "Discard your hand" (Slate of Ancestry)
        // "Discard a card at random" — a fixed, no-choice cost (the engine picks): Costs.DiscardAtRandom(1).
        // It carries no value selection / X / inherited choice, so it is safe to render exactly (Canyon Drake).
        "DiscardACardAtRandom" -> "Costs.DiscardAtRandom(1)"
        "TapPermanent" ->
            if (obj.field("args").strField("_Permanent") == "ThisPermanent") "Costs.Tap" else null
        "SacrificePermanent" ->
            if (obj.field("args").strField("_Permanent") == "ThisPermanent") "Costs.SacrificeSelf" else null
        // "Exile this card from your graveyard" — the self-exile cost of a from-graveyard activated
        // ability (Colossal Rattlewurm's Desert-fetch, Bonebind Orator's recursion). Only the
        // ThisGraveyardCard subject maps to Costs.ExileSelf; any other exiled card declines so we never
        // render a self-exile as a generic one.
        "ExileGraveyardCard" ->
            if (obj.field("args").strField("_GraveyardCard") == "ThisGraveyardCard") "Costs.ExileSelf" else null
        "SacrificeAPermanent" -> costFilterDsl(obj.field("args"))?.let {
            if (it == "GameObjectFilter.Any") "Costs.Sacrifice()" else "Costs.Sacrifice($it)"
        }
        "PayLife" -> (findInteger(obj.field("args")) as? Int)?.let { "Costs.PayLife($it)" }
        "TapNumberPermanents" -> {
            val a = obj["args"].asArr ?: return null
            val n = findInteger(a.getOrNull(0)) as? Int ?: return null
            // "Tap N untapped X you control" — TapPermanents implies untapped + you-control, so only the
            // creature-subtype distinguishes it; bail if there's no recognisable creature-type filter.
            val ctype = creatureTypeIn(a.getOrNull(1)) ?: return null
            "Costs.TapPermanents($n, GameObjectFilter.Creature.withSubtype(\"$ctype\"))"
        }
        else -> null
    }
}

/** A sacrifice/cost permanent filter: the creature-subtype shape the general filter DSL skips, "any
 *  permanent" -> the default Any, otherwise delegate to [gameObjectFilterDsl]. */
private fun EmitCtx.costFilterDsl(node: JsonElement?): String? {
    val obj = node as? JsonObject
    when (obj?.strField("_Permanents")) {
        "IsCreatureType" -> return obj["args"].asStr()?.let { "GameObjectFilter.Creature.withSubtype(\"$it\")" }
        "IsArtifactType" -> return obj["args"].asStr()?.let { "GameObjectFilter.Artifact.withSubtype(\"$it\")" }
        "AnyPermanent" -> return "GameObjectFilter.Permanent"
        "IsToken" -> return "GameObjectFilter.Token"  // "Sacrifice a token" (Fountainport)
    }
    val base = gameObjectFilterDsl(node) ?: return null
    // "Sacrifice a Goblin creature" = And[IsCreatureType X, IsCardtype Creature]: gameObjectFilterDsl
    // sees the Creature cardtype but skips the creature subtype, so re-apply it here.
    val ctype = creatureTypeIn(node)
    return if (ctype != null && base == "GameObjectFilter.Creature") "GameObjectFilter.Creature.withSubtype(\"$ctype\")" else base
}

/** First `IsCreatureType` subtype anywhere in a (possibly `And`-nested) cost filter. */
private fun creatureTypeIn(node: JsonElement?): String? {
    when (node) {
        is JsonObject -> {
            if (node.strField("_Permanents") == "IsCreatureType") return node["args"].asStr()
            node.values.forEach { creatureTypeIn(it)?.let { r -> return r } }
        }
        is JsonArray -> node.forEach { creatureTypeIn(it)?.let { r -> return r } }
        else -> {}
    }
    return null
}

/** True iff the activated rule carries an `ActivateOnlyAsASorcery` modifier ("Activate only as a
 *  sorcery." — rendered as `timing = TimingRule.SorcerySpeed`, not an ActivationRestriction). */
private fun activatedModifiers(rule: JsonObject): List<JsonObject> =
    (rule["args"].asArr ?: emptyList()).filterIsInstance<JsonObject>()
        .filter { it.strField("_ActivateModifier") != null }

private fun hasSorcerySpeedModifier(rule: JsonObject): Boolean =
    activatedModifiers(rule).any { it.strField("_ActivateModifier") == "ActivateOnlyAsASorcery" }

private fun EmitCtx.activationRestrictionLines(rule: JsonObject): List<String>? {
    if (rule.strField("_Rule") != "ActivatedWithModifiers") return emptyList()
    // ActivateOnlyAsASorcery is a timing rule, not an ActivationRestriction — it's emitted as
    // `timing = TimingRule.SorcerySpeed` elsewhere. Drop it here so an ability whose ONLY modifier is
    // sorcery-speed needs no `restrictions =` line (Silver Deputy); a real restriction alongside it
    // still flows through the matchers below.
    val nonTimingModifiers = activatedModifiers(rule)
        .filter { it.strField("_ActivateModifier") != "ActivateOnlyAsASorcery" }
    if (nonTimingModifiers.isEmpty()) return emptyList()
    val blob = compact(rule)
    if ("ActivateOnlyIf" in blob && "IsTheirTurn" in blob && "IsBeforeAttackersDeclared" in blob) {
        return listOf(
            "        restrictions = listOf(",
            "            ActivationRestriction.OnlyDuringYourTurn,",
            "            ActivationRestriction.BeforeStep(Step.DECLARE_ATTACKERS)",
            "        )",
        )
    }
    // "Activate only if you have no cards in hand" (Fool's Tome): an ActivateOnlyIf whose condition is
    // "you have exactly 0 cards in hand". Match the precise shape (the You player + NumCardsInHandIs ==
    // Integer 0) so any other hand-count gate or player still scaffolds.
    if ("ActivateOnlyIf" in blob && "NumCardsInHandIs" in blob &&
        jsonContains(rule, "_Player", "You") && jsonContains(rule, "_Comparison", "EqualTo") &&
        emptyHandModifier(rule)
    ) {
        return listOf("        restrictions = listOf(ActivationRestriction.OnlyIfCondition(Conditions.EmptyHand))")
    }
    // "Activate only if it's not your turn" (Ghost Town): the sole modifier is an ActivateOnlyIf whose
    // condition is IsAPlayersTurn over a player Other than you -> ActivationRestriction.OnlyIfCondition(
    // IsNotYourTurn). Match the exact shape so any other ActivateOnlyIf condition still scaffolds.
    run {
        val mods = (rule["args"].asArr ?: emptyList()).filterIsInstance<JsonObject>()
            .filter { it.strField("_ActivateModifier") != null }
        val only = mods.singleOrNull()
        if (only != null && only.strField("_ActivateModifier") == "ActivateOnlyIf") {
            val cond = only["args"] as? JsonObject
            if (cond?.strField("_Condition") == "IsAPlayersTurn" &&
                jsonContains(cond, "_Players", "Other") && jsonContains(cond, "_Player", "You")
            ) {
                return listOf("        restrictions = listOf(ActivationRestriction.OnlyIfCondition(IsNotYourTurn))")
            }
        }
    }
    // "Activate no more than N times each turn" (Pit Imp / Phyrexian Battleflies) -> MaxPerTurn(N).
    // The modifier list is the rule's args after the cost + action list; render only when EVERY
    // modifier present is this exact shape, so an unrecognised modifier still scaffolds.
    val modifiers = (rule["args"].asArr ?: emptyList()).filterIsInstance<JsonObject>()
        .filter { it.strField("_ActivateModifier") != null }
    if (modifiers.isNotEmpty() && modifiers.all { it.strField("_ActivateModifier") == "ActivateNoMoreThanNumberTimesEachTurn" }) {
        val lines = modifiers.map { mod ->
            val n = findInteger(mod.field("args")) as? Int ?: return run { reasons.add("activated-modifiers"); null }
            "ActivationRestriction.MaxPerTurn($n)"
        }
        return listOf("        restrictions = listOf(${lines.joinToString(", ")})")
    }
    reasons.add("activated-modifiers")
    return null
}

/** True when the rule's only `_ActivateModifier` is an `ActivateOnlyIf` gating on "you have exactly 0
 *  cards in hand" — and nothing else. Guards the EmptyHand restriction so a different count (e.g. "no
 *  more than N") or an extra modifier still scaffolds. */
private fun EmitCtx.emptyHandModifier(rule: JsonObject): Boolean {
    val modifiers = (rule["args"].asArr ?: emptyList()).filterIsInstance<JsonObject>()
        .filter { it.strField("_ActivateModifier") != null }
    if (modifiers.size != 1) return false
    val mod = modifiers.single()
    if (mod.strField("_ActivateModifier") != "ActivateOnlyIf") return false
    if (!jsonContains(mod, "_Players", "NumCardsInHandIs")) return false
    // The compared count must be the literal Integer 0 — not X, not another number.
    return (findInteger(mod.field("args")) as? Int) == 0
}

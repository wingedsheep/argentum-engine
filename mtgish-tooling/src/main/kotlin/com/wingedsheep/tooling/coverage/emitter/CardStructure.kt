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
import com.wingedsheep.tooling.coverage.firstArgStringTagged
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
        "ReduceCastingCostIfItTargetsAPermanent" -> targetPermanentCostReductionLines(rule) != null
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
        val targetPermReduction = targetPermanentCostReductionLines(rule)
        if (targetPermReduction != null) { lines.addAll(targetPermReduction); continue }
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
 * `CastEffect(ReduceCastingCostIfItTargetsAPermanent([CostReduceGeneric N], <permanent filter>))` -> a
 * `staticAbility { ability = ModifySpellCost(SelfCast, ReduceGenericBy(FixedIfAnyTargetMatches(N,
 * <filter>))) }` block. This Town Ain't Big Enough ("This spell costs {3} less to cast if it targets a
 * permanent you control").
 *
 * Renders only a single bare generic reduction over a recoverable permanent filter (the controller
 * "you control" clause is preserved as `.youControl()`). A colored/multi-symbol reduction, or an
 * unrenderable filter, declines -> SCAFFOLD rather than misrender the gate.
 */
private fun EmitCtx.targetPermanentCostReductionLines(rule: JsonObject): List<String>? {
    val node = rule["args"] as? JsonObject ?: return null
    if (node.strField("_CastEffect") != "ReduceCastingCostIfItTargetsAPermanent") return null
    val args = node["args"].asArr ?: return null
    val symbols = (args.getOrNull(0) as? JsonArray)?.filterIsInstance<JsonObject>() ?: return null
    if (symbols.size != 1 || symbols[0].strField("_CostReductionSymbol") != "CostReduceGeneric") return null
    val amount = symbols[0]["args"].asInt() ?: return null
    val filterNode = args.getOrNull(1) as? JsonObject ?: return null
    // gameObjectFilterDsl already preserves the "you control" / "an opponent controls" controller clause
    // (e.g. ControlledByAPlayer(You) -> GameObjectFilter.Permanent.youControl()); an unrenderable filter
    // declines -> SCAFFOLD.
    val filterDsl = gameObjectFilterDsl(filterNode) ?: return null
    val ability = call(
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
    return renderBlock(Block("staticAbility", listOf(Assign("ability", ability))), "    ")
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

/**
 * A `StackSpellsEffect(<spell filter>, [<spell effect>])` static rule -> one
 * `staticAbility { ability = ModifySpellCost(YouCast(<spell filter>), …) }`. Currently the only spell
 * effect rendered is **Affinity** — "Instant and sorcery spells you cast have affinity for creatures"
 * (Witherbloom, the Balancer). CR 702.41a: granting affinity to a class of spells is mechanically a
 * battlefield-sourced [SpellCostTarget.YouCast] generic reduction equal to the matching permanents you
 * control (the same lowering as Sami, Wildcat Captain) — no dedicated "granted affinity" engine path
 * is needed. The spell filter is rendered only for the exact Instant/Sorcery `Or` shape; any other
 * spell filter or spell effect declines -> SCAFFOLD rather than guess.
 */
internal fun EmitCtx.stackSpellsEffectBlock(rule: JsonObject): List<Stmt>? {
    val args = rule["args"].asArr ?: run { reasons.add("StackSpellsEffect"); return null }
    val spellFilterNode = args.getOrNull(0) as? JsonObject
    val spellFilterDsl = instantOrSorcerySpellFilter(spellFilterNode)
        ?: run { reasons.add("StackSpellsEffect"); return null }
    val effects = (args.getOrNull(1) as? JsonArray)?.filterIsInstance<JsonObject>()
    val affinity = effects?.singleOrNull()?.takeIf { it.strField("_SpellEffect") == "Affinity" }
        ?: run { reasons.add("StackSpellsEffect"); return null }
    val group = gameObjectFilterDsl(affinity["args"]) ?: run { reasons.add("StackSpellsEffect"); return null }
    val ability = call(
        "ModifySpellCost",
        arg("target", call("SpellCostTarget.YouCast", arg(Lit(spellFilterDsl)))),
        arg("modification", call(
            "CostModification.ReduceGenericBy",
            arg(call("CostReductionSource.PermanentsYouControlMatching", arg(Lit(group)))),
        )),
    )
    return listOf(staticAbilityStmt(ability))
}

/** The exact `Or[IsCardtype Instant, IsCardtype Sorcery]` spell filter -> `GameObjectFilter.InstantOrSorcery`;
 *  null for any other shape (so the caller declines rather than widening). */
private fun instantOrSorcerySpellFilter(spellFilter: JsonObject?): String? {
    if (spellFilter?.strField("_Spells") != "Or") return null
    val orTypes = (spellFilter["args"].asArr?.filterIsInstance<JsonObject>() ?: return null)
        .filter { it.strField("_Spells") == "IsCardtype" }
        .mapNotNull { it["args"].asStr() }
        .toSet()
    return if (orTypes == setOf("Instant", "Sorcery")) "GameObjectFilter.InstantOrSorcery" else null
}

private fun EmitCtx.additionalCostLine(rule: JsonObject): String? {
    val node = rule["args"] as? JsonObject ?: return null
    if (node.strField("_CastEffect") != "AdditionalCastingCost") return null
    val cost = node["args"] as? JsonObject ?: return null
    return when (cost.strField("_Cost")) {
        "SacrificeAPermanent" -> {
            val filter = gameObjectFilterDsl(cost["args"]) ?: return null
            "    additionalCost(Costs.additional.SacrificePermanent($filter))"
        }
        // "As an additional cost to cast this spell, return a permanent you control to its owner's
        // hand" (Fear of Isolation): `PutAPermanentIntoItsOwnersHand(ControlledByAPlayer(You))`. Only
        // the bare "any permanent you control" shape renders — the controller scope is implicit in the
        // cost (it always returns one the caster controls), so the filter is GameObjectFilter.Any. A
        // typed/restricted return (e.g. "return a creature you control") would need a real filter and
        // declines (-> SCAFFOLD) rather than silently widening to any permanent.
        "PutAPermanentIntoItsOwnersHand" -> {
            if (!isAnyPermanentYouControl(cost["args"])) return null
            "    additionalCost(Costs.additional.ReturnToHand())"
        }
        else -> null
    }
}

/** True iff [node] is exactly "any permanent you control" — `ControlledByAPlayer(SinglePlayer(You))`
 *  with no type/subtype/other restriction. Used to keep the return-a-permanent additional cost render
 *  to the bare shape (a restricted return declines rather than drop the restriction). */
private fun isAnyPermanentYouControl(node: JsonElement?): Boolean {
    val obj = node as? JsonObject ?: return false
    if (obj.strField("_Permanents") != "ControlledByAPlayer") return false
    if (!jsonContains(obj, "_Player", "You")) return false
    // No type / subtype / colour / state restriction may ride alongside the controller clause.
    val blob = compact(obj)
    val restrictions = listOf(
        "IsCardtype", "IsCreatureType", "IsLandType", "IsArtifactType", "IsEnchantmentType",
        "IsColor", "IsNonColor", "IsTapped", "IsUntapped", "PowerIs", "ToughnessIs",
        "ManaValueIs", "HasAbility", "HasACounterOfType", "Other", "IsAnOutlaw", "IsNonOutlaw",
    )
    return restrictions.none { it in blob }
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
 * "if defending player controls no [filter]" — `PlayerPassesFilter(Trigger_DefendingPlayer,
 * ControlsNo(<filter>))` -> `Conditions.CompareAmounts(DynamicAmount.AggregateBattlefield(
 * Player.DefendingPlayer, <filter>), ComparisonOperator.EQ, DynamicAmount.Fixed(0))`. "Controls no"
 * is the defending player's count of matching permanents being exactly zero. Renders only the
 * Trigger_DefendingPlayer player with a `ControlsNo` filter the strict [gameObjectFilterDsl] can
 * express exactly; anything else (another player ref, a non-ControlsNo clause, an unrenderable filter)
 * declines -> SCAFFOLD rather than drop or widen the constraint. (Fear of the Dark.)
 */
private fun EmitCtx.defendingPlayerControlsNoDsl(condNode: JsonElement?): String? {
    val cond = condNode as? JsonObject ?: return null
    if (cond.strField("_Condition") != "PlayerPassesFilter") return null
    val args = cond["args"].asArr ?: return null
    if ((args.getOrNull(0) as? JsonObject)?.strField("_Player") != "Trigger_DefendingPlayer") return null
    val controls = args.getOrNull(1) as? JsonObject ?: return null
    if (controls.strField("_Players") != "ControlsNo") return null
    val filter = gameObjectFilterDsl(controls["args"]) ?: return null
    return render(call(
        "Conditions.CompareAmounts",
        arg(call("DynamicAmount.AggregateBattlefield", arg("Player.DefendingPlayer"), arg(Lit(filter)))),
        arg("ComparisonOperator.EQ"),
        arg(call("DynamicAmount.Fixed", arg("0"))),
    ))
}

/**
 * "during your turn" condition (`IsPlayersTurn(You)`) -> the `Conditions.IsYourTurn` DSL string, or
 * null when the turn isn't scoped to You. Used by the [ifRuleBlock] conditional-lord gate (At
 * Knifepoint's "during your turn, outlaws you control have first strike"); the projected-state read
 * drops the grant the moment it stops being the controller's turn.
 */
private fun isYourTurnConditionDsl(condNode: JsonElement?): String? {
    val cond = condNode as? JsonObject ?: return null
    if (cond.strField("_Condition") != "IsPlayersTurn") return null
    if (!jsonContains(cond["args"], "_Player", "You")) return null
    return "Conditions.IsYourTurn"
}

/**
 * "as long as you gained life this turn" static gate — `PlayerPassesFilter(You, GainedLifeThisTurn)`
 * with no count/amount sub-clause -> `Conditions.YouGainedLifeThisTurn`. Backs the **Infusion** ability
 * word's static lord (Thornfist Striker). Only the bare You-scoped, no-amount shape renders; any other
 * player scope or a quantified "gained N life" clause declines -> SCAFFOLD.
 */
private fun youGainedLifeConditionDsl(condNode: JsonElement?): String? {
    val cond = condNode as? JsonObject ?: return null
    if (cond.strField("_Condition") != "PlayerPassesFilter") return null
    if (!jsonContains(cond, "_Player", "You")) return null
    val players = (cond["args"].asArr?.getOrNull(1)) as? JsonObject ?: return null
    // Must be exactly the bare GainedLifeThisTurn filter (no nested amount/count args).
    if (players.strField("_Players") != "GainedLifeThisTurn" || players.size > 1) return null
    return "Conditions.YouGainedLifeThisTurn"
}

/**
 * "if you gained N or more life this turn" — `PlayerPassesFilter(You, GainedLifeAmountThisTurn(
 * [Comparison GreaterThanOrEqualTo Integer N]))` -> `Conditions.YouGainedLifeThisTurnAtLeast(N)`.
 * The quantified sibling of [youGainedLifeConditionDsl]. Backs Scheming Silvertongue's second-main
 * intervening-if ("if you gained 2 or more life this turn"). Only the You scope with a
 * `GreaterThanOrEqualTo` comparison over a fixed integer renders; any other player scope or
 * comparator declines -> SCAFFOLD rather than miscount.
 */
private fun youGainedLifeAtLeastConditionDsl(condNode: JsonElement?): String? {
    val cond = condNode as? JsonObject ?: return null
    if (cond.strField("_Condition") != "PlayerPassesFilter") return null
    val args = cond["args"].asArr ?: return null
    if ((args.getOrNull(0) as? JsonObject)?.strField("_Player") != "You") return null
    val players = args.getOrNull(1) as? JsonObject ?: return null
    if (players.strField("_Players") != "GainedLifeAmountThisTurn") return null
    val comparison = players["args"] as? JsonObject ?: return null
    if (comparison.strField("_Comparison") != "GreaterThanOrEqualTo") return null
    val n = (comparison["args"] as? JsonObject)?.takeIf { it.strField("_GameNumber") == "Integer" }
        ?.get("args").asInt() ?: return null
    return "Conditions.YouGainedLifeThisTurnAtLeast($n)"
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
 * The "slow land" enters-tapped gate (Deathcap Glade, Dreamroot Cascade, Sundown Pass; VOW/SOS/INR):
 * an `Unless{<condition>}[EntersTapped]` replacement node whose condition is
 * `PlayerPassesFilter(You, ControlsNum(>= N, And(Other(ThisPermanent), IsCardtype Land)))` —
 * "you control N or more OTHER lands". Renders the `unlessCondition` argument for
 * `EntersTapped(...)` as `Conditions.YouControlAtLeast(N + 1, GameObjectFilter.Land)`.
 *
 * The `+ 1` accounts for the IR's `Other(ThisPermanent)` exclusion: Argentum's
 * [com.wingedsheep.sdk.scripting.values.DynamicAmount.AggregateBattlefield] over `GameObjectFilter.Land`
 * counts the entering land itself, so "N or more *other* lands" is "N + 1 or more lands total".
 *
 * Conservative by design — returns null (-> SCAFFOLD) unless the shape is exactly: the `Unless`
 * action is a single `EntersTapped`, the condition is the You/ControlsNum/`>=`/Integer gate over an
 * `And(Other(ThisPermanent), IsCardtype Land)` filter. A `<=` comparison (the parallel "fast land"
 * cycle, "two or fewer other lands") or any other filter declines rather than miscount.
 */
private fun EmitCtx.slowLandEntersTappedConditionDsl(rep: JsonObject): String? {
    val args = rep["args"].asArr ?: return null
    // Second arg is the list of replacement actions the Unless gates — must be a lone EntersTapped.
    val gated = (args.getOrNull(1) as? JsonArray)?.filterIsInstance<JsonObject>() ?: return null
    if (gated.singleOrNull()?.strField("_ReplacementActionWouldEnter") != "EntersTapped") return null

    val cond = args.getOrNull(0) as? JsonObject ?: return null
    if (cond.strField("_Condition") != "PlayerPassesFilter") return null
    val condArgs = cond["args"].asArr ?: return null
    if ((condArgs.getOrNull(0) as? JsonObject)?.strField("_Player") != "You") return null
    val controls = condArgs.getOrNull(1) as? JsonObject ?: return null
    if (controls.strField("_Players") != "ControlsNum") return null
    val cArgs = controls["args"].asArr ?: return null
    val comparison = cArgs.getOrNull(0) as? JsonObject ?: return null
    if (comparison.strField("_Comparison") != "GreaterThanOrEqualTo") return null
    val n = (comparison["args"] as? JsonObject)?.takeIf { it.strField("_GameNumber") == "Integer" }
        ?.get("args").asInt() ?: return null
    // The filter must be exactly "other lands": And(Other(ThisPermanent), IsCardtype Land).
    val filter = cArgs.getOrNull(1) as? JsonObject ?: return null
    if (filter.strField("_Permanents") != "And") return null
    val parts = filter["args"].asArr?.filterIsInstance<JsonObject>() ?: return null
    if (parts.size != 2) return null
    val other = parts.firstOrNull { it.strField("_Permanents") == "Other" } ?: return null
    if ((other["args"] as? JsonObject)?.strField("_Permanent") != "ThisPermanent") return null
    val land = parts.firstOrNull { it.strField("_Permanents") == "IsCardtype" } ?: return null
    if (land["args"].asStr() != "Land") return null
    return render(call("Conditions.YouControlAtLeast", arg("${n + 1}"), arg(Lit("GameObjectFilter.Land"))))
}

/**
 * The Duskmourn (DSK) common dual-land enters-tapped gate (Bleeding Woods, Etched Cornfield,
 * Murky Sewer, Razortrap Gorge, …): "This land enters tapped unless a player has N or less life."
 * The IR wraps the tapped replacement in an `Unless{<condition>}[EntersTapped]` whose condition is
 * `APlayerPassesFilter(AnyPlayer, LifeTotalIs(<= N))` — an existential life threshold over ALL
 * players. Renders the `unlessCondition` argument for `EntersTapped(...)` as
 * `Conditions.APlayerLifeAtMost(N)` (true when any player is at N life or below).
 *
 * Conservative by design — returns null (-> SCAFFOLD) unless the shape is exactly: the `Unless`
 * action is a single `EntersTapped`, the condition is `APlayerPassesFilter` over the `AnyPlayer`
 * subject with a `LifeTotalIs`/`LessThanOrEqualTo`/Integer threshold. Any other subject or
 * comparison (`GreaterThanOrEqualTo`, an opponent-only subject, …) declines rather than miscount.
 */
private fun EmitCtx.aPlayerLifeAtMostEntersTappedConditionDsl(rep: JsonObject): String? {
    val args = rep["args"].asArr ?: return null
    // Second arg is the list of replacement actions the Unless gates — must be a lone EntersTapped.
    val gated = (args.getOrNull(1) as? JsonArray)?.filterIsInstance<JsonObject>() ?: return null
    if (gated.singleOrNull()?.strField("_ReplacementActionWouldEnter") != "EntersTapped") return null

    val cond = args.getOrNull(0) as? JsonObject ?: return null
    if (cond.strField("_Condition") != "APlayerPassesFilter") return null
    val condArgs = cond["args"].asArr ?: return null
    // Subject must be the existential "AnyPlayer" (maps to the engine's any-player threshold).
    if ((condArgs.getOrNull(0) as? JsonObject)?.strField("_Players") != "AnyPlayer") return null
    val lifeFilter = condArgs.getOrNull(1) as? JsonObject ?: return null
    if (lifeFilter.strField("_Players") != "LifeTotalIs") return null
    val comparison = lifeFilter["args"] as? JsonObject ?: return null
    if (comparison.strField("_Comparison") != "LessThanOrEqualTo") return null
    val n = (comparison["args"] as? JsonObject)?.takeIf { it.strField("_GameNumber") == "Integer" }
        ?.get("args").asInt() ?: return null
    return render(call("Conditions.APlayerLifeAtMost", arg("$n")))
}

/**
 * A `FlashForCasters { Condition }` rule -> the card-level `conditionalFlash = <condition>`
 * assignment ("<this> has flash as long as <condition>", CR 702.8 / Colossal Rattlewurm). Only the
 * "you control a [filter]" condition the shared [youControlConditionDsl] renders exactly produces a
 * line; any other flash condition declines (null -> SCAFFOLD) so the emitter never grants flash on a
 * condition it can't reproduce.
 *
 * Note: the OTJ "...as long as you've committed a crime this turn" flash condition
 * ([youCommittedCrimeConditionDsl]) is renderable in isolation, but every OTJ card that carries it
 * (e.g. Take for a Ride) also has a spell body the mtgish IR renders lossily — Take for a Ride's
 * "It gains haste until end of turn" is absent from the IR, so a whole-card AUTO render would silently
 * drop it. Rendering the flash gate while the body stays lossy would ship a confidently-wrong card, so
 * we keep declining (-> SCAFFOLD) here rather than emit a partial card. Revisit if/when the IR carries
 * the full spell body.
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
 * Delirium (ability word) — "there are N or more card types among cards in your graveyard." mtgish
 * carries two equivalent shapes:
 *  - `PlayerPassesFilter(You, NumCardTypesInGraveyardIs(GreaterThanOrEqualTo Integer N))` — the static
 *    "as long as …" gate (Spineseeker Centipede).
 *  - `ThereAreNumberCardTypesInPlayersGraveyard(GreaterThanOrEqualTo Integer N, You)` — the
 *    activation/cost gate (Balustrade Wurm's "Activate only if …").
 * Both render to `Conditions.Delirium(N)` (the DISTINCT_TYPES aggregation over your graveyard).
 * The printed threshold is four, but N is read from the comparison so any "N or more card types"
 * variant renders. Only the You scope + GreaterThanOrEqualTo + Integer threshold renders; any other
 * player, comparison, or dynamic threshold declines -> SCAFFOLD.
 */
internal fun EmitCtx.deliriumConditionDsl(condNode: JsonElement?): String? {
    val cond = condNode as? JsonObject ?: return null
    val (comparison, player) = when (cond.strField("_Condition")) {
        // Static gate: PlayerPassesFilter(You, NumCardTypesInGraveyardIs(>= N))
        "PlayerPassesFilter" -> {
            val args = cond["args"].asArr ?: return null
            if ((args.getOrNull(0) as? JsonObject)?.strField("_Player") != "You") return null
            val num = args.getOrNull(1) as? JsonObject ?: return null
            if (num.strField("_Players") != "NumCardTypesInGraveyardIs") return null
            (num["args"] as? JsonObject) to "You"
        }
        // Activation gate: ThereAreNumberCardTypesInPlayersGraveyard(>= N, You)
        "ThereAreNumberCardTypesInPlayersGraveyard" -> {
            val args = cond["args"].asArr ?: return null
            val p = (args.getOrNull(1) as? JsonObject)?.strField("_Player") ?: return null
            (args.getOrNull(0) as? JsonObject) to p
        }
        else -> return null
    }
    if (player != "You") return null
    if (comparison?.strField("_Comparison") != "GreaterThanOrEqualTo") return null
    val n = (comparison["args"] as? JsonObject)?.takeIf { it.strField("_GameNumber") == "Integer" }
        ?.get("args").asInt() ?: return null
    return "Conditions.Delirium($n)"
}

/**
 * "this permanent has N or more +1/+1 counters on it"
 * (`PermanentPassesFilter(ThisPermanent, HasNumCountersOfType(GreaterThanOrEqualTo Integer N,
 * PTCounter(1,1)))`, Vadmir, New Blood) -> the `Conditions.SourceCounterCountAtLeast(
 * Counters.PLUS_ONE_PLUS_ONE, N)` DSL string, or null when the subject isn't ThisPermanent, the
 * comparison isn't `>= N`, or the counter isn't the bare ±1/±1 counter. Used by the [ifRuleBlock]
 * static gates (Vadmir's "menace and lifelink at 4+ counters"). The predicate reads the source's
 * counter map under projection, so the gated keywords appear/vanish as counters cross the threshold.
 *
 * Renders for any counter kind [counterTypeDsl] can name — the bare `PTCounter(1,1)` (+1/+1) and the
 * named/keyword/storage counters (e.g. a GROWTH counter, Comforting Counsel's "five or more growth
 * counters"). A counter type we can't name declines (-> SCAFFOLD) rather than guessing the plumbing.
 */
private fun EmitCtx.sourceCounterCountAtLeastConditionDsl(condNode: JsonElement?): String? {
    val cond = condNode as? JsonObject ?: return null
    if (cond.strField("_Condition") != "PermanentPassesFilter") return null
    val args = cond["args"].asArr ?: return null
    if ((args.getOrNull(0) as? JsonObject)?.strField("_Permanent") != "ThisPermanent") return null
    val has = args.getOrNull(1) as? JsonObject ?: return null
    if (has.strField("_Permanents") != "HasNumCountersOfType") return null
    val hArgs = has["args"].asArr ?: return null
    val comparison = hArgs.getOrNull(0) as? JsonObject ?: return null
    if (comparison.strField("_Comparison") != "GreaterThanOrEqualTo") return null
    val n = (comparison["args"] as? JsonObject)?.takeIf { it.strField("_GameNumber") == "Integer" }
        ?.get("args").asInt() ?: return null
    val counter = counterTypeDsl(hArgs.getOrNull(1)) ?: return null
    return "Conditions.SourceCounterCountAtLeast($counter, $n)"
}

/**
 * **Delirium** static gate — "as long as there are N or more card types among cards in your graveyard"
 * (`PlayerPassesFilter(You, NumCardTypesInGraveyardIs(GreaterThanOrEqualTo Integer N))`, Wildfire
 * Wickerfolk) -> the `Compare(AggregateZone(You, GRAVEYARD, Any, DISTINCT_TYPES), GTE, Fixed(N))` DSL
 * string. Delirium is an ability word with no rules meaning, so the gate is just the graveyard
 * distinct-card-type count threshold the engine reads under projection. Renders ONLY the You-scoped,
 * `>= N` (fixed integer) shape; any other player scope or comparator declines (-> SCAFFOLD) rather than
 * miscount the threshold.
 */
private fun deliriumConditionDsl(condNode: JsonElement?): String? {
    val cond = condNode as? JsonObject ?: return null
    if (cond.strField("_Condition") != "PlayerPassesFilter") return null
    val args = cond["args"].asArr ?: return null
    if ((args.getOrNull(0) as? JsonObject)?.strField("_Player") != "You") return null
    val pred = args.getOrNull(1) as? JsonObject ?: return null
    if (pred.strField("_Players") != "NumCardTypesInGraveyardIs") return null
    val comparison = pred["args"] as? JsonObject ?: return null
    if (comparison.strField("_Comparison") != "GreaterThanOrEqualTo") return null
    val n = (comparison["args"] as? JsonObject)?.takeIf { it.strField("_GameNumber") == "Integer" }
        ?.get("args").asInt() ?: return null
    return "Compare(DynamicAmount.AggregateZone(Player.You, Zone.GRAVEYARD, GameObjectFilter.Any, " +
        "Aggregation.DISTINCT_TYPES), ComparisonOperator.GTE, DynamicAmount.Fixed($n))"
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
    // Slickshot Vault-Buster: "This creature gets +2/+0 as long as you've committed a crime this
    //   turn." — same SELF gated-static shape, but the layer effect is a fixed stat buff:
    //   If(PlayerPassesFilter(You, CommitedACrimeThisTurn))
    //     [ PermanentLayerEffect(ThisPermanent, [ AdjustPT(p, t) ]) ]
    // -> ConditionalStaticAbility(ModifyStats(p, t, Filters.Self), Conditions.YouCommittedCrimeThisTurn).
    // Vadmir, New Blood: "As long as Vadmir has four or more +1/+1 counters on it, it has menace and
    //   lifelink." — same SELF gated-static shape, but the gate is a counter-count threshold and the
    //   layer effect carries *two* keyword grants:
    //   If(PermanentPassesFilter(ThisPermanent, HasNumCountersOfType(>= 4, PTCounter(1,1))))
    //     [ PermanentLayerEffect(ThisPermanent, [ AddAbility(Menace), AddAbility(Lifelink) ]) ]
    // -> one ConditionalStaticAbility(GrantKeyword(kw, Filters.Self), SourceCounterCountAtLeast(+1/+1, 4))
    //    per granted keyword.
    // Only the SELF "grant keyword(s) to this permanent" or "fixed +p/+t to this permanent" inner shape
    // renders; any other layer effect (dynamic P/T, group filter, non-keyword AddAbility) declines
    // -> SCAFFOLD.
    if (innerRule.strField("_Rule") == "PermanentLayerEffect" &&
        jsonContains(innerRule, "_Permanent", "ThisPermanent")
    ) {
        val condDsl = youHaventCastASpellConditionDsl(cond)
            ?: youCommittedCrimeConditionDsl(cond)
            ?: sourceCounterCountAtLeastConditionDsl(cond)
            ?: deliriumConditionDsl(cond)
            // "as long as you gained life this turn" gating a self-only buff (Ulna Alley Shopkeep's
            // Infusion +2/+0). Same condition the EachPermanentLayerEffect lord branch already uses.
            ?: youGainedLifeConditionDsl(cond)
            // "during your turn, this creature has <keyword>" (Razorkin Needlehead's first strike) —
            // the same SELF gated-static shape with an IsPlayersTurn(You) gate. The projected-state read
            // drops the grant the moment it stops being the controller's turn.
            ?: isYourTurnConditionDsl(cond)
            ?: run { reasons.add("If"); return null }
        val layerEffects = (innerRule["args"].asArr?.getOrNull(1) as? JsonArray)?.filterIsInstance<JsonObject>()
            ?: run { reasons.add("If"); return null }
        if (layerEffects.isEmpty()) { reasons.add("If"); return null }
        // Each layer effect grants ONE continuous effect to SELF, gated on the same condition — one
        // `ConditionalStaticAbility` per layer effect, in order. Two shapes render: a bare `AddAbility`
        // (grant a plain keyword) and a fixed `AdjustPT` (a +p/+t buff). A card can mix them — e.g.
        // Wildfire Wickerfolk's Delirium grants +1/+1 AND trample, and Spineseeker Centipede's grants
        // +1/+2 AND vigilance. One row per layer effect (Vadmir's two keywords too). Anything richer
        // (a dynamic AdjustPTX / AdjustPTForEach, an AddAbility wrapping an activated ability / landwalk /
        // protection) declines (-> SCAFFOLD) rather than dropping the variable count or the exception.
        val abilities = layerEffects.map { le ->
            when (le.strField("_StaticLayerEffect")) {
                "AddAbility" -> {
                    val granted = (le["args"] as? JsonArray)?.singleOrNull() as? JsonObject
                    if (granted?.strField("_Rule") != null && granted.size > 1) { reasons.add("If"); return null }
                    val kw = keywordOf(le) ?: run { reasons.add("If"); return null }
                    call("GrantKeyword", arg("Keyword.$kw"), arg("Filters.Self"))
                }
                "AdjustPT" -> {
                    val pt = le["args"].asArr
                    val p = pt?.getOrNull(0).asInt()
                    val t = pt?.getOrNull(1).asInt()
                    if (pt == null || pt.size != 2 || p == null || t == null) { reasons.add("If"); return null }
                    call("ModifyStats", arg("$p"), arg("$t"), arg("Filters.Self"))
                }
                else -> { reasons.add("If"); return null }
            }
        }
        return abilities.map { ability ->
            staticAbilityStmt(call(
                "ConditionalStaticAbility",
                arg("ability", ability),
                arg("condition", Lit(condDsl)),
            ))
        }
    }

    // At Knifepoint: "During your turn, outlaws you control have first strike."
    //   If(IsPlayersTurn(You)) [ EachPermanentLayerEffect(<group>, [AddAbility(<keyword>) | AdjustPT]) ]
    // -> one `staticAbility { condition = Conditions.IsYourTurn; ability = <lord ability> }` per layer
    // effect, reusing the always-on lord renderer (staticLordBlock) but gated on the controller's turn.
    // Thornfist Striker (Infusion): "Creatures you control get +1/+0 and have trample as long as you
    // gained life this turn."
    //   If(PlayerPassesFilter(You, GainedLifeThisTurn))
    //     [ EachPermanentLayerEffect(creatures you control, [AdjustPT(1,0), AddAbility(Trample)]) ]
    // -> the same gated lord, one row per layer effect, gated on Conditions.YouGainedLifeThisTurn.
    // Comforting Counsel: "As long as there are five or more growth counters on this enchantment,
    //   creatures you control get +3/+3."
    //   If(PermanentPassesFilter(ThisPermanent, HasNumCountersOfType(>= 5, GrowthCounter)))
    //     [ EachPermanentLayerEffect(creatures you control, [AdjustPT(3,3)]) ]
    // -> the same gated lord, gated on Conditions.SourceCounterCountAtLeast(Counters.GROWTH, 5).
    // Only the "during YOUR turn", "you gained life this turn", or "N+ counters on this permanent"
    // gates render; any other condition declines (-> SCAFFOLD). The lord renderer itself still
    // declines any group/ability it can't reproduce exactly.
    if (innerRule.strField("_Rule") == "EachPermanentLayerEffect") {
        val condDsl = isYourTurnConditionDsl(cond)
            ?: youGainedLifeConditionDsl(cond)
            ?: sourceCounterCountAtLeastConditionDsl(cond)
            ?: run { reasons.add("If"); return null }
        return staticLordBlock(innerRule, condition = condDsl)
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

    // Grand Abolisher: "During your turn, your opponents can't cast spells or activate abilities of
    // artifacts, creatures, or enchantments."
    //   If(IsPlayersTurn(You))
    //     [ EachPlayerEffect(Opponent, [ CantCastSpells(AnySpell),
    //                                    CantActivateAbilities(AbilityOfAPermanent(Or[<types>])) ]) ]
    // -> one `staticAbility { ability = PlayersCant{Cast,Activate}…(affected = Player.EachOpponent,
    //    …, condition = Conditions.IsYourTurn) }` per recognised opponent-scoped player effect, the
    // your-turn gate carried *inside* each static (not as a separate `condition =` row). Only the
    // EachOpponent scope + your-turn gate + the two recognised "can't cast" / "can't activate" effects
    // render; any other scope, condition, or effect declines -> SCAFFOLD rather than guess.
    if (innerRule.strField("_Rule") == "EachPlayerEffect") {
        return opponentLockBlock(innerRule, cond) ?: run { reasons.add("If"); return null }
    }

    reasons.add("If"); return null
}

/**
 * `EachPlayerEffect(Opponent, [<player effects>])` gated on "during your turn" -> one
 * `staticAbility { }` per recognised opponent-scoped lock, each carrying `affected =
 * Player.EachOpponent` and `condition = Conditions.IsYourTurn`. Backs Grand Abolisher / Voice of
 * Victory-style "your opponents can't … during your turn" cards. Returns null (-> SCAFFOLD) unless the
 * scope is exactly `Opponent`, the gate is `IsPlayersTurn(You)`, and *every* nested player effect is one
 * we render exactly — partial recognition would silently drop a clause, so it's all-or-nothing.
 */
private fun EmitCtx.opponentLockBlock(rule: JsonObject, cond: JsonObject): List<Stmt>? {
    if (isYourTurnConditionDsl(cond) == null) return null
    val args = rule["args"].asArr ?: return null
    val player = args.getOrNull(0) as? JsonObject ?: return null
    if (!jsonContains(player, "_Players", "Opponent")) return null
    val effects = (args.getOrNull(1) as? JsonArray)?.filterIsInstance<JsonObject>()
    if (effects.isNullOrEmpty()) return null

    val gate = arg("condition", Lit("Conditions.IsYourTurn"))
    val stmts = effects.map { e ->
        val ability = when (e.strField("_PlayerEffect")) {
            // "your opponents can't cast spells during your turn" — only the unfiltered "any spell"
            // shape renders; a filtered cast lock declines rather than approximate the filter.
            "CantCastSpells" -> {
                val spells = e["args"] as? JsonObject
                if (spells == null || !jsonContains(spells, "_Spells", "AnySpell")) return null
                call("PlayersCantCastSpells", arg("affected", Lit("Player.EachOpponent")), gate)
            }
            // "your opponents can't activate abilities of <permanent filter> during your turn" —
            // AbilityOfAPermanent over an `Or` of `IsCardtype` predicates -> a `GameObjectFilter`
            // type-union. Any non-cardtype / non-Or permanent filter declines.
            "CantActivateAbilities" -> {
                val filterExpr = cantActivatePermanentFilterExpr(e["args"] as? JsonObject) ?: return null
                call(
                    "PlayersCantActivateAbilities",
                    arg("affected", Lit("Player.EachOpponent")),
                    arg("permanentFilter", Lit(filterExpr)),
                    gate,
                )
            }
            else -> return null
        }
        staticAbilityStmt(ability)
    }
    return stmts
}

/**
 * The `GameObjectFilter` expr for a `CantActivateAbilities(AbilityOfAPermanent(<perm filter>))` effect.
 * Renders only `AbilityOfAPermanent` over an `Or` of `IsCardtype` predicates -> `GameObjectFilter.X or
 * GameObjectFilter.Y or …` (Grand Abolisher: artifacts, creatures, or enchantments), or a single bare
 * `IsCardtype`. Any richer permanent filter declines (-> SCAFFOLD) rather than emit an inexact lock.
 */
private fun cantActivatePermanentFilterExpr(abilitySet: JsonObject?): String? {
    if (abilitySet == null || abilitySet.strField("_ActivatedAbilities") != "AbilityOfAPermanent") return null
    val perm = abilitySet["args"] as? JsonObject ?: return null
    fun typeToFilter(type: String?): String? = when (type) {
        "Artifact" -> "GameObjectFilter.Artifact"
        "Creature" -> "GameObjectFilter.Creature"
        "Enchantment" -> "GameObjectFilter.Enchantment"
        "Land" -> "GameObjectFilter.Land"
        "Planeswalker" -> "GameObjectFilter.Planeswalker"
        else -> null
    }
    return when (perm.strField("_Permanents")) {
        "IsCardtype" -> typeToFilter(perm["args"].asStr())
        "Or" -> {
            val arms = (perm["args"] as? JsonArray)?.filterIsInstance<JsonObject>() ?: return null
            if (arms.isEmpty() || arms.any { it.strField("_Permanents") != "IsCardtype" }) return null
            val parts = arms.map { typeToFilter(it["args"].asStr()) ?: return null }
            parts.joinToString(" or ")
        }
        else -> null
    }
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
/**
 * Strip a sibling-distinctness `Other(<ref>)` predicate from a target node's filter. mtgish models
 * "two target creatures" as a first plain target plus a second whose filter is `And[Other(ref1),
 * <same type>]` — the `Other` only enforces that the two chosen objects differ, which the engine does
 * automatically (CR 115.1b). Drop the `Other` arm so the second target's filter renders to the same
 * type filter as the first. Returns the original node when there's no such clause.
 */
private fun JsonObject.stripSiblingOther(): JsonObject {
    val args = this["args"] as? JsonObject ?: return this
    if (args.strField("_Permanents") != "And") return this
    val arms = args["args"].asArr?.filterIsInstance<JsonObject>() ?: return this
    val kept = arms.filterNot { it.strField("_Permanents") == "Other" }
    if (kept.size == arms.size) return this // no Other arm
    val newFilter: JsonElement = when (kept.size) {
        0 -> return this // nothing left to filter on — keep original rather than widen to "any"
        1 -> kept[0]
        else -> buildJsonObject {
            put("_Permanents", JsonPrimitive("And"))
            put("args", JsonArray(kept))
        }
    }
    return buildJsonObject {
        this@stripSiblingOther.forEach { (k, v) -> if (k != "args") put(k, v) }
        put("args", newFilter)
    }
}

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
 * True iff the oracle text introduces its modes with "choose up to" (e.g. "choose up to one —",
 * "choose up to two —"). mtgish sometimes drops the "up to" and encodes such a card as a bare
 * `Modal_ChooseOne`, which the engine would render as a MANDATORY choose-one (minChooseCount = 1).
 * The modal renderers consult this to decline those lossy cases rather than force a choice the printed
 * card lets the player skip (Biblioplex Tomekeeper). Note the dedicated `Modal_ChooseUptoNumber` /
 * `Modal_ChooseUptoOne` IR tags carry the "up to" faithfully and aren't affected by this guard.
 */
internal fun EmitCtx.oracleSaysChooseUpTo(): Boolean =
    oracleText?.lowercase()?.contains("choose up to") == true

/**
 * Split a modal spell's oracle text into its per-mode bullet strings. mtgish carries no per-mode
 * label, but the engine's `mode("…")` wants the printed bullet text, so derive it from Scryfall's
 * oracle text: drop the "Choose one —" (or "Choose up to N —") header line, then split on the `•`
 * bullet marker. Returns null if the oracle text is absent or has no bullets — the renderer then
 * declines (→ SCAFFOLD) rather than invent labels.
 */
internal fun EmitCtx.modeBullets(): List<String>? {
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
    if (targets == null) return null
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
    // See modalChooseOneEffectExpr: a "choose up to one" card mis-encoded as bare `Modal_ChooseOne`
    // can't render to the mandatory `modal(chooseCount = 1)` without forcing a choice the card lets the
    // player decline. Decline -> SCAFFOLD.
    if (oracleSaysChooseUpTo()) { reasons.add("modal-spell"); return null }
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
 * "Choose up to N. You may choose the same mode more than once." modal spell
 * (`SpellActions { Modal_ChooseNumberMayChooseSameModeMoreThanOnce([N, [arm, arm, …]]) }`, CR 700.2d) →
 * a `spell { modal(chooseCount = N, minChooseCount = 0, allowRepeat = true) { mode("…") { … } … } }`
 * block (Moment of Reckoning). The IR args are `[<Integer N>, [arms]]`; each arm renders via the same
 * [modalArmBody] machinery as `Modal_ChooseOne`, so a mode emits exactly as the equivalent stand-alone
 * spell. Mode labels come from the oracle-text bullets. Declines (→ SCAFFOLD) unless N is a fixed
 * integer, every arm renders, AND the bullet count matches the arm count — a card never emits with a
 * missing or mislabeled mode.
 */
internal fun EmitCtx.modalChooseNumberSpell(card: JsonObject): List<Stmt>? {
    val spellActions = (card["Rules"].asArr ?: return null).filterIsInstance<JsonObject>()
        .firstNotNullOfOrNull { rule ->
            (rule["args"] as? JsonObject)
                ?.takeIf { it.strField("_Actions") == "Modal_ChooseNumberMayChooseSameModeMoreThanOnce" }
        } ?: return null
    val outer = spellActions["args"].asArr ?: return null
    val chooseCount = findInteger(outer.getOrNull(0)) as? Int ?: run { reasons.add("modal-spell"); return null }
    val arms = outer.getOrNull(1).asArr?.filterIsInstance<JsonObject>() ?: return null
    if (arms.isEmpty()) return null

    val bullets = modeBullets() ?: run { reasons.add("modal-spell"); return null }
    if (bullets.size != arms.size) { reasons.add("modal-spell"); return null }

    val modeBlocks = arms.mapIndexed { i, arm ->
        val body = modalArmBody(arm) ?: run { reasons.add("modal-spell"); return null }
        Sub(Block("mode(\"${ktStr(bullets[i])}\")", body))
    }
    val builder = "modal(chooseCount = $chooseCount, minChooseCount = 0, allowRepeat = true)"
    return listOf(Sub(Block("spell", listOf(Sub(Block(builder, modeBlocks))))))
}

/**
 * One arm of a `Modal_ChooseOne` on a triggered ability → a `Mode.withTarget(effect, target, "bullet")`
 * (a single targeted mode) or `Mode.noTarget(effect, "bullet")` (an untargeted mode) call expression.
 *
 * Unlike [modalArmBody] (which emits `mode("…") { val t = target(…); effect = … }` statement blocks for
 * the spell-side `modal(chooseCount = 1) { }` builder), a triggered ability hosts its modal as a plain
 * effect — `effect = ModalEffect.chooseOne(Mode.withTarget(…), …)`. So each arm renders to the inline
 * `Mode.*` call form, binding any per-mode target to the modal slot `EffectTarget.ContextTarget(0)`.
 *
 * Renders only the shapes already covered by the per-effect handlers: an untargeted `ActionList`, or a
 * `Targeted` arm with exactly one conventional target. Returns null for anything else so the whole
 * modal trigger declines to SCAFFOLD rather than emit a partial/lossy arm.
 */
private fun EmitCtx.modalArmEffectExpr(arm: JsonObject, bullet: String): Dsl? {
    when (arm.strField("_Actions")) {
        "ActionList" -> {
            val actions = arm["args"].asArr?.filterIsInstance<JsonObject>() ?: return null
            val effect = renderEffectList(actions, "EffectTarget.ContextTarget(0)") ?: return null
            return call("Mode.noTarget", arg(effect), arg(Lit("\"${ktStr(bullet)}\"")))
        }
        "Targeted" -> {
            val (targets, actions) = targetedArms(arm) ?: return null
            if (targets == null || targets.size != 1) return null
            val tnode = targetExpr(targets[0], actions)
                ?: run { reasons.add("target:${targets[0].strField("_Target")}"); return null }
            val effect = renderEffectList(actions, "EffectTarget.ContextTarget(0)") ?: return null
            return call("Mode.withTarget", arg(effect), arg(tnode), arg(Lit("\"${ktStr(bullet)}\"")))
        }
        else -> return null
    }
}

/**
 * A `Modal_ChooseOne` actions node (on a triggered ability) → a `ModalEffect.chooseOne(Mode.…, Mode.…)`
 * effect expression, the inline modal form a `triggeredAbility { effect = … }` hosts (the sibling of
 * [modalChooseOneSpell], which uses the spell-only `modal(chooseCount = 1) { }` builder). Mode labels
 * come from the oracle-text bullets ([modeBullets]); each arm renders via [modalArmEffectExpr].
 *
 * Declines (→ SCAFFOLD) unless every arm renders AND the bullet count matches the arm count, so a modal
 * trigger never emits with a missing or mislabeled mode.
 */
internal fun EmitCtx.modalChooseOneEffectExpr(actionsNode: JsonObject): Dsl? {
    if (actionsNode.strField("_Actions") != "Modal_ChooseOne") return null
    // mtgish encodes some "choose up to one —" cards (Biblioplex Tomekeeper) as a bare `Modal_ChooseOne`,
    // dropping the optional "up to" (which the engine models as minChooseCount = 0 — the player may
    // decline). `ModalEffect.chooseOne` is MANDATORY (minChooseCount = 1), so rendering it for an
    // "up to one" card would force a choice the card lets the player skip. Decline -> SCAFFOLD rather
    // than emit that lossy mandatory modal.
    if (oracleSaysChooseUpTo()) { reasons.add("modal-trigger"); return null }
    val arms = actionsNode["args"].asArr?.filterIsInstance<JsonObject>() ?: return null
    if (arms.isEmpty()) return null

    val bullets = modeBullets() ?: run { reasons.add("modal-trigger"); return null }
    if (bullets.size != arms.size) { reasons.add("modal-trigger"); return null }

    val modeCalls = arms.mapIndexed { i, arm ->
        modalArmEffectExpr(arm, bullets[i]) ?: run { reasons.add("modal-trigger"); return null }
    }
    return Call("ModalEffect.chooseOne", modeCalls.map { arg(it) })
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
                if (targets == null || targets.isEmpty()) { reasons.add("Spree"); return null }
                when (targets.size) {
                    1 -> {
                        val tnode = targetExpr(targets[0], actions)
                            ?: run { reasons.add("target:${targets[0].strField("_Target")}"); return null }
                        // The targeted arm spends the modal target slot — bind the effect's target ref to it.
                        val effect = renderEffectList(actions, "EffectTarget.ContextTarget(0)") ?: return null
                        if (effect is Composite) { reasons.add("Spree"); return null }
                        modeArgs.add(arg("effect", effect))
                        modeArgs.add(arg("targetRequirements", call("listOf", arg(tnode))))
                    }
                    2 -> {
                        // A two-target mode (Shifting Grift: "exchange control of two target …"). Render
                        // both target requirements and bind the per-kind refs (Ref_TargetPermanentN) to
                        // the mode-local ContextTarget(0)/(1) so the effect resolves against them. Restore
                        // the ref-var state afterwards so other modes/cards are unaffected.
                        val tnodes = targets.map { t ->
                            // "two target creatures" — the second slot carries an `Other(<sibling ref>)`
                            // distinctness clause. Distinct targets are automatic (CR 115.1b: a spell
                            // can't choose the same object for two of its targets), so strip the sibling
                            // `Other` before rendering the filter rather than declining on it.
                            val cleaned = t.stripSiblingOther()
                            targetExpr(cleaned, actions) ?: run { reasons.add("target:${t.strField("_Target")}"); return null }
                        }
                        val kinds = targets.map { refKindForTarget(it) }
                        if (kinds.any { it == null } || kinds.toSet().size != 1) { reasons.add("Spree"); return null }
                        val kind = kinds.first()!!
                        val ctxRefs = listOf("EffectTarget.ContextTarget(0)", "EffectTarget.ContextTarget(1)")
                        val savedByKind = targetRefVarsByKind
                        val savedVars = targetVars
                        targetRefVarsByKind = mapOf(kind to ctxRefs)
                        targetVars = ctxRefs
                        val effect = try {
                            renderEffectList(actions, ctxRefs.first())
                        } finally {
                            targetRefVarsByKind = savedByKind
                            targetVars = savedVars
                        } ?: return null
                        if (effect is Composite) { reasons.add("Spree"); return null }
                        modeArgs.add(arg("effect", effect))
                        modeArgs.add(arg("targetRequirements", call("listOf", *tnodes.map { arg(it) }.toTypedArray())))
                    }
                    else -> { reasons.add("Spree"); return null }
                }
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
    // "Choose up to N. You may choose the same mode more than once." (Moment of Reckoning) renders to
    // `modal(chooseCount = N, minChooseCount = 0, allowRepeat = true) { … }`. Render before the generic
    // `Modal_*` scaffold guard.
    modalChooseNumberSpell(card)?.let { return it }
    // Other modal spells ("Choose up to four", entwine, escalate, …) carry a `Modal_*` envelope whose
    // children are the individual modes. The generic envelope path below would grab only the FIRST mode
    // and silently drop the rest, so scaffold the whole card rather than emit one arm of a modal spell.
    if ("\"Modal_" in compact(card["Rules"])) { reasons.add("modal-spell"); return null }
    // SOS whole-spell composite recognizers (exact-shape, render the full block or decline).
    wisdomOfAgesSpell(card)?.let { return it }
    // One-line `effect =` shortcuts, then whole-block shortcuts, then the generic envelope path.
    eachplayerMaydraw(card)?.let { return spellOf(it) }
    fluxEffect(card)?.let { return spellOf(it) }
    windsEffect(card)?.let { return spellOf(it) }
    extraTurnEffect(card)?.let { return spellOf(it) }
    distributedSpell(card)?.let { return it }
    balanceEffect(card)?.let { return it }
    conditionalSpell(card)?.let { return it }

    // Extract the spell body from the `SpellActions` rule specifically, not the whole `card["Rules"]`
    // tree. A card can carry a sibling cast-trigger rule (`FromStack { TriggerA … }` — Infusion copy on
    // Lumaret's Favor, Social Snub) whose inner `ActionList`/`Targeted` envelope precedes the
    // `SpellActions` rule; walking the whole tree would grab THAT envelope first and render the trigger's
    // actions as the spell body (and scaffold on them), dropping the real spell. Scope to the
    // SpellActions rule so the spell renders its own body.
    val spellRule = (card["Rules"].asArr ?: JsonArray(emptyList()))
        .filterIsInstance<JsonObject>().firstOrNull { it.strField("_Rule") == "SpellActions" }
        ?: card  // fall back to the whole card for cards whose body isn't under a SpellActions rule
    val (targets, rawActions) = extractEnvelope(spellRule)
    if (rawActions == null) return null

    // Paradigm (Secrets of Strixhaven) lowers to the spell-envelope ability word `paradigm()`, not an
    // effect: the IR carries a trailing `Paradigm(ThisSpell)` action after the spell's real body. Strip
    // it here and emit `paradigm()` in the spell block; the remaining actions render through the normal
    // body path, so every body the emitter can already render automatically gains Paradigm support. Only
    // the trailing-action form (`Paradigm` as the last action of the spell's ActionList, scoped to
    // ThisSpell) is recognized — anything else declines to SCAFFOLD via the normal path.
    val paradigm = rawActions.lastOrNull()?.let { it.strField("_Action") == "Paradigm" && jsonContains(it, "_Spell", "ThisSpell") } == true
    val actions = if (paradigm) rawActions.dropLast(1) else rawActions
    if (actions.isEmpty()) return null
    fun withParadigm(stmts: List<Stmt>): List<Stmt> =
        if (paradigm) listOf<Stmt>(RawLine("        paradigm()")) + stmts else stmts

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
            return listOf(Sub(Block("spell", withParadigm(stmts))))
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
    return listOf(Sub(Block("spell", withParadigm(stmts))))
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
    // "Whenever this permanent becomes tapped" (SELF) — e.g. Wylie Duke, Atiin Hero
    // ("Whenever Wylie Duke becomes tapped, you gain 1 life and draw a card.").
    "WhenAPermanentBecomesTapped" to "Triggers.BecomesTapped",
)

/**
 * A modal `TriggerA` rule ("At the beginning of combat on your turn, choose one — …") →
 * `triggeredAbility { trigger = …; effect = ModalEffect.chooseOne(Mode.…, Mode.…) }`.
 *
 * Only the plain `Modal_ChooseOne` shape with a recoverable trigger spec renders: the trigger comes
 * from [triggerSpecFor] (after unwrapping any intervening-if gate), and the modal effect from
 * [modalChooseOneEffectExpr] (per-mode targets bound to the modal slot). Returns null — so the caller
 * falls through to its `Modal_*` decline → SCAFFOLD — for any other modal kind, an unrecoverable
 * trigger, an intervening-if-gated modal trigger (not yet calibrated), or an "optional"/once-per-turn
 * wrapper, rather than emit a partial/lossy modal trigger.
 */
private fun EmitCtx.modalTriggerBlock(rule: JsonObject, oncePerTurn: Boolean, triggerCondition: String?): List<Stmt>? {
    // Find the modal actions node directly under the TriggerA (args = [trigger, Modal_ChooseOne]).
    val modalNode = rule["args"].asArr?.filterIsInstance<JsonObject>()
        ?.firstOrNull { it.strField("_Actions") == "Modal_ChooseOne" } ?: return null

    // Recover the trigger spec the same way the non-modal path does; decline (→ caller's SCAFFOLD) on
    // an intervening-if-gated modal trigger or any once-per-turn/extra wrapper we haven't calibrated.
    val (effRule, effTriggerCondition) = unwrapIfGatedTrigger(rule, triggerCondition)
    if (effTriggerCondition != null || oncePerTurn) return null
    val spec = triggerSpecFor(effRule) ?: return null

    val modal = modalChooseOneEffectExpr(modalNode) ?: return null
    return listOf(Sub(Block("triggeredAbility", listOf(
        Assign("trigger", Lit(spec)),
        Assign("effect", modal),
    ))))
}

/**
 * The Opus ability word (Secrets of Strixhaven) -> the `opus { }` builder. "Opus" is a flavor ability
 * word (CR 207.2c) with **no IR signal**, so the mechanic is recognised purely from its structural
 * shape: a `WhenAPlayerCastsASpell(You, Instant|Sorcery)` trigger whose sole action is an
 * `IfElse(SpellPassesFilter(ThatSpell, AnAmountOfManaWasSpentToCastIt >= 5), <then>, <else>)`. The
 * builder lowers to exactly the `ConditionalEffect(5+ -> then, otherwise -> else)` gameplay tree this
 * recognises, so the emit is gameplay-tree-identical (Deluge Virtuoso's +2/+2 instead of +1/+1).
 *
 * Only the "replaces" tier (`IfElse` with both arms present) maps to `insteadIfFiveOrMore`; the
 * additive tier (`alsoIfFiveOrMore`, a base effect THEN a 5+ bonus) has a different IR shape and is
 * left to a future recogniser. Anything off the exact shape — a different mana threshold/comparator, a
 * spell filter that isn't bare instant-or-sorcery, an `oncePerTurn`/intervening-if wrapper, an extra
 * action, or a branch that doesn't render to a single effect — declines (null), so the generic trigger
 * path scaffolds rather than collapse a non-Opus shape.
 */
private fun EmitCtx.opusTriggerBlock(rule: JsonObject, oncePerTurn: Boolean, triggerCondition: String?): List<Stmt>? {
    if (oncePerTurn || triggerCondition != null) return null
    val ruleArgs = rule["args"].asArr ?: return null
    // Trigger: WhenAPlayerCastsASpell(You, Or[IsCardtype Instant, IsCardtype Sorcery]).
    val trig = ruleArgs.getOrNull(0) as? JsonObject ?: return null
    if (trig.strField("_Trigger") != "WhenAPlayerCastsASpell") return null
    if (!jsonContains(trig, "_Player", "You")) return null
    val trigArgs = trig["args"].asArr ?: return null
    val spellFilter = trigArgs.getOrNull(1) as? JsonObject ?: return null
    if (spellFilter.strField("_Spells") != "Or") return null
    val orArgs = spellFilter["args"].asArr?.filterIsInstance<JsonObject>() ?: return null
    val orTypes = orArgs
        .filter { it.strField("_Spells") == "IsCardtype" }
        .mapNotNull { it["args"].asStr() }
        .toSet()
    if (orArgs.size != 2 || orTypes != setOf("Instant", "Sorcery")) return null

    // Sole action: a single IfElse with both arms.
    val (targets, actions) = extractEnvelope(rule)
    val ifElse = actions?.singleOrNull()?.takeIf { it.strField("_Action") == "IfElse" } ?: return null
    val ifArgs = ifElse["args"].asArr ?: return null
    val cond = ifArgs.getOrNull(0) as? JsonObject ?: return null
    if (!isFiveOrMoreManaSpentCondition(cond)) return null
    val thenActions = (ifArgs.getOrNull(1) as? JsonArray)?.filterIsInstance<JsonObject>() ?: return null
    val elseActions = (ifArgs.getOrNull(2) as? JsonArray)?.filterIsInstance<JsonObject>() ?: return null
    if (thenActions.isEmpty() || elseActions.isEmpty()) return null

    // mtgish encodes the count on an Opus 5+ `PutNumberCountersOfTypeOnPermanent` bonus arm
    // UNRELIABLY — it reuses the literal 5-mana threshold as the counter count rather than the
    // printed value (verified wrong: Tackle Artist's IR says 5 where the oracle prints "two";
    // Spectacular Skywhale's says 5 where the oracle prints "three"). We can't recover the true
    // count from the IR, so decline this shape to SCAFFOLD rather than emit a confidently-wrong
    // counter count ("render correctly or decline — never emit a lossy approximation").
    if ((thenActions as List<JsonObject>?).orEmpty().any {
            it.strField("_Action") == "PutNumberCountersOfTypeOnPermanent"
        }
    ) {
        reasons.add("opus-counter-count")
        return null
    }

    // An optional single bound target shared by both arms ("target player mills three cards … that
    // player mills ten cards instead" — Exhibition Tidecaller). The `opus { }` builder hosts a
    // `target(…)` local just like a triggered ability, and BOTH arms reference the same chosen target
    // ("that player" in the 5+ arm), so the target is declared once and threaded into each arm's
    // effect via the bound-target var. `spellTargetExpr` returns (null, null) for the no-target opus
    // (Deluge Virtuoso's self-pump) and declines (-> SCAFFOLD) on a target it can't render exactly.
    val (tnode, tvar) = spellTargetExpr(targets, actions) ?: return null

    // A spell-cast trigger's triggering entity is the spell, so any "that …" body reference resolves
    // against the caster — match the generic path's bookkeeping while rendering the two arms.
    val prev = triggeringEntityIsSpell
    triggeringEntityIsSpell = true
    val base = renderEffectList(elseActions, tvar).also { triggeringEntityIsSpell = prev } ?: return null
    triggeringEntityIsSpell = true
    val bonus = renderEffectList(thenActions, tvar).also { triggeringEntityIsSpell = prev } ?: return null

    val body = buildList {
        if (tnode != null) add(targetLocal(tnode))
        add(Assign("effect", base))
        add(Assign("insteadIfFiveOrMore", bonus))
    }
    return listOf(Sub(Block("opus", body)))
}

/**
 * True iff a `TriggerA` rule has the Opus *structural* shape: a `WhenAPlayerCastsASpell(You, instant|
 * sorcery)` trigger whose sole action is an `IfElse` gated on the 5-or-more-mana-spent condition. This
 * is the recognition net [opusTriggerBlock] uses, minus the per-arm rendering. [triggerBlock] consults
 * it so that when a card is unmistakably Opus but [opusTriggerBlock] *declines* (e.g. the unreliable
 * `PutNumberCountersOfTypeOnPermanent` count), the whole card downgrades to SCAFFOLD rather than
 * falling through to the generic trigger path and re-rendering the same wrong value.
 */
private fun EmitCtx.isOpusShaped(rule: JsonObject): Boolean {
    val ruleArgs = rule["args"].asArr ?: return false
    val trig = ruleArgs.getOrNull(0) as? JsonObject ?: return false
    if (trig.strField("_Trigger") != "WhenAPlayerCastsASpell") return false
    if (!jsonContains(trig, "_Player", "You")) return false
    val spellFilter = trig["args"].asArr?.getOrNull(1) as? JsonObject ?: return false
    if (spellFilter.strField("_Spells") != "Or") return false
    val orTypes = spellFilter["args"].asArr?.filterIsInstance<JsonObject>()
        ?.filter { it.strField("_Spells") == "IsCardtype" }
        ?.mapNotNull { it["args"].asStr() }?.toSet() ?: return false
    if (orTypes != setOf("Instant", "Sorcery")) return false
    val (_, actions) = extractEnvelope(rule)
    val ifElse = actions?.singleOrNull()?.takeIf { it.strField("_Action") == "IfElse" } ?: return false
    val cond = ifElse["args"].asArr?.getOrNull(0) as? JsonObject ?: return false
    return isFiveOrMoreManaSpentCondition(cond)
}

/**
 * True iff a resolution-time condition node is exactly "5 or more mana was spent to cast that spell" —
 * `SpellPassesFilter(Trigger_ThatSpell, AnAmountOfManaWasSpentToCastIt(GreaterThanOrEqualTo, Integer 5))`
 * — the Opus 5+ tier gate. Any other comparator, threshold, spell ref, or filter returns false so a
 * near-miss declines rather than mis-rendering as Opus.
 */
private fun isFiveOrMoreManaSpentCondition(cond: JsonObject): Boolean {
    if (cond.strField("_Condition") != "SpellPassesFilter") return false
    val blob = compact(cond)
    if ("AnAmountOfManaWasSpentToCastIt" !in blob) return false
    if (!jsonContains(cond, "_Comparison", "GreaterThanOrEqualTo")) return false
    // The compared count must be the literal Integer 5 — Opus's fixed threshold.
    return (findInteger(cond) as? Int) == 5
}

/** A TriggerA rule (self-triggered) -> triggeredAbility { trigger; [triggerCondition]; [target]; effect }.
 *  [oncePerTurn] is set by the `TriggerOnceEachTurn` rule envelope, whose body is otherwise shaped
 *  identically to a TriggerA. [triggersOnce] is the lifetime cap ("This ability triggers only once"),
 *  set by the `TriggerIOnce` envelope; like oncePerTurn it just rides through as a `triggersOnce = true`
 *  line on an otherwise TriggerA-shaped body. [triggerCondition] is an optional intervening-if condition
 *  DSL (CR 603.4) supplied by an enclosing gate (e.g. the "while saddled" `If` wrapper passes
 *  `Conditions.SourceIsSaddled`); it renders as a `triggerCondition = …` line. */
internal fun EmitCtx.triggerBlock(
    rule: JsonObject,
    oncePerTurn: Boolean = false,
    triggerCondition: String? = null,
    triggersOnce: Boolean = false,
): List<Stmt>? {
    // A "choose one —" modal triggered ability hosts its modal as a plain effect:
    // `triggeredAbility { trigger = …; effect = ModalEffect.chooseOne(Mode.…, Mode.…) }`. Render the
    // `Modal_ChooseOne` arms via [modalChooseOneEffectExpr] (the inline `Mode.*` form), per-mode targets
    // bound to the modal slot. Any other `Modal_*` shape (choose N, choose up to N, …) — or a
    // `Modal_ChooseOne` whose arms / bullets can't be recovered exactly — still declines to SCAFFOLD
    // inside the renderer rather than emit a partial/lossy modal.
    modalTriggerBlock(rule, oncePerTurn, triggerCondition)?.let { return it }
    if ("\"Modal_" in compact(rule)) { reasons.add("modal-trigger"); return null }

    // Opus (Secrets of Strixhaven) — the `opus { }` ability-word builder. Recognise its exact structural
    // shape before the generic trigger path; "Opus" is a flavor ability word with no IR signal, so it is
    // inferred from structure alone (then matched precisely so nothing else collapses). When the card is
    // unmistakably Opus-shaped but the renderer declines (e.g. the unreliable counter-count arm), scaffold
    // the whole card rather than letting the generic trigger path re-render the same wrong value.
    opusTriggerBlock(rule, oncePerTurn, triggerCondition)?.let { return it }
    if (isOpusShaped(rule)) { reasons.add("opus"); return null }

    // "Whenever this creature enters OR attacks, …" — an `Or` of sibling self-triggers sharing one
    // effect body (Stadium Tidalmage, Dragonhawk's enters/attacks). One `TriggerSpec` can't express the
    // union (each arm has its own event + binding), so the hand-authored idiom is N separate
    // `triggeredAbility` blocks sharing the same rendered effect. Recognise the exact shape — an `Or`
    // trigger whose every arm is itself recoverable by [triggerSpecFor] — rewrite the rule per arm
    // (each carrying the same actions / targets envelope) and recurse, concatenating the blocks. Any
    // arm [triggerSpecFor] can't render whole declines the WHOLE union -> SCAFFOLD (never a partial that
    // silently drops an arm). Done before [unwrapIfGatedTrigger], whose single-trigger assumption the
    // `Or` node would otherwise break.
    orTriggerBlocks(rule, oncePerTurn, triggerCondition)?.let { return it }

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

    // A triggered ability that returns its own source from the graveyard (Eerie recursion — Fear of
    // Infinity's "Whenever an enchantment you control enters …, you may return this card from your
    // graveyard to your hand") must function from the graveyard: the hand-authored idiom is
    // `triggerZone = Zone.GRAVEYARD` plus a resolution-time `MayEffect`. This envelope emits neither
    // (it would wrongly render a battlefield-zone `optional = true` trigger), so decline to SCAFFOLD
    // rather than emit a trigger that never fires from the graveyard.
    if (effectActions.any { jsonContains(it, "_GraveyardCard", "ThisGraveyardCard") }) {
        reasons.add("graveyard-active-trigger")
        return null
    }

    // An intervening-if written in the trigger's effect — "Whenever ~ attacks, create a token IF you
    // control a creature with power 4 or greater" (Scalestorm Summoner) — is modeled in mtgish as a lone
    // `If{cond}[then]` action, not a TriggerI. Per CR 603.4 this IS an intervening-if ability: the
    // condition is checked when the trigger would fire and again on resolution. Lift it to a
    // `triggerCondition = …` gate over the then-branch, but ONLY when the condition renders via the same
    // strict [interveningIfDsl] used by TriggerI (no else-branch, single If). Any other shape falls
    // through to the normal action path (where `on("If")` handles the static gates or declines).
    // An `Unless{cond}[actions]` action — "do [actions] unless [cond]" (Tragedy Feaster's Infusion
    // "sacrifice a permanent unless you gained life this turn") — lifts to a NEGATED triggerCondition
    // gate (CR 603.4) over the inner actions, the mirror of the intervening-if lift.
    val lifted = if (effTriggerCondition == null) {
        liftInterveningIfAction(effectActions) ?: liftUnlessAction(effectActions)
    } else null
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
    if (triggersOnce) stmts.add(Assign("triggersOnce", Lit("true")))
    if (mayWrapped && !selfOptional) stmts.add(Assign("optional", Lit("true")))
    if (tvar != null) stmts.add(targetLocal(tnode!!))
    stmts.add(Assign("effect", edsl))
    return listOf(Sub(Block("triggeredAbility", stmts)))
}

/**
 * "Whenever ~ enters OR attacks, …" — a `TriggerA` whose trigger (`args[0]`) is an `Or` of two-plus
 * sibling sub-triggers that share one effect body. Returns N `triggeredAbility` blocks (one per arm,
 * each rendered through [triggerBlock] with the same actions/targets envelope), or null when this isn't
 * an Or-trigger or any arm can't be rendered whole.
 *
 * Each arm rule is the original rule with `args[0]` replaced by that arm's `_Trigger` node — every other
 * field (the actions envelope at `args[1]`, the rule kind) is preserved — so [triggerBlock] renders each
 * arm exactly as if it were written as its own ability, sharing the identical effect tree. This mirrors
 * the hand-authored idiom (two `triggeredAbility { }` blocks sharing one `effect` val, e.g. Dragonhawk,
 * Stadium Tidalmage). If even one arm declines, the whole union returns null so the card scaffolds rather
 * than silently dropping an arm.
 */
private fun EmitCtx.orTriggerBlocks(rule: JsonObject, oncePerTurn: Boolean, triggerCondition: String?): List<Stmt>? {
    val args = rule["args"].asArr ?: return null
    val trig = args.getOrNull(0) as? JsonObject ?: return null
    if (trig.strField("_Trigger") != "Or") return null
    val arms = trig["args"].asArr?.filterIsInstance<JsonObject>() ?: return null
    if (arms.size < 2) return null
    if (arms.any { it.strField("_Trigger") == null || it.strField("_Trigger") == "Or" }) return null

    // "Whenever this creature OR another <filter> enters" — the self-or-other-matching ETB union
    // (Bogwater Lumaret: "this creature or another creature you control enters"). Two arms, both
    // WhenAPermanentEntersTheBattlefield: one is the bare self ETB (`ThisPermanent`), the other is an
    // `Other(ThisPermanent)` ETB whose remaining filter the source itself satisfies. CR-wise this is
    // exactly one ANY-binding trigger over that filter — `ANY` matches the source AND any other matching
    // permanent — which is the hand-authored idiom. Collapse to a single `entersBattlefield(filter, ANY)`
    // when the shape is exactly that (and the "other" arm's filter renders whole); otherwise fall through
    // to the per-arm expansion below.
    if (arms.size == 2) {
        val selfArm = arms.firstOrNull { isBareSelfEtb(it) }
        val otherArm = arms.firstOrNull { it !== selfArm }
        if (selfArm != null && otherArm != null &&
            jsonContains(otherArm, "_Trigger", "WhenAPermanentEntersTheBattlefield") &&
            jsonContains(otherArm, "_Permanents", "Other")
        ) {
            // Strip the `Other(ThisPermanent)` clause from the "other" arm's subject so it becomes a
            // plain "<filter> enters" ETB. triggerBlock then renders it as a single ANY-binding
            // entersBattlefield trigger (ANY covers both the source and any other matching permanent),
            // sharing the same effect body. If the stripped shape can't be rendered whole, fall through
            // to the per-arm expansion below rather than emit a wrong card.
            val plainOtherArm = stripOtherThisPermanentClause(otherArm)
            if (plainOtherArm != null) {
                val collapsedRule = buildJsonObject {
                    rule.forEach { (k, v) -> if (k != "args") put(k, v) }
                    put("args", JsonArray(listOf<JsonElement>(plainOtherArm) + args.drop(1)))
                }
                triggerBlock(collapsedRule, oncePerTurn, triggerCondition)?.let { return it }
            }
        }
    }

    val out = mutableListOf<Stmt>()
    for (arm in arms) {
        val armRule = buildJsonObject {
            rule.forEach { (k, v) -> if (k != "args") put(k, v) }
            put("args", JsonArray(listOf<JsonElement>(arm) + args.drop(1)))
        }
        val block = triggerBlock(armRule, oncePerTurn, triggerCondition) ?: return null
        out.addAll(block)
    }
    return out
}

/**
 * True iff [arm] is the bare "this permanent enters" ETB — a `WhenAPermanentEntersTheBattlefield`
 * whose subject is exactly `ThisPermanent` with no `Other` / type / controller constraints. The self
 * half of a "this creature or another <filter> enters" union (Bogwater Lumaret).
 */
private fun isBareSelfEtb(arm: JsonObject): Boolean {
    if (arm.strField("_Trigger") != "WhenAPermanentEntersTheBattlefield") return false
    if (!jsonContains(arm, "_Permanent", "ThisPermanent")) return false
    return !jsonContains(arm, "_Permanents", "Other") &&
        !jsonContains(arm, "_Permanents", "IsCardtype") &&
        !jsonContains(arm, "_Permanents", "ControlledByAPlayer")
}

/**
 * Remove the `Other(ThisPermanent)` clause from [arm]'s subject filter, collapsing a now-single-element
 * `And` to its sole child. Turns the "another creature you control enters" arm into a plain "creature
 * you control enters" trigger (which renders as an ANY binding). Returns null if no such clause was
 * found (so the caller falls back to the per-arm union rather than emit an unchanged tree).
 */
private fun stripOtherThisPermanentClause(arm: JsonObject): JsonObject? {
    var removed = false
    fun isOtherThisPermanent(node: JsonElement?): Boolean =
        node is JsonObject && node.strField("_Permanents") == "Other" &&
            jsonContains(node, "_Permanent", "ThisPermanent")
    fun strip(node: JsonElement): JsonElement = when (node) {
        is JsonObject -> {
            // An `And` of permanent filters: drop any `Other(ThisPermanent)` member, recurse the rest.
            if (node.strField("_Permanents") == "And") {
                val kept = (node["args"].asArr ?: JsonArray(emptyList()))
                    .filterNot { isOtherThisPermanent(it) }
                    .map { strip(it) }
                if (kept.size != (node["args"].asArr?.size ?: 0)) removed = true
                when (kept.size) {
                    0 -> node // shouldn't happen; leave untouched
                    1 -> kept.single()
                    else -> buildJsonObject {
                        node.forEach { (k, v) -> if (k != "args") put(k, v) }
                        put("args", JsonArray(kept))
                    }
                }
            } else {
                buildJsonObject {
                    node.forEach { (k, v) -> put(k, strip(v)) }
                }
            }
        }
        is JsonArray -> JsonArray(node.map { strip(it) })
        else -> node
    }
    val result = strip(arm) as? JsonObject ?: return null
    return if (removed) result else null
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
 * A `TriggerIOnce` rule — a TriggerI ([trigger, condition, actions]) that ALSO carries the lifetime cap
 * "This ability triggers only once" (CR 603.6e-style permanent latch). Same recovery path as
 * [triggerIBlock] (intervening-if condition lifted out, body re-keyed to a TriggerA), with the extra
 * `triggersOnce = true` rider threaded through. The DSK Survival cards Acrobatic Cheerleader / Pearl
 * Collector / Jet Collector use this shape: "Survival — At the beginning of your second main phase, if
 * <condition>, <effect>. This ability triggers only once."
 */
internal fun EmitCtx.triggerIOnceBlock(rule: JsonObject): List<Stmt>? {
    val args = rule["args"].asArr ?: run { reasons.add("TriggerIOnce"); return null }
    val cond = args.getOrNull(1) as? JsonObject
    val condDsl = interveningIfDsl(cond) ?: run { reasons.add("TriggerIOnce"); return null }
    val triggerA = buildJsonObject {
        put("_Rule", JsonPrimitive("TriggerA"))
        put("args", JsonArray(listOfNotNull(args.getOrNull(0)) + listOfNotNull(args.getOrNull(2))))
    }
    return triggerBlock(triggerA, triggerCondition = condDsl, triggersOnce = true)
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
 * A trigger effect that is a single `Unless{cond}[actions]` action -> a NEGATED intervening-if
 * condition gate plus the inner actions, or null when it isn't a lone liftable `Unless`. Used by
 * [triggerBlock] to model the Infusion "sacrifice a permanent unless you gained life this turn"
 * shape (Tragedy Feaster) as `triggerCondition = Conditions.Not(<cond>)` over the inner actions
 * (CR 603.4): "do [actions] unless [cond]" means the trigger's actions resolve only when the
 * condition is FALSE.
 *
 * The IR `Unless` args are `[<condition>, [<actions>]]`. Renders ONLY when the condition renders via
 * the same strict [interveningIfDsl] used by the intervening-if path, so the negated gate is always
 * exact — anything else declines (-> SCAFFOLD) rather than dropping or widening the "unless" clause.
 */
private fun EmitCtx.liftUnlessAction(actions: List<JsonObject>): Pair<String, List<JsonObject>>? {
    val only = actions.singleOrNull() ?: return null
    if (only.strField("_Action") != "Unless") return null
    val args = only["args"].asArr ?: return null
    val cond = args.getOrNull(0) as? JsonObject ?: return null
    val innerActions = (args.getOrNull(1) as? JsonArray)?.filterIsInstance<JsonObject>() ?: return null
    if (innerActions.isEmpty()) return null
    val condDsl = interveningIfDsl(cond) ?: return null
    return "Conditions.Not($condDsl)" to innerActions
}

/**
 * A `TriggerMayOnceEachTurn` rule — a triggered ability whose actions are a "you may [do X]. Do this only
 * once each turn." rummage (Irreverent Gremlin: "Whenever another creature you control with power 2 or
 * less enters, you may discard a card. If you do, draw a card. Do this only once each turn.").
 *
 * The IR's once-each-turn tag bakes in BOTH the once-per-turn cap (CR 603.3b) AND the "you may" framing
 * over its `MustCost(...) + If(CostWasPaid)[...]` action body. So the body's forced `MustCost` becomes a
 * *resolution-time* MayEffect: `MayEffect(IfYouDoEffect(action = <cost-as-effect>, ifYouDo = <then>))`,
 * exactly the engine's loot idiom — and the ability carries `oncePerTurn = true`. The trigger spec is
 * recovered by the shared [triggerSpecFor] (so the "another creature you control with power 2 or less"
 * ETB filter round-trips through gameObjectFilterDsl, declining if it can't).
 *
 * Renders ONLY the exact rummage body: a `MustCost(DiscardACard)` followed by `If(CostWasPaid, [DrawACard])`
 * with no else-branch. Any other cost, then-effect, or shape declines (-> SCAFFOLD) rather than guess; in
 * particular the optional "may" must come solely from this once-each-turn tag, never invented for a bare
 * forced cost.
 */
internal fun EmitCtx.triggerMayOnceEachTurnBlock(rule: JsonObject): List<Stmt>? {
    val spec = triggerSpecFor(rule) ?: run { reasons.add("TriggerMayOnceEachTurn"); return null }
    val (_, actions) = extractEnvelope(rule)
    if (actions == null || actions.size != 2) { reasons.add("TriggerMayOnceEachTurn"); return null }
    val (mustCost, ifPaid) = actions
    // The forced cost — "discard a card" — becomes the IfYouDo's action effect.
    if (mustCost.strField("_Action") != "MustCost") { reasons.add("TriggerMayOnceEachTurn"); return null }
    val costAction = when ((mustCost["args"] as? JsonObject)?.strField("_Cost")) {
        "DiscardACard" -> call("Patterns.Hand.discardCards", arg("1"))
        else -> { reasons.add("TriggerMayOnceEachTurn"); return null }
    }
    // The gate must be exactly If(CostWasPaid, [then]) with no else-branch.
    if (ifPaid.strField("_Action") != "If") { reasons.add("TriggerMayOnceEachTurn"); return null }
    val ifArgs = ifPaid["args"].asArr ?: run { reasons.add("TriggerMayOnceEachTurn"); return null }
    if (!jsonContains(ifArgs.getOrNull(0), "_Condition", "CostWasPaid") || ifArgs.getOrNull(2) != null) {
        reasons.add("TriggerMayOnceEachTurn"); return null
    }
    val thenActions = (ifArgs.getOrNull(1) as? JsonArray)?.filterIsInstance<JsonObject>()
        ?: run { reasons.add("TriggerMayOnceEachTurn"); return null }
    val thenEffect = renderEffectList(thenActions, null) ?: run { reasons.add("TriggerMayOnceEachTurn"); return null }

    val effect = call(
        "MayEffect",
        arg("effect", call("IfYouDoEffect", arg("action", costAction), arg("ifYouDo", thenEffect))),
    )
    return listOf(Sub(Block("triggeredAbility", listOf(
        Assign("trigger", Lit(spec)),
        Assign("effect", effect),
        Assign("oncePerTurn", Lit("true")),
    ))))
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
    // "if it had counters on it" — DeadPermanentPassesFilter(HasACounter): the just-died permanent
    // (the triggering entity of a dies trigger) had at least one counter of any kind on it (Scolding
    // Administrator's "When this creature dies, if it had counters on it, …"). Maps to
    // Conditions.TriggeringEntityHadCounters. Only the bare HasACounter (any counter kind) renders;
    // a counter-type-specific filter declines -> SCAFFOLD rather than widening.
    if (cond.strField("_Condition") == "DeadPermanentPassesFilter" &&
        jsonContains(cond, "_Permanents", "HasACounter")
    ) {
        return "Conditions.TriggeringEntityHadCounters"
    }
    // "if a card left your graveyard this turn" — ACardLeftPlayersGraveyardThisTurn(AnyCard, You) ->
    // Conditions.CardsLeftGraveyardThisTurn(1) (Living History, Primary Research). Only the bare
    // "any card" + You scope renders; a typed card filter or another player scope has no calibrated
    // DSL, so it declines -> SCAFFOLD rather than widening the condition.
    if (cond.strField("_Condition") == "ACardLeftPlayersGraveyardThisTurn" &&
        jsonContains(cond, "_Cards", "AnyCard") &&
        jsonContains(cond, "_Player", "You")
    ) {
        return "Conditions.CardsLeftGraveyardThisTurn(1)"
    }
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
    // "if it's tapped" — PermanentPassesFilter(<subject>, IsTapped) over a bare IsTapped filter (no
    // other clause). Two subjects render:
    //  - Ref_TargetPermanent (the ability's first targeted permanent) -> Conditions.TargetIsTapped()
    //    (Shackle Slinger's "If it's tapped, put a stun counter on it. Otherwise, tap it.").
    //  - ThisPermanent (the source itself) -> Conditions.SourceIsTapped (the Survival ability word's
    //    "if this creature is tapped" — Cautious Survivor, Veteran Survivor).
    // Any other subject/filter declines -> SCAFFOLD.
    if (cond.strField("_Condition") == "PermanentPassesFilter") {
        val condArgs = cond["args"].asArr
        val subject = (condArgs?.getOrNull(0) as? JsonObject)?.strField("_Permanent")
        val filt = condArgs?.getOrNull(1) as? JsonObject
        if (filt?.strField("_Permanents") == "IsTapped" && filt.size == 1) {
            if (subject == "Ref_TargetPermanent" || subject == "Ref_TargetPermanent1") {
                return "Conditions.TargetIsTapped()"
            }
            if (subject == "ThisPermanent") {
                return "Conditions.SourceIsTapped"
            }
        }
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
    // "this enchantment isn't a creature" — PermanentPassesFilter(ThisPermanent, IsNonCardtype Creature)
    // (Emergent Haunting's end-step "becomes a creature" gate, which self-disables once animated).
    // Renders to Conditions.SourceMatches(GameObjectFilter.Noncreature). Only the exact bare
    // "isn't a creature" filter over ThisPermanent renders; another card type declines -> SCAFFOLD.
    if (cond.strField("_Condition") == "PermanentPassesFilter") {
        val condArgs = cond["args"].asArr
        val subject = (condArgs?.getOrNull(0) as? JsonObject)?.strField("_Permanent")
        val filt = condArgs?.getOrNull(1) as? JsonObject
        if (subject == "ThisPermanent" &&
            filt?.strField("_Permanents") == "IsNonCardtype" && filt.field("args").asStr() == "Creature"
        ) {
            return "Conditions.SourceMatches(GameObjectFilter.Noncreature)"
        }
    }
    // "if a creature died this turn" — ACreatureOrPlaneswalkerDiedThisTurn over a creature-cardtype
    // filter. Two calibrated shapes render:
    //  - bare "a creature" (no controller / subtype / count clause) -> Conditions.CreatureDiedThisTurn
    //    (Rictus Robber).
    //  - "a creature ... under your control" — And(IsCardtype Creature, ControlledByAPlayer You) ->
    //    Conditions.ControlledCreatureDiedThisTurn (Essenceknit Scholar's end-step draw).
    // Anything more specific (subtype, count, an opponent's control) declines -> SCAFFOLD.
    if (cond.strField("_Condition") == "ACreatureOrPlaneswalkerDiedThisTurn") {
        val filter = cond["args"] as? JsonObject
        val bareCreature = filter?.strField("_Permanents") == "IsCardtype" &&
            filter.field("args").asStr() == "Creature"
        if (bareCreature) return "Conditions.CreatureDiedThisTurn"
        // And(IsCardtype Creature, …) — a creature death narrowed by a second clause.
        if (filter?.strField("_Permanents") == "And") {
            val arms = filter["args"].asArr?.filterIsInstance<JsonObject>().orEmpty()
            val hasCreature = arms.any {
                it.strField("_Permanents") == "IsCardtype" && it.field("args").asStr() == "Creature"
            }
            val controlledByYou = arms.any {
                it.strField("_Permanents") == "ControlledByAPlayer" && jsonContains(it, "_Player", "You")
            }
            if (arms.size == 2 && hasCreature && controlledByYou) {
                return "Conditions.ControlledCreatureDiedThisTurn"
            }
            // "a non-<subtype> creature died this turn" — And(IsNonCreatureType <sub>, IsCardtype Creature)
            // (Undead Sprinter: "if a non-Zombie creature died this turn"). The negated creature subtype
            // maps to Conditions.NonSubtypeCreatureDiedThisTurn(Subtype.<SUB>); the positive form
            // (IsCreatureType) maps to SubtypeCreatureDiedThisTurn. Only the bare two-arm shape (the
            // subtype clause + the Creature cardtype, nothing else) renders; any extra clause declines.
            if (arms.size == 2 && hasCreature) {
                val negSub = arms.firstOrNull { it.strField("_Permanents") == "IsNonCreatureType" }?.field("args").asStr()
                if (negSub != null) return "Conditions.NonSubtypeCreatureDiedThisTurn(${subtypeArg(negSub)})"
                val posSub = arms.firstOrNull { it.strField("_Permanents") == "IsCreatureType" }?.field("args").asStr()
                if (posSub != null) return "Conditions.SubtypeCreatureDiedThisTurn(${subtypeArg(posSub)})"
            }
        }
        return null
    }
    // "if N or more creatures died this turn" — ANumberOfPermanentsDiedThisTurn([>= N], IsCardtype
    // Creature) (Emeritus of Woe's "if two or more creatures died this turn" end-step gate). The bare
    // ACreatureOrPlaneswalkerDiedThisTurn condition above is one-or-more only, so a `>= N` count needs
    // the explicit Compare over the global creatures-died tracker:
    //   Conditions.CompareAmounts(DynamicAmounts.creaturesDiedThisTurn(Player.Each), GTE, Fixed(N)).
    // Player.Each sums every player's per-player tracker = a true global count, matching the IR's
    // unscoped "creatures died this turn". Only the `>= N` (fixed integer) comparison over a bare
    // creature-cardtype filter renders; any other comparator or a narrower filter (subtype, control)
    // declines -> SCAFFOLD rather than miscount.
    if (cond.strField("_Condition") == "ANumberOfPermanentsDiedThisTurn") {
        val condArgs = cond["args"].asArr ?: return null
        val comparison = condArgs.getOrNull(0) as? JsonObject ?: return null
        if (comparison.strField("_Comparison") != "GreaterThanOrEqualTo") return null
        val n = (comparison["args"] as? JsonObject)?.takeIf { it.strField("_GameNumber") == "Integer" }
            ?.get("args").asInt() ?: return null
        val filter = condArgs.getOrNull(1) as? JsonObject ?: return null
        val bareCreature = filter.strField("_Permanents") == "IsCardtype" &&
            filter.field("args").asStr() == "Creature" && filter.size == 2
        if (!bareCreature) return null
        return "Conditions.CompareAmounts(DynamicAmounts.creaturesDiedThisTurn(Player.Each), " +
            "ComparisonOperator.GTE, DynamicAmount.Fixed($n))"
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
    // "if you gained N or more life this turn" — PlayerPassesFilter(You, GainedLifeAmountThisTurn(
    // [Comparison GreaterThanOrEqualTo Integer N])) (Scheming Silvertongue's "if you gained 2 or more
    // life this turn") -> Conditions.YouGainedLifeThisTurnAtLeast(N). Only the >= comparison with a
    // fixed integer renders; any other comparator declines -> SCAFFOLD rather than miscount.
    youGainedLifeAtLeastConditionDsl(cond)?.let { return it }
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
    // "if you've cast two or more spells this turn" — PlayerPassesFilter(You,
    // CastNumSpellsThisTurn([GreaterThanOrEqualTo N], AnySpell)) (Loan Shark). Only the unrestricted
    // "any spell" count with a `>= N` comparison maps to Conditions.YouCastSpellsThisTurn(N); a
    // filtered spell set or any other comparison (exactly N, fewer than N) declines -> SCAFFOLD.
    youCastNumSpellsThisTurnDsl(cond)?.let { return it }
    // "if defending player controls no [filter]" — PlayerPassesFilter(Trigger_DefendingPlayer,
    // ControlsNo(<filter>)) (Fear of the Dark: "if defending player controls no Glimmer creatures").
    // The defending player controlling zero matching permanents is a count-equals-zero comparison over
    // their battlefield: Conditions.CompareAmounts(AggregateBattlefield(Player.DefendingPlayer, <filter>),
    // EQ, Fixed(0)). Player.DefendingPlayer resolves through the attacking source's combat assignment, so
    // this belongs on a `Whenever this creature attacks` (TriggerI) trigger. Only the bare ControlsNo over
    // a filter gameObjectFilterDsl can render exactly is recognised; anything else declines -> SCAFFOLD.
    defendingPlayerControlsNoDsl(cond)?.let { return it }
    // "you control another outlaw" — ControlsA over And(Other(ThatEnteringPermanent), IsAnOutlaw). The
    // entering permanent is itself an outlaw, so this is exactly "two or more outlaws you control".
    youControlAnotherOutlawDsl(cond)?.let { return it }
    // "if you control N or more <filter>" — PlayerPassesFilter(You, ControlsNum(>= N, <filter>)) ->
    // Conditions.YouControlAtLeast(N, <filter>) (Emeritus of Abundance's "if you control eight or more
    // lands" attack-trigger intervening-if). Reuses the same calibrated renderer the enters-with-counters
    // gate uses; only the `>= N` comparison over a filter that renders exactly produces a line.
    youControlAtLeastConditionDsl(cond)?.let { return it }
    // "you control a <filter>" — reuse the static-gate renderer (PlayerPassesFilter(You, ControlsA(filter))).
    youControlConditionDsl(cond)?.let { return it }
    // "if a card left your graveyard this turn" — ACardLeftPlayersGraveyardThisTurn(AnyCard, You)
    // (Primary Research's end-step draw). Only the unrestricted "a card" (AnyCard) over the You player
    // maps to Conditions.CardsLeftGraveyardThisTurn(1); a typed card filter or any other player scope
    // declines -> SCAFFOLD rather than over-fire on a narrower/different condition.
    if (cond.strField("_Condition") == "ACardLeftPlayersGraveyardThisTurn") {
        val condArgs = cond["args"].asArr ?: return null
        val cards = (condArgs.getOrNull(0) as? JsonObject)?.strField("_Cards")
        val player = (condArgs.getOrNull(1) as? JsonObject)?.strField("_Player")
        if (cards == "AnyCard" && player == "You") return "Conditions.CardsLeftGraveyardThisTurn(1)"
        return null
    }
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

/** `PlayerPassesFilter(You, CastNumSpellsThisTurn([Comparison GreaterThanOrEqualTo Integer N], AnySpell))`
 *  ("if you've cast N or more spells this turn") -> `Conditions.YouCastSpellsThisTurn(N)`, else null. Only
 *  the unrestricted `AnySpell` set with a `>= N` comparison renders — the spell being cast counts itself
 *  in the cast tracker by the time the enters trigger resolves, so "two or more spells this turn" is the
 *  literal threshold (Loan Shark). A filtered spell set, or any comparison other than `GreaterThanOrEqualTo`
 *  (exactly / fewer than), declines -> SCAFFOLD rather than miscount. */
private fun EmitCtx.youCastNumSpellsThisTurnDsl(cond: JsonObject): String? {
    if (cond.strField("_Condition") != "PlayerPassesFilter") return null
    val args = cond["args"].asArr ?: return null
    if ((args.getOrNull(0) as? JsonObject)?.strField("_Player") != "You") return null
    val cast = args.getOrNull(1) as? JsonObject ?: return null
    if (cast.strField("_Players") != "CastNumSpellsThisTurn") return null
    val castArgs = cast["args"].asArr ?: return null
    val comparison = castArgs.getOrNull(0) as? JsonObject ?: return null
    if (comparison.strField("_Comparison") != "GreaterThanOrEqualTo") return null
    val n = (comparison["args"] as? JsonObject)?.takeIf { it.strField("_GameNumber") == "Integer" }
        ?.get("args").asInt() ?: return null
    // Only the unrestricted "any spell" set renders; a filtered spell set declines rather than over/undercount.
    if ((castArgs.getOrNull(1) as? JsonObject)?.strField("_Spells") != "AnySpell") return null
    return "Conditions.YouCastSpellsThisTurn($n)"
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
    // "At the beginning of your second main phase" (You) — the postcombat main step trigger used by
    // the Survival ability word (Cautious Survivor, Veteran Survivor). Scoped exactly like upkeep/end-
    // step: only the You scope has a matching Triggers.* constant, so an any-player / opponent scope
    // declines -> SCAFFOLD.
    if (jsonContains(trig, "_Trigger", "AtTheBeginningOfAPlayersSecondMainPhase")) {
        if (jsonContains(trig, "_Player", "You")) return "Triggers.YourPostcombatMain"
    }
    // "your end step" is scoped to You (a SinglePlayer(You) subject); "each end step" to any player.
    // The opponent / host-relative end-step scopes have no matching Triggers.* constant yet, so they
    // decline -> SCAFFOLD, mirroring the upkeep block above (which has an EachOpponentUpkeep but no
    // end-step counterpart exists).
    if (jsonContains(trig, "_Trigger", "AtTheBeginningOfAPlayersEndStep")) {
        if (jsonContains(trig, "_Player", "You")) return "Triggers.YourEndStep"
        if (jsonContains(trig, "_Players", "AnyPlayer")) return "Triggers.EachEndStep"
    }

    // "At the beginning of combat on your turn" (You) / "on each turn" (any player) — the begin-of-
    // combat step trigger (Ornery Tumblewagg). Scoped exactly like the upkeep/end-step blocks; an
    // opponent / host-relative scope has no matching Triggers.* constant, so it declines -> SCAFFOLD.
    if (jsonContains(trig, "_Trigger", "AtTheBeginningOfCombatDuringAPlayersTurn")) {
        if (jsonContains(trig, "_Player", "You")) return "Triggers.BeginCombat"
        if (jsonContains(trig, "_Players", "AnyPlayer")) return "Triggers.EachCombat"
    }

    // "At the beginning of your first main phase" (You) — Abstract Paintmage's mana trigger. Only the
    // You scope has a matching Triggers.FirstMainPhase constant; any-player / opponent scopes decline
    // -> SCAFFOLD, mirroring the upkeep/end-step/combat blocks above.
    if (jsonContains(trig, "_Trigger", "AtTheBeginningOfAPlayersFirstMainPhase")) {
        if (jsonContains(trig, "_Player", "You")) return "Triggers.FirstMainPhase"
    }

    // "Whenever a player cycles a card" (any player) — Fleeting Aven, Invigorating Boon.
    if (jsonContains(trig, "_Trigger", "WhenAPlayerCyclesACard") && jsonContains(trig, "_Players", "AnyPlayer"))
        return "Triggers.AnyPlayerCycles"

    // "Whenever an opponent draws a card" (Razorkin Needlehead, Consecrated Sphinx) / "whenever you
    // draw a card" (A-Queza). The plain draw trigger fires once per card drawn (CR 121.2), with no
    // draw-step exemption — the sibling `WhenAPlayerDrawsACardExceptTheFirstCardDuringTheirDrawStep`
    // (Orcish Bowmasters) carries that exemption and is a *different* IR tag, so we never conflate
    // them. Only the Opponent and You scopes have matching Triggers.* constants; an AnyPlayer scope
    // (Ob Nixilis) has no plain constant yet, so it declines -> SCAFFOLD rather than guess a binding.
    if (jsonContains(trig, "_Trigger", "WhenAPlayerDrawsACard")) {
        val scope = trig["args"] as? JsonObject
        if (scope?.strField("_Players") == "Opponent") return "Triggers.OpponentDraws"
        if (scope?.strField("_Players") == "SinglePlayer" &&
            jsonContains(scope["args"], "_Player", "You")
        ) return "Triggers.YouDraw"
    }

    // "Whenever you gain life" (You) — Pest Mascot, Essence Channeler. Only the You scope maps to
    // Triggers.YouGainLife; an any-player / opponent scope has no calibrated card yet, so it
    // declines -> SCAFFOLD rather than guess a binding.
    if (jsonContains(trig, "_Trigger", "WhenAPlayerGainsLife") && jsonContains(trig, "_Player", "You"))
        return "Triggers.YouGainLife"

    // "Whenever you gain life for the first time each turn" (You) — Leech Collector. The IR tag bakes
    // in the once-per-turn semantics. Only the You scope renders.
    if (jsonContains(trig, "_Trigger", "WhenAPlayerGainsLifeForTheFirstTimeEachTurn") && jsonContains(trig, "_Player", "You"))
        return "Triggers.YouGainLifeFirstTimeEachTurn"

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

    // "Whenever equipped creature attacks" — an Equipment/Aura whose attack trigger is bound to the
    // permanent it's attached to (subject SinglePermanent(HostPermanent)). Maps to the ATTACHED binding
    // (Thunder Lasso, Heart-Piercer Bow). Only the bare host subject renders.
    if (jsonContains(trig, "_Trigger", "WhenACreatureAttacks") && isHost(trig))
        return "Triggers.attacks(binding = TriggerBinding.ATTACHED)"

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

    // "Whenever you attack with one or more creatures [matching a filter]" —
    // WhenAPlayerAttacksWithAnyNumberOfCreatures scoped to You. The args are [caster scope, attacker
    // filter]; the batched trigger fires once per combat when at least one declared attacker matches.
    // Maps to Triggers.YouAttackWithFilter(<filter>) (Jolene, Plundering Pugilist's "with power 4 or
    // greater"). Only the You scope renders; the attacker filter must round-trip exactly through
    // gameObjectFilterDsl (a shape it can't recover declines -> SCAFFOLD rather than widening the trigger).
    if (jsonContains(trig, "_Trigger", "WhenAPlayerAttacksWithAnyNumberOfCreatures")) {
        val argv = trig["args"].asArr ?: return null
        val scope = castScope(argv.getOrNull(0) as? JsonObject)
        if (scope != CastScope.YOU) return null
        val filter = gameObjectFilterDsl(argv.getOrNull(1)) ?: return null
        return "Triggers.YouAttackWithFilter($filter)"
    }

    // "Whenever you attack" — WhenAPlayerAttacks scoped to a SinglePlayer(You). The batched trigger
    // fires once per combat when you declare one or more attackers. Maps to Triggers.YouAttack
    // (Living History). Only the You scope renders; any other player scope has no calibrated
    // Triggers.* constant, so it declines -> SCAFFOLD rather than widening the trigger.
    if (jsonContains(trig, "_Trigger", "WhenAPlayerAttacks")) {
        val scope = castScope(trig["args"] as? JsonObject)
        if (scope != CastScope.YOU) return null
        return "Triggers.YouAttack"
    }

    // "Whenever you fully unlock a Room" — the Eerie Room half (CR 709.5h, Balemurk Leech, Optimistic
    // Scavenger). The args are [player scope, the Room subject]. The engine's Triggers.RoomFullyUnlocked
    // is fixed to the You scope over any Room, so only that exact shape renders: a SinglePlayer(You)
    // scope plus an IsEnchantmentType "Room" subject. Any other player scope, or a Room subject carrying
    // extra constraints, has no matching Triggers.* constant, so it declines -> SCAFFOLD rather than
    // widening the trigger.
    if (jsonContains(trig, "_Trigger", "WhenAPlayerFullyUnlocksARoom")) {
        val argv = trig["args"].asArr ?: return null
        val scope = castScope(argv.getOrNull(0) as? JsonObject)
        val subject = argv.getOrNull(1) as? JsonObject
        val bareRoomSubject = subject?.strField("_Permanents") == "IsEnchantmentType" &&
            subject.field("args").asStr() == "Room"
        if (scope == CastScope.YOU && bareRoomSubject) return "Triggers.RoomFullyUnlocked"
        return null
    }

    // "Whenever a [filtered] permanent enters the battlefield" (the SELF case returned above): an
    // `Other(ThisPermanent)` clause means "another …" -> OTHER binding (Elvish Vanguard's "another
    // Elf", Wretched Anurid's "another creature"); otherwise "a …" -> ANY (Wirewood Savage's "a Beast").
    if (jsonContains(trig, "_Trigger", "WhenAPermanentEntersTheBattlefield")) {
        val binding = if (jsonContains(trig, "_Permanents", "Other")) "TriggerBinding.OTHER" else "TriggerBinding.ANY"
        val filter = gameObjectFilterDsl(trig) ?: return null
        return "Triggers.entersBattlefield(filter = $filter, binding = $binding)"
    }

    // "Whenever equipped creature deals combat damage to a player" — an Equipment/Aura combat-damage
    // trigger bound to the host permanent (subject SinglePermanent(HostPermanent)). Maps to the
    // ATTACHED binding (The Key to the Vault). Only the bare host subject to any player renders.
    if (jsonContains(trig, "_Trigger", "WhenACreatureDealsCombatDamageToAPlayer") && isHost(trig) &&
        jsonContains(trig, "_Players", "AnyPlayer")
    ) {
        return "Triggers.dealsDamage(DamageType.Combat, RecipientFilter.AnyPlayer, " +
            "binding = TriggerBinding.ATTACHED)"
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
        // "Whenever a [filtered] creature you control deals combat damage to a player, …" — a
        // controller/supertype-scoped source filter beyond a bare subtype (Vraska Joins Up's
        // "legendary creature you control"). Recover the source filter via gameObjectFilterDsl, which
        // declines (-> SCAFFOLD) for any shape it can't round-trip exactly, so we never widen the
        // source. Only a creature filter renders — the trigger's source is always a creature here.
        val srcFilter = (trig["args"].asArr?.getOrNull(0) as? JsonObject)?.let { gameObjectFilterDsl(it) }
        if (srcFilter != null && srcFilter.startsWith("GameObjectFilter.Creature"))
            return "Triggers.dealsDamage(DamageType.Combat, RecipientFilter.AnyPlayer, " +
                "sourceFilter = $srcFilter, binding = TriggerBinding.ANY)"
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
        val n = comparison?.field("args").let { findInteger(it) } as? Int
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
        // "a spell with {X} in its mana cost" (Geometer's Arthropod, Matterbending Mage) — a bare
        // HasXInCost / HasXInManaCost filter (the IR uses both spellings interchangeably for the same
        // "{X} in its mana cost" predicate), expressed as a cast-time `SpellCastPredicate.HasXInCost`
        // (the X value of the triggering spell is read in the payoff via
        // DynamicAmounts.xValueOfTriggeringSpell()). Only the You scope has the matching Triggers facade
        // today; any other scope declines -> SCAFFOLD.
        if (spellsNode?.strField("_Spells") in setOf("HasXInCost", "HasXInManaCost")) {
            return if (scope == CastScope.YOU)
                "Triggers.youCastSpell(requires = setOf(SpellCastPredicate.HasXInCost))"
            else null
        }
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

    // "Whenever one or more +1/+1 counters are put on this creature, …" —
    // WhenAnyNumberOfCountersOfTypeArePutOnAPermanent. The args are [counter type, subject]; the
    // subject is the second arg (a SinglePermanent(ThisPermanent)), so isSelf — which inspects args[0]
    // — does not apply here. Render only the exact SELF + nameable counter-type shape as a
    // CountersPlacedEvent bound to SELF over an unfiltered permanent (the binding fixes it to this
    // permanent), matching Exemplar of Light / Pensive Professor. A non-self subject or an unnameable
    // counter declines -> SCAFFOLD. firstTimeEachTurn is NOT baked into this IR tag, so the once-each-
    // turn rider (Exemplar of Light) is carried separately by the TriggerOnceEachTurn envelope, not here.
    if (jsonContains(trig, "_Trigger", "WhenAnyNumberOfCountersOfTypeArePutOnAPermanent")) {
        val targs = trig["args"].asArr ?: return null
        val counter = counterTypeDsl(targs.getOrNull(0)) ?: return null
        val subject = targs.getOrNull(1) as? JsonObject
        val selfSubject = subject?.strField("_Permanents") == "SinglePermanent" &&
            subject.field("args").strField("_Permanent") == "ThisPermanent"
        if (!selfSubject) return null
        return "TriggerSpec(EventPattern.CountersPlacedEvent(counterType = $counter, " +
            "filter = GameObjectFilter.Any), TriggerBinding.SELF)"
    }

    // "Whenever one or more tokens you control enter, …" — the batched
    // WhenAnyNumberOfPermanentsEnterTheBattlefield (distinct from the singular
    // WhenAPermanentEntersTheBattlefield above). Fires once per enter batch. The controller scope is
    // carried by the trigger VARIANT (OneOrMorePermanentsEnter defaults to "you control";
    // OneOrMoreOpponentPermanentsEnter for opponents), so the rendered filter must NOT re-encode the
    // controller clause. Only the exact "token" subject renders today — the bare GameObjectFilter.Token
    // (Spiritcall Enthusiast's "tokens you control", Kambal's "tokens your opponents control"). Any other
    // subject filter declines -> SCAFFOLD rather than widening (gameObjectFilterDsl has no positive
    // IsToken rendering, so it would silently drop the token restriction).
    if (jsonContains(trig, "_Trigger", "WhenAnyNumberOfPermanentsEnterTheBattlefield")) {
        val subject = (trig["args"] as? JsonArray)?.firstOrNull() ?: trig["args"]
        val opponentControlled = jsonContains(subject, "_Players", "Opponent")
        // Strip the controller clause (it's conveyed by the variant) and require the remainder to be
        // exactly IsToken. The subject is either a bare IsToken or an And(IsToken, ControlledByAPlayer).
        val isTokenSubject = when {
            subject.strField("_Permanents") == "IsToken" -> true
            subject.strField("_Permanents") == "And" -> {
                val arms = subject.field("args").asArr?.filterIsInstance<JsonObject>() ?: return null
                // every arm must be either IsToken or a controller clause we recognise — nothing else.
                arms.all {
                    it.strField("_Permanents") == "IsToken" ||
                        it.strField("_Permanents") == "ControlledByAPlayer"
                } && arms.any { it.strField("_Permanents") == "IsToken" }
            }
            else -> false
        }
        if (!isTokenSubject) return null
        return if (opponentControlled)
            "Triggers.OneOrMoreOpponentPermanentsEnter(GameObjectFilter.Token)"
        else
            "Triggers.OneOrMorePermanentsEnter(GameObjectFilter.Token)"
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
    // "an outlaw spell" — a spell with one or more outlaw creature types (Double Down). Maps to the
    // shared Subtype.OUTLAW_TYPES group via withAnyOfSubtypes.
    "IsAnOutlaw" -> "outlaw"
    "IsCardtype" -> when (spells.field("args").asStr()) {
        "Creature" -> "creature"
        "Enchantment" -> "enchantment"
        else -> null
    }
    "IsNonCardtype" -> if (spells.field("args").asStr() == "Creature") "noncreature" else null
    // "a multicolored spell" (Mage Tower Referee). Maps to GameObjectFilter.Multicolored.
    "IsMulticolored" -> "multicolored"
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
    "outlaw" -> "GameObjectFilter.Any.withAnyOfSubtypes(Subtype.OUTLAW_TYPES)"
    "multicolored" -> "GameObjectFilter.Multicolored"
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
            "outlaw" -> "Triggers.youCastSpell(spellFilter = ${categoryFilter("outlaw")})"
            "multicolored" -> "Triggers.youCastSpell(spellFilter = ${categoryFilter("multicolored")})"
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
/**
 * Typecycling (CR 702.29) — the subtype/land-type cycling variants ("Forestcycling {2}",
 * "Slivercycling {3}", …). The IR is `[Cards IsLandType|IsCreatureType <Subtype>, Cost PayMana
 * {cost}]`. Renders `keywordAbility(KeywordAbility.typecycling("<Subtype>", ManaCost.parse("{cost}")))`,
 * the same builder hand-authored Forestcycling cards use (Wirewood Guardian, Slavering Branchsnapper).
 * Pure-mana cost only; a non-mana typecycling cost (none printed) declines -> scaffold.
 */
internal fun EmitCtx.typecyclingLine(rule: JsonObject): List<Stmt>? {
    val args = rule["args"] as? JsonArray ?: return null
    val cardsNode = args.getOrNull(0) as? JsonObject ?: return null
    // The subtype rides as `IsLandType <Type>` (Forest/Swamp/…) or `IsCreatureType <Type>` (Sliver/…).
    val subtype = when (cardsNode.strField("_Cards")) {
        "IsLandType", "IsCreatureType" -> cardsNode["args"].asStr() ?: return null
        else -> return null
    }
    val costNode = args.getOrNull(1) as? JsonObject ?: return null
    if (costNode.strField("_Cost") != "PayMana") return null
    val mana = renderMana(costNode.field("args"))
    if (mana.isEmpty()) return null
    return listOf(
        Eval(
            call(
                "keywordAbility",
                arg(
                    call(
                        "KeywordAbility.typecycling",
                        arg("\"$subtype\""),
                        arg(call("ManaCost.parse", arg("\"$mana\"")))
                    )
                )
            )
        )
    )
}

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

/** True when a trigger's subject is the *host* permanent — a direct SinglePermanent(HostPermanent)
 *  reference. This is the "equipped/enchanted creature" of an Equipment/Aura, mapped to the ATTACHED
 *  trigger binding. Mirrors [isSelf] but for the attached-to permanent. */
private fun isHost(trig: JsonObject): Boolean {
    val subject = (trig["args"] as? JsonArray)?.firstOrNull() ?: trig["args"]
    return subject.strField("_Permanents") == "SinglePermanent" &&
        subject.field("args").strField("_Permanent") == "HostPermanent"
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
            // The "slow land" cycle (Deathcap Glade, Dreamroot Cascade, Sundown Pass; VOW/SOS/INR):
            // "~ enters tapped unless you control two or more OTHER lands." The IR wraps the tapped
            // replacement in an `Unless{<condition>}[EntersTapped]`. Only the conservative
            // control-N-other-lands gate renders -> EntersTapped(unlessCondition = ...); any other
            // Unless condition declines (-> SCAFFOLD) rather than guess the gate. Note the parallel
            // "fast land" cycle ("two or FEWER other lands") uses a `<=` comparison this helper
            // deliberately doesn't render — the at-least condition facade only models `>=`.
            "Unless" -> {
                if (!onSelf) { reasons.add("AsPermanentEnters"); return null }
                // Two conservative enters-tapped gates render exactly: the "slow land"
                // control-N-other-lands threshold, and the DSK common dual-land
                // "unless a player has N or less life" existential life threshold. Any other
                // Unless condition declines (-> SCAFFOLD) rather than guess the gate.
                val cond = slowLandEntersTappedConditionDsl(rep)
                    ?: aPlayerLifeAtMostEntersTappedConditionDsl(rep)
                    ?: run { reasons.add("AsPermanentEnters"); return null }
                call("EntersTapped", arg("unlessCondition", Lit(cond)))
            }
            "ChooseACreatureType" -> call("EntersWithChoice", arg("ChoiceType.CREATURE_TYPE"))
            // "As ~ enters, choose a color." Only the unrestricted any-color choice (Mirage Mesa,
            // Uncharted Haven) renders exactly; a constrained color pick scaffolds. Pairs with the
            // `AddMana(ManaOfTheChosenColor)` -> `Effects.AddManaOfChosenColor()` mana ability.
            "ChooseAColor" -> {
                if ((rep["args"] as? JsonObject)?.strField("_ChoosableColor") != "AnyColor") {
                    reasons.add("AsPermanentEnters"); return null
                }
                call("EntersWithChoice", arg("ChoiceType.COLOR"))
            }
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
            // "~ enters with a +1/+1 counter on it IF <condition>" (Cackling Slasher) — the IR nests an
            // `If(<condition>, [<inner replacement(s)>])` inside the AsPermanentEnters replacement list.
            // Recurse over the inner replacement(s) with the condition threaded onto the EntersWithCounters
            // replacement, reusing every per-replacement renderer above. Only the conditions
            // [singleInterveningIfDsl] maps exactly render; anything else declines -> SCAFFOLD rather than
            // drop the gate. An outer condition combined with an inner If would need AND-composition we
            // don't model, so that declines too.
            "If" -> {
                if (condition != null) { reasons.add("AsPermanentEnters"); return null }
                val ifArgs = rep["args"].asArr ?: run { reasons.add("AsPermanentEnters"); return null }
                val cond = ifArgs.getOrNull(0) as? JsonObject ?: run { reasons.add("AsPermanentEnters"); return null }
                val innerReps = ifArgs.getOrNull(1) as? JsonArray ?: run { reasons.add("AsPermanentEnters"); return null }
                val condDsl = singleInterveningIfDsl(cond) ?: run { reasons.add("AsPermanentEnters"); return null }
                // Rebuild an AsPermanentEnters node carrying the inner replacements, preserving the same
                // scope permanent (first arg), and render it with the recovered condition.
                val scopePermanent = rule["args"].asArr?.getOrNull(0) as? JsonElement
                    ?: run { reasons.add("AsPermanentEnters"); return null }
                val synthetic = buildJsonObject {
                    put("_Rule", JsonPrimitive("AsPermanentEnters"))
                    put("args", JsonArray(listOf(scopePermanent, innerReps)))
                }
                val inner = asEntersBlock(synthetic, condition = condDsl) ?: return null
                stmts.addAll(inner)
                continue
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
    // Render the prepare spell in a child context with NO oracleText. The IR's `oracleText` is the
    // WHOLE two-faced card's Scryfall text, joined across the creature face and this prepare spell;
    // leaking it into the prepare spell's renderers makes the whole-card wording bleed into the
    // prepare spell's inference. E.g. a creature face that mentions "creatures died this turn" would
    // wrongly steer `landSearchFilterExpr`'s `"creature" in oracle` proxy to emit
    // GameObjectFilter.Creature for a Demonic Tutor that searches for *any* card. The prepare spell's
    // own characteristics come from its structured IR rules, not the shared oracle blob.
    val faceCtx = EmitCtx(keywords, oracleText = null)
    val spellStmts = faceCtx.spellBlock(prepared)
    reasons.addAll(faceCtx.reasons)
    if (spellStmts == null) { reasons.add("Prepared"); return null }
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
 * `FromGraveyardIf(condition, PlayerEffect(You, [MayCastGraveyardCardWithEnterActions(ThisGraveyardCard,
 *   [EntersWithACounter PTCounter[1,1]])]))` -> the conditional self-cast-from-graveyard static ability
 * plus the cast-this-way +1/+1-counter rider (Undead Sprinter, DSK).
 *
 * `FromGraveyardIf(ACreatureOrPlaneswalkerDiedThisTurn(And[IsNonCreatureType Zombie, IsCardtype Creature]),
 *   PlayerEffect(You, [MayCastGraveyardCardWithEnterActions(ThisGraveyardCard,
 *     [EntersWithACounter PTCounter[1,1]])]))`
 * →
 * ```
 * staticAbility {
 *     ability = MayCastSelfFromZones(
 *         zones = listOf(Zone.GRAVEYARD),
 *         condition = Conditions.NonSubtypeCreatureDiedThisTurn(Subtype.ZOMBIE))
 * }
 * replacementEffect(EntersWithCounters(count = 1, selfOnly = true, condition = Conditions.WasCastFromGraveyard))
 * ```
 *
 * "You may cast this card from your graveyard if [condition]. If you do, it enters with a +1/+1 counter."
 * The graveyard-cast permission is a gated [MayCastSelfFromZones] over `Zone.GRAVEYARD`; the "enter with a
 * counter" is the *cast-this-way* rider, modeled as a self-only [EntersWithCounters] gated on
 * `Conditions.WasCastFromGraveyard` (it applies only when this creature was cast from the graveyard, not
 * when cast normally from hand). Renders ONLY this exact shape — a `FromGraveyardIf` whose body is the
 * You-scoped `MayCastGraveyardCardWithEnterActions(ThisGraveyardCard, [+1/+1 enter counter])`, gated by a
 * died-this-turn condition the intervening-if recovery can name — and only when the +1/+1 counter is the
 * lone enter flag; anything else (a different enter action, a non-self graveyard card, an unrecoverable
 * condition) declines (-> SCAFFOLD).
 */
internal fun EmitCtx.fromGraveyardIfBlock(rule: JsonObject): List<Stmt>? {
    fun bail(): List<Stmt>? { reasons.add("FromGraveyardIf"); return null }
    val args = rule["args"].asArr ?: return bail()
    if (args.size != 2) return bail()
    // 1. The gate condition (e.g. "a non-Zombie creature died this turn"), via the shared intervening-if
    //    recovery — declines if the condition can't be named exactly (never a dropped/guessed gate).
    val condNode = args.getOrNull(0) as? JsonObject ?: return bail()
    val condition = singleInterveningIfDsl(condNode) ?: return bail()
    // 2. The body: PlayerEffect(You, [ MayCastGraveyardCardWithEnterActions(ThisGraveyardCard, [...]) ]).
    val player = args.getOrNull(1) as? JsonObject ?: return bail()
    if (player.strField("_Rule") != "PlayerEffect") return bail()
    val peArgs = player["args"].asArr ?: return bail()
    if (!jsonContains(peArgs.getOrNull(0), "_Player", "You")) return bail()
    val effects = (peArgs.getOrNull(1) as? JsonArray)?.filterIsInstance<JsonObject>() ?: return bail()
    val cast = effects.singleOrNull()
        ?.takeIf { it.strField("_PlayerEffect") == "MayCastGraveyardCardWithEnterActions" } ?: return bail()
    val castArgs = cast["args"].asArr ?: return bail()
    // The card cast must be THIS card from its own graveyard (a self-recursion permission).
    if (!jsonContains(castArgs.getOrNull(0), "_GraveyardCard", "ThisGraveyardCard")) return bail()
    // 3. The enter actions must be exactly one EntersWithACounter PTCounter[1,1] (+1/+1).
    val enterActions = (castArgs.getOrNull(1) as? JsonArray)?.filterIsInstance<JsonObject>() ?: return bail()
    val enterCounter = enterActions.singleOrNull()
        ?.takeIf { it.strField("_EnterFlag") == "EntersWithACounter" } ?: return bail()
    val ptCounter = enterCounter["args"] as? JsonObject
    val pt = ptCounter?.takeIf { it.strField("_CounterType") == "PTCounter" }?.get("args").asArr
    if (pt?.getOrNull(0).asInt() != 1 || pt?.getOrNull(1).asInt() != 1) return bail()

    val static = staticAbilityStmt(call(
        "MayCastSelfFromZones",
        arg("zones", call("listOf", arg("Zone.GRAVEYARD"))),
        arg("condition", Lit(condition)),
    ))
    val replacement = Eval(call(
        "replacementEffect",
        arg(call(
            "EntersWithCounters",
            arg("count", "1"),
            arg("selfOnly", "true"),
            arg("condition", Lit("Conditions.WasCastFromGraveyard")),
        )),
    ))
    return listOf(static, replacement)
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
    if ((rule["args"].asArr ?: emptyList()).filterIsInstance<JsonObject>().any { it.strField("_Actions") == "Modal_ChooseOne" }) {
        reasons.add("modal-activated")
        return null
    }
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
    // When the activation cost sacrifices/exiles the source, its counters are wiped before the
    // effect resolves; flag that so a "for each counter on this" count renders as last-known
    // information rather than reading the (now-zero) live count. Scoped to this ability's effect
    // rendering only.
    val prevSacrificed = sourceSacrificedByCost
    sourceSacrificedByCost = cost.contains("SacrificeSelf") || cost.contains("ExileSelf")
    val edsl = try {
        renderEffectList(actions, tvar) ?: return null
    } finally {
        sourceSacrificedByCost = prevSacrificed
    }
    val stmts = mutableListOf<Stmt>(Assign("cost", Lit(cost)))
    if (hasWaterbend) stmts.add(Assign("hasWaterbend", Lit("true")))
    activationRestrictionLines(rule)?.let { lines -> lines.forEach { stmts.add(RawLine(it)) } } ?: return null
    activationCostReductionLines(rule)?.let { lines -> lines.forEach { stmts.add(RawLine(it)) } } ?: return null
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
        // "Discard another card named ~" — the Grandeur cost (Secrets of Strixhaven — Page, Loose Leaf,
        // CR 207.2c). The mtgish filter is `IsNamed(NamedCard "<name>")`. The discard cost draws from the
        // controller's hand, where the source on the battlefield is never a candidate, so the printed
        // "another" is satisfied automatically by a bare name filter — `Costs.Discard(Any.named(<name>))`.
        // Only this exact bare-name shape renders; any other filter constraint declines (-> SCAFFOLD) so
        // we never approximate a richer discard cost as a plain name match.
        "DiscardACardOfType" -> {
            val filter = obj.field("args") as? JsonObject ?: return null
            if (filter.strField("_Cards") != "IsNamed") return null
            val name = filter.field("args").firstArgStringTagged("NamedCard") ?: return null
            "Costs.Discard(GameObjectFilter.Any.named(\"${ktStr(name)}\"))"
        }
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
            // "Sacrifice ANOTHER <permanent>" carries an `Other(ThisPermanent)` clause (Hungry Ghoul:
            // "Sacrifice another creature"); render Costs.SacrificeAnother (excludeSelf) so the source
            // can't pay by sacrificing itself. A bare "Sacrifice a <permanent>" stays Costs.Sacrifice.
            val another = jsonContains(obj.field("args"), "_Permanents", "Other") &&
                jsonContains(obj.field("args"), "_Permanent", "ThisPermanent")
            when {
                another && it == "GameObjectFilter.Any" -> "Costs.SacrificeAnother()"
                another -> "Costs.SacrificeAnother($it)"
                it == "GameObjectFilter.Any" -> "Costs.Sacrifice()"
                else -> "Costs.Sacrifice($it)"
            }
        }
        // "Sacrifice N <permanents>" as an activation cost (Magda, the Hoardmaster's "Sacrifice three
        // Treasures"). IR args are [<N Integer>, <permanent filter>]. Maps to Costs.SacrificeMultiple(N,
        // <filter>); only a FIXED integer count and an exactly-recoverable filter render (a dynamic count
        // or an unrenderable filter declines -> SCAFFOLD rather than guessing).
        "SacrificeNumberPermanents" -> {
            val a = obj["args"].asArr ?: return null
            val n = findInteger(a.getOrNull(0)) as? Int ?: return null
            val filter = costFilterDsl(a.getOrNull(1)) ?: return null
            if (filter == "GameObjectFilter.Any") "Costs.SacrificeMultiple($n)"
            else "Costs.SacrificeMultiple($n, $filter)"
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
        // "Remove N <type> counters from this permanent" as an activation cost (Bandit's Haul). IR args
        // are [<N Integer>, <CounterType>, <Permanent ThisPermanent>]. Only the self-subject and a
        // nameable counter kind render via Costs.RemoveCounterFromSelf; any other subject or counter
        // declines (-> SCAFFOLD) rather than guess.
        "RemoveNumberCountersOfTypeFromPermanent" -> {
            val a = obj["args"].asArr ?: return null
            val n = findInteger(a.getOrNull(0)) as? Int ?: return null
            if (a.getOrNull(2).strField("_Permanent") != "ThisPermanent") return null
            val counter = counterTypeDsl(a.getOrNull(1)) ?: return null
            "Costs.RemoveCounterFromSelf($counter, $n)"
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
        // ReduceCostForEach is a generic-cost reduction, emitted as `genericCostReduction = …`
        // (see [activationCostReductionLines]), not an ActivationRestriction. Drop it here so an
        // ability whose only non-timing modifier is a cost reduction needs no `restrictions =` line.
        .filter { it.strField("_ActivateModifier") != "ReduceCostForEach" }
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
    // "Activate only if you have seven or more cards in your hand" (Resonating Lute): the sole modifier
    // is an ActivateOnlyIf whose condition is "you have >= N cards in hand" ->
    // ActivationRestriction.OnlyIfCondition(Conditions.CardsInHandAtLeast(N)). Match the precise shape
    // (the You player + NumCardsInHandIs GreaterThanOrEqualTo Integer N) so any other comparator or
    // player still scaffolds.
    handSizeAtLeastModifier(rule)?.let { n ->
        return listOf(
            "        restrictions = listOf(ActivationRestriction.OnlyIfCondition(Conditions.CardsInHandAtLeast($n)))",
        )
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
    // "Activate only if you've cast an instant or sorcery spell this turn" (Potioner's Trove): the
    // sole modifier is an ActivateOnlyIf whose condition is PlayerPassesFilter(You,
    // CastASpellThisTurn(Or(IsCardtype Instant, IsCardtype Sorcery))) ->
    // ActivationRestriction.OnlyIfCondition(Conditions.YouCastSpellsThisTurn(1,
    // GameObjectFilter.InstantOrSorcery)). Match the exact shape (You + the instant-or-sorcery spell
    // filter) so any other player, count, or spell filter still scaffolds.
    run {
        val mods = (rule["args"].asArr ?: emptyList()).filterIsInstance<JsonObject>()
            .filter { it.strField("_ActivateModifier") != null }
        val only = mods.singleOrNull()
        if (only != null && only.strField("_ActivateModifier") == "ActivateOnlyIf") {
            val cond = only["args"] as? JsonObject
            if (cond?.strField("_Condition") == "PlayerPassesFilter") {
                val cArgs = cond["args"].asArr ?: emptyList()
                val player = cArgs.getOrNull(0) as? JsonObject
                val playerFilter = cArgs.getOrNull(1) as? JsonObject
                if (jsonContains(player, "_Player", "You") &&
                    playerFilter?.strField("_Players") == "CastASpellThisTurn" &&
                    isInstantOrSorcerySpellFilter(playerFilter["args"] as? JsonObject)
                ) {
                    return listOf(
                        "        restrictions = listOf(",
                        "            ActivationRestriction.OnlyIfCondition(",
                        "                Conditions.YouCastSpellsThisTurn(",
                        "                    atLeast = 1,",
                        "                    filter = GameObjectFilter.InstantOrSorcery,",
                        "                ),",
                        "            ),",
                        "        )",
                    )
                }
            }
        }
    }
    // Per-turn activation caps -> ActivationRestriction frequency rules. Render only when EVERY
    // modifier present is a recognised per-turn cap, so an unrecognised modifier still scaffolds:
    //  - "Activate only once each turn"  (Mindful Biomancer)              -> OncePerTurn
    //  - "Activate no more than N times each turn" (Pit Imp / Phyrexian Battleflies) -> MaxPerTurn(N)
    val modifiers = (rule["args"].asArr ?: emptyList()).filterIsInstance<JsonObject>()
        .filter { it.strField("_ActivateModifier") != null }
    val perTurnCaps = setOf("ActivateOnlyOnceEachTurn", "ActivateNoMoreThanNumberTimesEachTurn")
    if (modifiers.isNotEmpty() && modifiers.all { it.strField("_ActivateModifier") in perTurnCaps }) {
        val lines = modifiers.map { mod ->
            when (mod.strField("_ActivateModifier")) {
                "ActivateOnlyOnceEachTurn" -> "ActivationRestriction.OncePerTurn"
                else -> {
                    val n = findInteger(mod.field("args")) as? Int ?: return run { reasons.add("activated-modifiers"); null }
                    "ActivationRestriction.MaxPerTurn($n)"
                }
            }
        }
        return listOf("        restrictions = listOf(${lines.joinToString(", ")})")
    }
    reasons.add("activated-modifiers")
    return null
}

/**
 * The `genericCostReduction = …` line for an `ActivatedWithModifiers` rule carrying a `ReduceCostForEach`
 * modifier — "This ability costs {1} less to activate for each <counter> counter on this artifact" (Diary
 * of Dreams). Returns an empty list when no such modifier is present, the assignment line when the exact
 * shape renders, or null (-> SCAFFOLD) when a `ReduceCostForEach` exists in an unrenderable shape.
 *
 * The only shape rendered is the per-counter self-reduction: a single `CostReduceGeneric 1` reduction
 * scaled by `NumCountersOfTypeOnPermanent(<named passive counter>, ThisPermanent)`, which maps to
 * `genericCostReduction = DynamicAmounts.countersOnSelf(CounterTypeFilter.Named(Counters.X))`. Any other
 * reduction symbol, amount, game-number source, or subject declines rather than guess.
 */
private fun EmitCtx.activationCostReductionLines(rule: JsonObject): List<String>? {
    if (rule.strField("_Rule") != "ActivatedWithModifiers") return emptyList()
    val reductions = activatedModifiers(rule).filter { it.strField("_ActivateModifier") == "ReduceCostForEach" }
    if (reductions.isEmpty()) return emptyList()
    val reduction = reductions.singleOrNull() ?: run { reasons.add("activated-modifiers"); return null }
    val rArgs = reduction["args"].asArr ?: run { reasons.add("activated-modifiers"); return null }
    // First arg: a list with exactly one CostReduceGeneric of amount 1 (each counter shaves {1}).
    val symbols = (rArgs.getOrNull(0) as? JsonArray)?.filterIsInstance<JsonObject>()
        ?: run { reasons.add("activated-modifiers"); return null }
    val genericSymbol = symbols.singleOrNull()?.takeIf { it.strField("_CostReductionSymbol") == "CostReduceGeneric" }
        ?: run { reasons.add("activated-modifiers"); return null }
    // The symbol's args is a bare integer (the {N} shaved per counter); only the {1}-per-counter
    // shape ("costs {1} less … for each counter") renders.
    if (genericSymbol.field("args").asInt() != 1) { reasons.add("activated-modifiers"); return null }
    // Second arg: NumCountersOfTypeOnPermanent(<counter>, ThisPermanent).
    val gameNumber = rArgs.getOrNull(1) as? JsonObject ?: run { reasons.add("activated-modifiers"); return null }
    if (gameNumber.strField("_GameNumber") != "NumCountersOfTypeOnPermanent") { reasons.add("activated-modifiers"); return null }
    val gnArgs = gameNumber["args"].asArr ?: run { reasons.add("activated-modifiers"); return null }
    val constant = passiveCounterConstant(gnArgs.getOrNull(0)) ?: run { reasons.add("activated-modifiers"); return null }
    if (!jsonContains(gnArgs.getOrNull(1), "_Permanent", "ThisPermanent")) { reasons.add("activated-modifiers"); return null }
    return listOf(
        "        genericCostReduction = DynamicAmounts.countersOnSelf(CounterTypeFilter.Named($constant))",
    )
}

/** A mtgish `_CounterType` node for a passive storage counter -> the `Counters.*` string constant the
 *  count-reading sites (`DynamicAmounts.countersOnSelf(CounterTypeFilter.Named(...))`) take, or null for a
 *  counter kind we don't name. Mirrors the passive-counter rows in [counterTypeDsl]. */
private fun passiveCounterConstant(counterNode: JsonElement?): String? =
    when ((counterNode as? JsonObject)?.strField("_CounterType")) {
        "LootCounter" -> "Counters.LOOT"
        "GrowthCounter" -> "Counters.GROWTH"
        "NestCounter" -> "Counters.NEST"
        "PageCounter" -> "Counters.PAGE"
        else -> null
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

/** The threshold N when the rule's only `_ActivateModifier` is an `ActivateOnlyIf` gating on "you have
 *  N or more cards in hand" (`NumCardsInHandIs(GreaterThanOrEqualTo, Integer N)` over the You player),
 *  else null. Guards `CardsInHandAtLeast` so a different comparator (less-than, exactly), a different
 *  player, an X count, or an extra modifier still scaffolds. */
private fun EmitCtx.handSizeAtLeastModifier(rule: JsonObject): Int? {
    val modifiers = (rule["args"].asArr ?: emptyList()).filterIsInstance<JsonObject>()
        .filter { it.strField("_ActivateModifier") != null }
    val mod = modifiers.singleOrNull() ?: return null
    if (mod.strField("_ActivateModifier") != "ActivateOnlyIf") return null
    if (!jsonContains(mod, "_Player", "You")) return null
    if (!jsonContains(mod, "_Players", "NumCardsInHandIs")) return null
    if (!jsonContains(mod, "_Comparison", "GreaterThanOrEqualTo")) return null
    return findInteger(mod.field("args")) as? Int
}

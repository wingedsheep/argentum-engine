package com.wingedsheep.tooling.coverage.emitter

import com.wingedsheep.tooling.coverage.Assign
import com.wingedsheep.tooling.coverage.Block
import com.wingedsheep.tooling.coverage.Call
import com.wingedsheep.tooling.coverage.Dsl
import com.wingedsheep.tooling.coverage.Eval
import com.wingedsheep.tooling.coverage.Lit
import com.wingedsheep.tooling.coverage.Stmt
import com.wingedsheep.tooling.coverage.Sub
import com.wingedsheep.tooling.coverage.arg
import com.wingedsheep.tooling.coverage.asInt
import com.wingedsheep.tooling.coverage.asStr
import com.wingedsheep.tooling.coverage.call
import com.wingedsheep.tooling.coverage.compact
import com.wingedsheep.tooling.coverage.dot
import com.wingedsheep.tooling.coverage.firstArgWordTagged
import com.wingedsheep.tooling.coverage.firstWordAtKey
import com.wingedsheep.tooling.coverage.hasTag
import com.wingedsheep.tooling.coverage.jsonContains
import com.wingedsheep.tooling.coverage.nodesTagged
import com.wingedsheep.tooling.coverage.pascalToUpperSnake
import com.wingedsheep.tooling.coverage.strField
import com.wingedsheep.tooling.coverage.subtypes
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/** `staticAbility { ability = <ability> }` as one card-body statement. */
internal fun staticAbilityStmt(ability: Dsl): Stmt = Sub(Block("staticAbility", listOf(Assign("ability", ability))))

/** `staticAbility { condition = <cond>; ability = <ability> }` — a threshold-gated static ability row. */
private fun gatedStaticAbilityStmt(cond: String, ability: Dsl): Stmt =
    Sub(Block("staticAbility", listOf(Assign("condition", Lit(cond)), Assign("ability", ability))))

/**
 * Station `{N+}[abilities][P/T]` symbol that animates the permanent into a creature (CR 721.2b,
 * mtgish `StationChargedAnimate`): "As long as this permanent has N or more charge counters, it has
 * [abilities] and is a creature with base power/toughness [P/T]." Renders one threshold-gated
 * `staticAbility { }` row per granted ability — `GrantCardType("CREATURE", …)` for the animate, plus a
 * `GrantKeyword(...)` per listed keyword — each gated on
 * `Conditions.SourceCounterCountAtLeast(Counters.CHARGE, N)`. The base P/T (args[2]) is the card's
 * printed power/toughness, already emitted on the card, so it needs no separate row.
 *
 * Only *bare keyword* abilities render. A threshold that grants a triggered or activated ability (or any
 * parameterized ability) declines to a scaffold — "decline→SCAFFOLD, don't widen". The sibling
 * `StationCharged` symbol (a non-animating threshold gating an activated/triggered ability) is likewise
 * left to scaffold via the dispatcher's default branch.
 */
internal fun EmitCtx.stationAnimateBlock(rule: JsonObject): List<Stmt>? {
    val args = rule["args"] as? JsonArray ?: return scaffoldStation()
    // The threshold N rides as a raw integer in the `ValueOrBigger` range node's args (`{N+}` symbol).
    val n = (args.getOrNull(0) as? JsonObject)
        ?.takeIf { it.strField("_GameRange") == "ValueOrBigger" }
        ?.get("args").asInt() ?: return scaffoldStation()
    val abilityRules = (args.getOrNull(1) as? JsonArray)?.filterIsInstance<JsonObject>() ?: emptyList()
    val cond = "Conditions.SourceCounterCountAtLeast(Counters.CHARGE, $n)"
    val stmts = mutableListOf<Stmt>()
    stmts.add(gatedStaticAbilityStmt(cond, call("GrantCardType", arg("\"CREATURE\""), arg("GroupFilter.source()"))))
    for (ar in abilityRules) {
        val rn = ar.strField("_Rule") ?: return scaffoldStation()
        if (ar["args"] != null) return scaffoldStation() // parameterized / non-keyword ability
        val kw = pascalToUpperSnake(rn).takeIf { it in keywords } ?: return scaffoldStation()
        stmts.add(gatedStaticAbilityStmt(cond, call("GrantKeyword", arg("Keyword.$kw.name"), arg("GroupFilter.source()"))))
    }
    return stmts
}

private fun EmitCtx.scaffoldStation(): List<Stmt>? { reasons.add("StationChargedAnimate"); return null }

/**
 * PermanentRuleEffect → `flags()` / `staticAbility { ability = ... }`. These classes live outside the
 * effects registry, so the capability gate is vacuous for them; the generated static is best-effort
 * and (like every draft) flagged for rules-text review.
 */
internal fun EmitCtx.staticBlock(rule: JsonObject): List<Stmt>? {
    val rules = mutableListOf<JsonObject>()
    fun collect(n: JsonElement?) {
        when (n) {
            is JsonObject -> { if (n.strField("_PermanentRule") != null) rules.add(n); n.values.forEach { collect(it) } }
            is JsonArray -> n.forEach { collect(it) }
            else -> {}
        }
    }
    collect(rule)
    if (rules.isEmpty()) { reasons.add("PermanentRuleEffect"); return null }
    // "Enchanted creature attacks each combat if able" (Furor of the Bitten): the rule's subject is the
    // aura's HostPermanent, not the card itself. The self-scoped renders below (flags(), the default
    // GroupFilter.source() statics) would apply the rule to the AURA — a silent no-op. Render the
    // attached-creature scope for the rules whose StaticAbility takes a GroupFilter; decline the rest.
    val hostSubject = jsonContains((rule["args"] as? JsonArray)?.getOrNull(0), "_Permanent", "HostPermanent")
    if (hostSubject) {
        val stmts = mutableListOf<Stmt>()
        for (r in rules) {
            val ability = when (r.strField("_PermanentRule")!!) {
                "MustAttack" -> call("MustAttack", arg(call("GroupFilter.attachedCreature")))
                "CantBlock" -> call("CantBlock", arg(call("GroupFilter.attachedCreature")))
                else -> { reasons.add("PermanentRuleEffect"); return null }
            }
            stmts.add(staticAbilityStmt(ability))
        }
        return stmts
    }
    val stmts = mutableListOf<Stmt>()
    for (r in rules) {
        val name = r.strField("_PermanentRule")!!
        if (name == "CantBeBlocked") {
            stmts.add(Eval(call("flags", arg("AbilityFlag.CANT_BE_BLOCKED")))); continue
        }
        if (name == "MayChooseNotToUntapDuringUntap") {
            stmts.add(Eval(call("flags", arg("AbilityFlag.MAY_NOT_UNTAP")))); continue
        }
        val ability = staticAbilityExpr(name, r) ?: run { reasons.add(name); return null }
        stmts.add(staticAbilityStmt(ability))
    }
    return stmts
}

/**
 * A static `EachPermanentLayerEffect` "lord" rule -> one `staticAbility { ability = ... }` per static
 * layer effect: AdjustPT -> `ModifyStats(powerBonus, toughnessBonus, filter)`, AddAbility{kw} ->
 * `GrantKeyword(Keyword.X, filter)`. The affected group is a GroupFilter (a fixed creature subtype with
 * excludeSelf for "other …", or `GroupFilter.ChosenSubtypeCreatures()` for "creatures of the chosen
 * type"). Anything we can't render exactly scaffolds.
 */
internal fun EmitCtx.staticLordBlock(rule: JsonObject): List<Stmt>? {
    val args = rule["args"] as? JsonArray
    val layerEffects = (args?.getOrNull(1) as? JsonArray)?.filterIsInstance<JsonObject>()
    if (args == null || layerEffects.isNullOrEmpty()) { reasons.add("EachPermanentLayerEffect"); return null }
    val group = lordGroupFilterExpr(args.getOrNull(0)) ?: run { reasons.add("EachPermanentLayerEffect"); return null }
    val stmts = mutableListOf<Stmt>()
    for (le in layerEffects) {
        val ability: Dsl = when (le.strField("_StaticLayerEffect")) {
            "AdjustPT" -> {
                val pt = le["args"] as? JsonArray ?: return scaffoldLord()
                if (pt.size != 2) return scaffoldLord()
                call("ModifyStats", arg("powerBonus", "${pt[0].asInt()}"), arg("toughnessBonus", "${pt[1].asInt()}"), arg("filter", group))
            }
            "AddAbility" -> {
                val granted = (le["args"] as? JsonArray)?.getOrNull(0) as? JsonObject
                // "All Slivers have '{cost}: …'" (the Tempest sliver cycle, Crypt/Magma/Spectral Sliver):
                // the granted ability is a full activated ability, rendered as GrantActivatedAbility over
                // the affected group. Anything the inner ability can't render exactly scaffolds.
                if (granted?.strField("_Rule") in setOf("Activated", "ActivatedWithModifiers")) {
                    val inner = grantedActivatedAbilityExpr(granted!!) ?: return scaffoldLord()
                    call("GrantActivatedAbility", arg("ability", inner), arg("filter", group))
                } else {
                    // "Other Merfolk have islandwalk" (Lord of Atlantis / Goblin King): the granted ability is a
                    // Landwalk rule carrying a land subtype, not a plain keyword — recover the *WALK keyword.
                    val kw = if (granted?.strField("_Rule") == "Landwalk") {
                        val lw = mutableSetOf<String>()
                        findLandwalkKeywords(granted, keywords, lw)
                        lw.singleOrNull() ?: return scaffoldLord()
                    } else {
                        keywordOf(le) ?: return scaffoldLord()
                    }
                    // "Other creatures you control have prowess" — prowess is a triggered-ability keyword
                    // the engine derives from an explicit +1/+1 trigger, so a GrantKeyword grant would add
                    // the display tag but never the pump. Granting it faithfully needs a GrantTriggeredAbility
                    // of the prowess trigger, which this generic AddAbility path doesn't model — decline to a
                    // scaffold (per "decline→SCAFFOLD, don't widen") rather than emit a confidently-wrong lord.
                    if (kw == "PROWESS") return scaffoldLord()
                    call("GrantKeyword", arg("Keyword.$kw"), arg(group))
                }
            }
            else -> return scaffoldLord()
        }
        stmts.add(staticAbilityStmt(ability))
    }
    return stmts
}

private fun EmitCtx.scaffoldLord(): List<Stmt>? { reasons.add("EachPermanentLayerEffect"); return null }

/**
 * A self-buff `PermanentLayerEffect(ThisPermanent, [AdjustPTForEach])` -> one
 * `staticAbility { ability = GrantDynamicStatsEffect(filter = GroupFilter.source(), powerBonus = …,
 * toughnessBonus = …) }`. `AdjustPTForEach`'s args are `[powerMult, toughnessMult, countNode]`:
 * "this creature gets +powerMult/+toughnessMult for each [countNode]". The per-permanent count is
 * rendered as a resolution-time `DynamicAmount.Count` over the You battlefield with the recovered
 * filter (matching the `Count(Player.You, Zone.BATTLEFIELD, …)` convention used by hand-authored
 * "for each [type] you control" cards, e.g. Desert's Due). A multiplier other than 1 wraps the count
 * in `DynamicAmount.Multiply`.
 *
 * Only the You-controlled-battlefield count shape renders; any other count scope, a non-AdjustPTForEach
 * layer effect, or a filter the count path can't express exactly returns null so the card scaffolds
 * rather than emit a wrong buff. ("Crusading Knight" — Swamps your *opponents* control — therefore still
 * scaffolds here; the You case is the common Outlaws Desert pattern.)
 */
private fun EmitCtx.selfDynamicStatsBlock(rule: JsonObject): List<Stmt>? {
    val args = rule["args"] as? JsonArray ?: return null
    val layerEffects = (args.getOrNull(1) as? JsonArray)?.filterIsInstance<JsonObject>() ?: return null
    if (layerEffects.isEmpty()) return null
    val stmts = mutableListOf<Stmt>()
    for (le in layerEffects) {
        if (le.strField("_StaticLayerEffect") != "AdjustPTForEach") return null
        val pt = le["args"] as? JsonArray ?: return null
        if (pt.size != 3) return null
        val powerMult = pt[0].asInt() ?: return null
        val toughnessMult = pt[1].asInt() ?: return null
        // The count must be a You-controlled battlefield tally; decline anything else (opponent /
        // each-player scope, or a filter the count path widens) so we never misrender the buff.
        val countNode = pt[2] as? JsonObject ?: return null
        if (countNode.strField("_GameNumber") != "TheNumberOfPermanentsOnTheBattlefield") return null
        if (!jsonContains(countNode, "_Player", "You")) return null
        // Reject the predicates the land/type count filter path can't render faithfully (it silently
        // widens to GameObjectFilter.Any) — mirror dynamicAmountExpr's guards.
        val blob = compact(countNode)
        if ("IsArtifactType" in blob || "SharesACreatureTypeWithPermanent" in blob) return null
        if (countNode.firstArgWordTagged("IsEnchantmentType") != null &&
            countNode.firstArgWordTagged("IsCreatureType") == null) return null
        val subtype = countNode.firstArgWordTagged("IsCreatureType")
        val filter = if (subtype != null) Lit("GameObjectFilter.Creature").dot("withSubtype", arg("\"$subtype\""))
                     else landSearchFilterExpr(countNode)
        val count: Dsl = call("DynamicAmount.Count", arg("Player.You"), arg("Zone.BATTLEFIELD"), arg(filter))
        fun bonus(mult: Int): Dsl =
            if (mult == 1) count else call("DynamicAmount.Multiply", arg(count), arg("$mult"))
        stmts.add(
            staticAbilityStmt(
                call(
                    "GrantDynamicStatsEffect",
                    arg("filter", call("GroupFilter.source")),
                    arg("powerBonus", bonus(powerMult)),
                    arg("toughnessBonus", bonus(toughnessMult)),
                )
            )
        )
    }
    return stmts
}

/**
 * An `Activated` / `ActivatedWithModifiers` rule granted to a group ("All Slivers have '{cost}: …'") ->
 * an `ActivatedAbility(id = AbilityId.generate(), cost = …, [timing = …], effect = …, [targetRequirement
 * = …])` constructor expression for wrapping in `GrantActivatedAbility`. Reuses the same cost / target /
 * effect recovery as the card-body [activatedBlock], but in expression form: a chosen target becomes
 * `targetRequirement = <node>` and the effect references `EffectTarget.ContextTarget(0)` (the granted
 * ability has no card-body `target(...)` local to bind). The only activation modifier rendered is
 * `ActivateOnlyAsASorcery` -> `timing = TimingRule.SorcerySpeed`; any other modifier scaffolds.
 */
internal fun EmitCtx.grantedActivatedAbilityExpr(rule: JsonObject): Dsl? {
    val costNode = (rule["args"] as? JsonArray)?.firstOrNull() as? JsonObject
    val cost = costNode?.let { abilityCostDsl(it) } ?: return null
    val (targets, actions) = extractEnvelope(rule)
    if (actions == null) return null
    if (targets != null && targets.size > 1) return null
    // A chosen target becomes the ability's targetRequirement; the effect then refers to it via
    // ContextTarget(0) (the granted ability has no bound `t` local).
    val targetNode = targets?.firstOrNull()?.let { targetExpr(it, actions) ?: return null }
    val tvar = if (targetNode != null) "EffectTarget.ContextTarget(0)" else null
    val effect = renderEffectList(actions, tvar) ?: return null
    val timing = grantedActivationTiming(rule) ?: return null

    val args = mutableListOf(
        arg("id", "AbilityId.generate()"),
        arg("cost", cost),
    )
    if (timing.isNotEmpty()) args.add(arg("timing", timing))
    args.add(arg("effect", effect))
    if (targetNode != null) args.add(arg("targetRequirement", targetNode))
    return Call("ActivatedAbility", args)
}

/** The `timing = …` value for a granted activated ability: "" (omit, default instant speed) for a plain
 *  `Activated` rule, `TimingRule.SorcerySpeed` for an `ActivatedWithModifiers` carrying ONLY the
 *  `ActivateOnlyAsASorcery` modifier (Mindwhip Sliver); null (-> SCAFFOLD) for any other modifier. */
private fun EmitCtx.grantedActivationTiming(rule: JsonObject): String? {
    if (rule.strField("_Rule") != "ActivatedWithModifiers") return ""
    val modifiers = (rule["args"] as? JsonArray).orEmpty()
        .filterIsInstance<JsonObject>().filter { it.strField("_ActivateModifier") != null }
    if (modifiers.isEmpty()) return ""
    if (modifiers.all { it.strField("_ActivateModifier") == "ActivateOnlyAsASorcery" }) return "TimingRule.SorcerySpeed"
    return null
}

/**
 * `EnchantPermanent` -> the card-level `auraTarget = Targets.X` line. The enchant restriction is a
 * cardtype filter ("Enchant creature / land / artifact / enchantment"); anything more specific than a
 * bare cardtype (e.g. "enchant tapped creature") scaffolds rather than emit an inexact restriction.
 */
internal fun EmitCtx.auraTargetBlock(rule: JsonObject): List<Stmt>? {
    val filter = rule["args"] as? JsonObject ?: run { reasons.add("EnchantPermanent"); return null }
    // "Enchant creature / land / artifact / enchantment": a single card-type restriction.
    if (filter.strField("_Permanents") == "IsCardtype") {
        val target = when (filter["args"].asStr()) {
            "Creature" -> "Targets.Creature"
            "Land" -> "Targets.Land"
            "Artifact" -> "Targets.Artifact"
            "Enchantment" -> "Targets.Enchantment"
            else -> { reasons.add("EnchantPermanent"); return null }
        }
        return listOf(Assign("auraTarget", Lit(target)))
    }
    // "Enchant artifact or creature you control" (Moonlit Meditation): a card-type Or AND-ed with a
    // you-control restriction. Only shapes with an exact SDK TargetFilter render; any other combination
    // declines to a scaffold.
    auraYouControlTarget(filter)?.let { return listOf(Assign("auraTarget", Lit(it))) }
    reasons.add("EnchantPermanent")
    return null
}

/** `And(Or(<types>), ControlledBy You)` -> a you-control `TargetPermanent(...)` expr, or null
 *  (-> SCAFFOLD). Currently only "artifact or creature you control" maps to an exact SDK filter. */
private fun auraYouControlTarget(filter: JsonObject): String? {
    if (filter.strField("_Permanents") != "And") return null
    val args = (filter["args"] as? JsonArray)?.filterIsInstance<JsonObject>() ?: return null
    if (args.size != 2) return null
    if (args.none { jsonContains(it, "_Permanents", "ControlledByAPlayer") && jsonContains(it, "_Player", "You") }) return null
    val typeNode = args.firstOrNull { it.strField("_Permanents") == "Or" } ?: return null
    val types = (typeNode["args"] as? JsonArray)
        ?.mapNotNull { (it as? JsonObject)?.takeIf { o -> o.strField("_Permanents") == "IsCardtype" }?.get("args").asStr() }
        ?.toSet() ?: return null
    return when (types) {
        setOf("Artifact", "Creature") -> "TargetPermanent(TargetFilter.CreatureOrArtifact.youControl())"
        else -> null
    }
}

/**
 * A static `PermanentLayerEffect` whose target is the aura's `HostPermanent` (the enchanted permanent)
 * -> one `staticAbility { ability = ... }` per layer effect, applied to the enchanted permanent (no
 * filter, the aura-static default): AdjustPT -> `ModifyStats(p, t)`, AddAbility{kw} ->
 * `GrantKeyword(Keyword.X)`, AddAbility{protection-from-color} -> `GrantProtection(Color.X)` (the Ward
 * cycle). A layer effect we can't render exactly scaffolds.
 */
internal fun EmitCtx.staticHostBlock(rule: JsonObject): List<Stmt>? {
    val args = rule["args"] as? JsonArray
    // "This creature gets +X/+Y for each [permanents you control]" (Outcaster Greenblade, Crusading
    // Knight): a self-targeting layer effect whose subject is ThisPermanent, not an aura's host. Route
    // to the dynamic self-buff renderer; only the AdjustPTForEach shape renders there, everything else
    // falls through to the scaffold below.
    if (jsonContains(args?.getOrNull(0), "_Permanent", "ThisPermanent")) {
        selfDynamicStatsBlock(rule)?.let { return it }
    }
    if (args == null || !jsonContains(args.getOrNull(0), "_Permanent", "HostPermanent")) {
        reasons.add("PermanentLayerEffect"); return null
    }
    val layerEffects = (args.getOrNull(1) as? JsonArray)?.filterIsInstance<JsonObject>()
    if (layerEffects.isNullOrEmpty()) { reasons.add("PermanentLayerEffect"); return null }
    val stmts = mutableListOf<Stmt>()
    for (le in layerEffects) {
        val abilities: List<Dsl> = when (le.strField("_StaticLayerEffect")) {
            "AdjustPT" -> {
                val pt = le["args"] as? JsonArray
                if (pt?.size != 2) { reasons.add("PermanentLayerEffect"); return null }
                listOf(call("ModifyStats", arg("${pt[0].asInt()}"), arg("${pt[1].asInt()}")))
            }
            "AddAbility" -> {
                val granted = (le["args"] as? JsonArray)?.getOrNull(0) as? JsonObject
                // "Enchanted creature has protection from <color>": the Ward cycle uses a
                // `ProtectionAndDoesntRemovePermanents` rule, the Crowns a plain `Protection` rule.
                if (granted?.strField("_Rule") in setOf("Protection", "ProtectionAndDoesntRemovePermanents")) {
                    val colors = protectionGrantColors(granted!!) ?: run { reasons.add("PermanentLayerEffect"); return null }
                    colors.map { call("GrantProtection", arg("Color.$it")) }
                } else if (granted?.strField("_Rule") == "Ward") {
                    // "Equipped/enchanted creature has ward {N}" (Lavaspur Boots) — render GrantWard carrying
                    // the cost, never a bare GrantKeyword(WARD) which would drop it. Only a mana ward cost
                    // renders; life/discard/sacrifice ward costs aren't modeled here, so scaffold.
                    val costNode = granted["args"] as? JsonObject
                    if (costNode?.strField("_Cost") != "PayMana") { reasons.add("PermanentLayerEffect"); return null }
                    listOf(call("GrantWard", arg("WardCost.Mana(\"${renderMana(costNode["args"])}\")")))
                } else {
                    val kw = keywordOf(le) ?: run { reasons.add("PermanentLayerEffect"); return null }
                    // Prowess grants need the +1/+1 trigger, not just the keyword tag (see staticLordBlock) —
                    // scaffold rather than emit a no-op GrantKeyword grant on the enchanted creature.
                    if (kw == "PROWESS") { reasons.add("PermanentLayerEffect"); return null }
                    listOf(call("GrantKeyword", arg("Keyword.$kw")))
                }
            }
            "SetController" -> {
                // "you control enchanted permanent" (Control Magic, Steal Artifact). Only controller=You
                // maps to the parameterless ControlEnchantedPermanent; any other player scaffolds.
                if (!jsonContains(le["args"], "_Player", "You")) { reasons.add("PermanentLayerEffect"); return null }
                listOf(Lit("ControlEnchantedPermanent"))
            }
            "SetLandType" -> {
                // "Enchanted land is an Island" (Sea's Claim) — replace the host land's subtypes.
                val landType = le["args"].asStr() ?: run { reasons.add("PermanentLayerEffect"); return null }
                listOf(call("SetEnchantedLandType", arg("\"${ktStr(landType)}\"")))
            }
            else -> { reasons.add("PermanentLayerEffect"); return null }
        }
        abilities.forEach { stmts.add(staticAbilityStmt(it)) }
    }
    return stmts
}

/** The colors of a host protection grant ("enchanted creature has protection from <color>" — the Ward
 *  cycle's `ProtectionAndDoesntRemovePermanents` or the Crowns' plain `Protection`), uppercased for
 *  `Color.X`; null for a non-color protection scope (from a type/quality) or none, which scaffolds.
 *  Works regardless of whether the grant wraps its `_Protectable` in an array (Ward) or directly (Crown). */
internal fun protectionGrantColors(granted: JsonObject): List<String>? {
    if (!jsonContains(granted, "_Protectable", "FromColor")) return null
    val colors = Regex(""""_Color":\s*"(\w+)"""").findAll(compact(granted)).map { it.groupValues[1].uppercase() }.toList()
    return colors.ifEmpty { null }
}

/** The affected-group GroupFilter for a lord: chosen-creature-type variable -> the named helper,
 *  otherwise the generic group-filter recovery (fixed subtype, excludeSelf for "other"). */
private fun EmitCtx.lordGroupFilterExpr(filterNode: JsonElement?): Dsl? {
    if (jsonContains(filterNode, "_CreatureTypeVariable", "TheChosenCreatureType") ||
        jsonContains(filterNode, "_Permanents", "IsCreatureTypeVariable")) {
        return call("GroupFilter.ChosenSubtypeCreatures")
    }
    return groupFilterExpr(filterNode)
}

private fun EmitCtx.staticAbilityExpr(ruleName: String, ruleNode: JsonObject): Dsl? {
    when (ruleName) {
        "CantBlock" -> return call("CantBlock")
        "CantBeBlockedByMoreThanOne" -> return call("CantBeBlockedByMoreThan", arg("maxBlockers", "1"))
        "CanBlockOnly" -> {
            val kw = keywordOf(ruleNode)
            val bf = if (kw != null) "GameObjectFilter.Creature.withKeyword(Keyword.$kw)" else "GameObjectFilter.Creature"
            return call("CanOnlyBlockCreaturesWith", arg("blockerFilter", bf))
        }
        "CantBeBlockedByDefenders" -> {
            // mtgish "Defenders" means blockers generally; the rule's args carry the blocker restriction,
            // so this is "can't be blocked BY [filtered creatures]" (Fleet-Footed Monk: power ≥ 2; Sacred
            // Knight: black and/or red). Render the blocker filter generically; scaffold if it can't be
            // expressed faithfully. (Distinct from CantBeBlockedExceptByDefenders, which RESTRICTS blockers.)
            // gameObjectFilterDsl silently ignores predicates it can't render, so scaffold on any shape it
            // doesn't cover (e.g. ToughnessIs) rather than emit a blocker filter missing a restriction.
            if (Regex("\"(ToughnessIs|HasKeyword|ManaValueIs|HasSubtypeFrom)\"").containsMatchIn(compact(ruleNode))) return null
            val filter = gameObjectFilterExpr(ruleNode["args"]) ?: return null
            return call("CantBeBlockedBy", arg("blockerFilter", filter))
        }
        "CantBeBlockedExceptByDefenders" -> {
            val bf = cantBeBlockedExceptByFilter(ruleNode) ?: return null
            return call("CantBeBlockedExceptBy", arg("blockerFilter", bf))
        }
        "CantAttackUnlessDefendingPlayer" -> {  // Deep-Sea Serpent: defender must control an Island
            val subs = subtypes(ruleNode)
            if (subs.isEmpty()) return null
            return call("CantAttackUnless", arg(call("Conditions.OpponentControlsLandType", arg("\"${subs[0]}\""))))
        }
        "MustBlockAttacker" -> return call("MustBlock")
        "MustAttackPlayer" -> return call("MustAttack")
        // "This creature attacks each combat if able" (Dauthi Slayer, Juggernaut) — the unfiltered
        // self MustAttack, distinct from MustAttackPlayer which carries a forced defender.
        "MustAttack" -> return call("MustAttack")
        "CanBlockAnyNumberOfCreatures" -> return call("CanBlockAnyNumber")
    }
    return null
}

/**
 * The blocker filter for a `CantBeBlockedExceptByDefenders` rule — the creatures that may STILL block
 * (the rule restricts blockers down to these). Renders the "defender" oracle idiom, a single creature
 * subtype, or a single keyword restriction ("except by creatures with haste"); declines (null) on any
 * compound / unrecognised shape so the card scaffolds rather than emitting a too-broad "except by any
 * creature". Shared by the static [com.wingedsheep.sdk.scripting.CantBeBlockedExceptBy] ability and the
 * floating one-shot `Effects.GrantCantBeBlockedExceptBy` grant (`CreatePermanentRuleEffectUntil`).
 */
internal fun EmitCtx.cantBeBlockedExceptByFilter(ruleNode: JsonObject): String? {
    // A compound blocker restriction ("except by Walls and/or creatures with flying" — Elven Riders)
    // unions a creature subtype with a keyword/type clause via an Or/And node. This surface renders only
    // flat single-clause shapes; the nested IsCreatureType / HasAbility scans below would each grab one
    // branch and silently drop the other (emitting "except by Walls", dropping the flyers). Decline so
    // the card scaffolds rather than emitting a confidently-wrong, too-narrow blocker filter.
    if ((ruleNode["args"] as? JsonObject)?.strField("_Permanents") in setOf("Or", "And")) return null
    if (oracleText?.contains("defender", ignoreCase = true) == true)
        return "GameObjectFilter.Creature.withKeyword(Keyword.DEFENDER)"
    // "except by [creature subtype]" (Invisibility: except by Walls). The rule names the *only* legal
    // blockers, so it must render CantBeBlockedExceptBy with that subtype — a bare CantBeBlockedBy would
    // invert the meaning (removing those blockers rather than restricting to them).
    ruleNode.firstArgWordTagged("IsCreatureType")?.let {
        return "GameObjectFilter.Creature.withSubtype(${subtypeArg(it)})"
    }
    // "except by creatures with <keyword>" (Resilient Roadrunner: haste). Render only the clean
    // "Creature + one known keyword" shape; any extra predicate (color / power / mana value / subtype /
    // a second or negated ability) declines so we never silently drop a restriction.
    val hasAbilities = ruleNode.nodesTagged("HasAbility")
    if (hasAbilities.size == 1 && ruleNode.nodesTagged("DoesntHaveAbility").isEmpty()) {
        val foreign = listOf(
            "IsColor", "IsNonColor", "PowerIs", "ToughnessIs", "ManaValueIs",
            "IsTapped", "IsUntapped", "IsAttacking", "IsBlocking", "HasACounterOfType"
        )
        if (foreign.any { ruleNode.hasTag(it) }) return null
        val kw = pascalToUpperSnake(hasAbilities[0].firstWordAtKey("_CheckHasable") ?: return null)
        if (kw !in keywords) return null  // unknown ability -> decline, don't widen the filter
        return "GameObjectFilter.Creature.withKeyword(Keyword.$kw)"
    }
    return null
}

/**
 * A top-level `PlayerEffect(You, [...])` rule -> one `staticAbility { ability = ... }` per recognised
 * player-static. Only shapes with an exact controller-scoped StaticAbility render; anything else (the
 * top-of-library / cost-reduction player statics) scaffolds rather than guess. Currently: "you have
 * shroud" (True Believer).
 */
internal fun EmitCtx.playerEffectBlock(rule: JsonObject): List<Stmt>? {
    val args = rule["args"] as? JsonArray
    val player = args?.getOrNull(0)
    val effects = (args?.getOrNull(1) as? JsonArray)?.filterIsInstance<JsonObject>()
    if (player == null || effects.isNullOrEmpty() || !jsonContains(player, "_Player", "You")) {
        reasons.add("PlayerEffect"); return null
    }
    val stmts = mutableListOf<Stmt>()
    for (e in effects) {
        val ability = when (e.strField("_PlayerEffect")) {
            "Shroud" -> Lit("GrantShroudToController")
            else -> { reasons.add("PlayerEffect"); return null }
        }
        stmts.add(staticAbilityStmt(ability))
    }
    return stmts
}

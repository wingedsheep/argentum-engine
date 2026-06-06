package com.wingedsheep.tooling.coverage.emitter

import com.wingedsheep.tooling.coverage.asArr
import com.wingedsheep.tooling.coverage.asStr
import com.wingedsheep.tooling.coverage.compact
import com.wingedsheep.tooling.coverage.field
import com.wingedsheep.tooling.coverage.findInteger
import com.wingedsheep.tooling.coverage.jsonContains
import com.wingedsheep.tooling.coverage.strField
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

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

/** (tdsl, tvar) for an envelope's targets; (null,null) if unrenderable; ("",null) if none. */
internal fun EmitCtx.spellTarget(targets: List<JsonObject>?, actions: List<JsonObject>? = null): Pair<String?, String?> {
    if (targets.isNullOrEmpty()) return "" to null
    if (targets.size > 1) { reasons.add("multi-target"); return null to null }
    val tdsl = targetDsl(targets[0], actions) ?: run { reasons.add("target:${targets[0].strField("_Target")}"); return null to null }
    return tdsl to "t"
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
        else -> false
    }
}

internal fun EmitCtx.cardLevelCastEffectLines(card: JsonObject): List<String>? {
    val lines = mutableListOf<String>()
    for (rule in (card["Rules"].asArr ?: JsonArray(emptyList())).filterIsInstance<JsonObject>()) {
        if (rule.strField("_Rule") != "CastEffect") continue
        val line = additionalCostLine(rule)
        if (line != null) lines.add(line)
        else if (!castEffectHandled(rule)) return null
    }
    return lines
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
private fun EmitCtx.conditionalSpell(card: JsonObject): List<String>? {
    val (_, actions) = extractEnvelope(card["Rules"])
    if (actions == null || actions.size != 1 || actions[0].strField("_Action") != "If") return null
    val ifNode = actions[0]
    val cond = conditionDsl(ifNode) ?: return null
    val body = ifNode["args"].asArr
    val inner = if (body != null && body.size > 1 && body[1] is JsonArray) (body[1] as JsonArray).filterIsInstance<JsonObject>() else null
    if (inner == null) return null
    val edsl = renderEffectList(inner, null) ?: return null
    return listOf("    spell {", "        condition = $cond", "        effect = $edsl", "    }")
}

internal fun EmitCtx.spellBlock(card: JsonObject): List<String>? {
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
    val (tdsl, tvar) = spellTarget(targets, actions)
    if (tdsl == null) return null
    val edsl = renderEffectList(actions, tvar) ?: return null
    val restrictions = castRestrictionLines((card["Rules"].asArr ?: JsonArray(emptyList())).filterIsInstance<JsonObject>()) ?: return null
    val inner = if (tvar != null) listOf("        val t = target(\"target\", $tdsl)") else emptyList()
    return listOf("    spell {") + restrictions + inner + listOf("        effect = $edsl", "    }")
}

private fun spellOf(effect: String) = listOf("    spell {", "        effect = $effect", "    }")

private val TRIGGER_SPEC = mapOf(
    "WhenAPermanentEntersTheBattlefield" to "Triggers.EntersBattlefield",
    "WhenACreatureOrPlaneswalkerDies" to "Triggers.Dies",
    "WhenACreatureAttacks" to "Triggers.Attacks",
    "WhenACreatureDealsCombatDamageToAPlayer" to "Triggers.DealsCombatDamageToPlayer",
)

/** A TriggerA rule (self-triggered) -> triggeredAbility { trigger; [target]; effect }. */
internal fun EmitCtx.triggerBlock(rule: JsonObject): List<String>? {
    var spec: String? = null
    for ((mtTrigger, dsl) in TRIGGER_SPEC) {
        if (jsonContains(rule, "_Trigger", mtTrigger) && jsonContains(rule, "_Permanent", "ThisPermanent")) { spec = dsl; break }
    }
    if (spec == null) { reasons.add("trigger-shape"); return null }
    val (targets, actions) = extractEnvelope(rule)
    if (actions == null) { reasons.add("trigger-actions"); return null }
    val (tdsl, tvar) = spellTarget(targets, actions)
    if (tdsl == null) return null

    // "you may [do X]" on a triggered ability is an OPTIONAL ability (declined at announcement /
    // by choosing no targets), not a resolution-time MayEffect. Unwrap a lone MayAction so the
    // ability carries `optional = true` and a plain effect — the engine's idiom for "may [target]".
    val mayWrapped = actions.singleOrNull()?.strField("_Action") == "MayAction"
    val effectActions = if (mayWrapped) listOf(innerAction(actions.single()) ?: return null) else actions
    val edsl = renderEffectList(effectActions, tvar) ?: return null

    val lines = mutableListOf("    triggeredAbility {", "        trigger = $spec")
    if (mayWrapped) lines.add("        optional = true")
    if (tvar != null) lines.add("        val t = target(\"target\", $tdsl)")
    lines.addAll(listOf("        effect = $edsl", "    }"))
    return lines
}

/** An Activated / ActivatedWithModifiers rule -> activatedAbility { cost; [target]; effect }. */
internal fun EmitCtx.activatedBlock(rule: JsonObject): List<String>? {
    val args = rule["args"].asArr
    val costNode = args?.firstOrNull() as? JsonObject
    // Recover the exact activation cost. Anything we can't render exactly -> SCAFFOLD (never guess Tap).
    val cost = costNode?.let { abilityCostDsl(it) }
    if (cost == null) { reasons.add("activated-cost"); return null }
    val (targets, actions) = extractEnvelope(rule)
    if (actions == null) { reasons.add("activated-actions"); return null }
    val (tdsl, tvar) = spellTarget(targets, actions)
    if (tdsl == null) return null
    val edsl = renderEffectList(actions, tvar) ?: return null
    val lines = mutableListOf("    activatedAbility {", "        cost = $cost")
    activationRestrictionLines(rule)?.let { lines.addAll(it) } ?: return null
    if (tvar != null) lines.add("        val t = target(\"target\", $tdsl)")
    lines.addAll(listOf("        effect = $edsl", "    }"))
    return lines
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
        "TapPermanent" ->
            if (obj.field("args").strField("_Permanent") == "ThisPermanent") "Costs.Tap" else null
        "SacrificePermanent" ->
            if (obj.field("args").strField("_Permanent") == "ThisPermanent") "Costs.SacrificeSelf" else null
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
        "AnyPermanent" -> return "GameObjectFilter.Permanent"
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

private fun EmitCtx.activationRestrictionLines(rule: JsonObject): List<String>? {
    if (rule.strField("_Rule") != "ActivatedWithModifiers") return emptyList()
    val blob = compact(rule)
    if ("ActivateOnlyIf" in blob && "IsTheirTurn" in blob && "IsBeforeAttackersDeclared" in blob) {
        return listOf(
            "        restrictions = listOf(",
            "            ActivationRestriction.OnlyDuringYourTurn,",
            "            ActivationRestriction.BeforeStep(Step.DECLARE_ATTACKERS)",
            "        )",
        )
    }
    reasons.add("activated-modifiers")
    return null
}

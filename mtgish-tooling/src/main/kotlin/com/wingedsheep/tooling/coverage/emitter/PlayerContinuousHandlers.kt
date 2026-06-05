package com.wingedsheep.tooling.coverage.emitter

import com.wingedsheep.tooling.coverage.compact
import com.wingedsheep.tooling.coverage.findInteger
import com.wingedsheep.tooling.coverage.jsonContains
import com.wingedsheep.tooling.coverage.strField
import kotlinx.serialization.json.JsonObject

/** Player-delegated actions ("target player does X" / each-player), the `you may` wrapper, and the
 *  duration-scoped trigger/replacement creators + group rule grants. */
internal val playerContinuousHandlers: Map<String, ActionHandler> = buildMap {
    fun reg(vararg keys: String, h: ActionHandler) = keys.forEach { put(it, h) }

    reg("MayAction") { node, _, tvar ->  // "you may X" -> MayEffect wrapper
        val inner = innerAction(node) ?: return@reg null
        val rendered = renderAction(inner, tvar) ?: return@reg null
        used.add("MayEffect")
        "MayEffect($rendered)"
    }

    reg("EachPlayerAction") { node, _, _ -> renderEachPlayer(node) }
    reg("PlayerAction", "HavePlayerTakeAction") { node, _, tvar -> renderPlayerAction(node, tvar) }

    reg("CreateReplaceWouldDealDamageUntil") { node, _, _ ->
        // "prevent all damage attacking creatures would deal to you this turn" (Deep Wood)
        val blob = compact(node)
        if ("IsAttacking" in blob && "PreventThatDamage" in blob && jsonContains(node, "_Player", "You")) {
            used.add("Effects"); "Effects.PreventDamageFromAttackingCreatures()"
        } else null
    }
    reg("CreateTriggerUntil") { node, _, _ ->
        // Harsh Justice: reflect attackers' combat damage back to their controller.
        val blob = compact(node)
        if ("WhenACreatureDealsCombatDamageToAPlayer" in blob && "ControllerOfPermanent" in blob && "Trigger_ThatMuch" in blob) {
            used.add("ReflectCombatDamageEffect"); "ReflectCombatDamageEffect()"
        } else null
    }
    reg("CreateEachPermanentRuleEffectUntil") { node, _, tvar -> renderGrantToGroup(node, tvar) }
}

/** A spell that grants a combat rule to a group / target (Alluring Scent, Taunt, Dread Charge). */
internal fun EmitCtx.renderGrantToGroup(node: JsonObject, tvar: String?): String? {
    if (jsonContains(node, "_PermanentRule", "MustBlockAttacker")) {  // "all creatures must block target"
        val tgt = refTarget(node["args"], tvar) ?: return null
        used.add("MustBeBlockedEffect"); return "MustBeBlockedEffect($tgt)"
    }
    if (jsonContains(node, "_PermanentRule", "MustAttackPlayer")) {  // Taunt
        used.add("TauntEffect"); return if (tvar != null) "TauntEffect($tvar)" else "TauntEffect()"
    }
    val blob = compact(node)
    if (("CantBeBlockedExceptByColor" in blob || "CantBeBlockedExceptByDefenders" in blob) && "\"_Color\"" in blob) {
        used.addAll(listOf("GrantCantBeBlockedExceptByColorEffect", "GroupFilter", "Color"))
        val m = Regex(""""_Color":\s*"(\w+)"""").find(blob)
        val color = if (m != null) "Color.${m.groupValues[1].uppercase()}" else "Color.BLACK"
        val filter = groupFilterDsl(node["args"]) ?: return null
        return "GrantCantBeBlockedExceptByColorEffect(filter = $filter, canOnlyBeBlockedByColor = $color)"
    }
    return null
}

/** `target player does X` — render X scoped to the referenced player. */
internal fun EmitCtx.renderPlayerAction(node: JsonObject, tvar: String?): String? {
    val args = node["args"]
    if (jsonContains(node, "_Player", "OwnerOfPermanent") && jsonContains(node, "_Action", "GainLife")) {
        used.add("OwnerGainsLifeEffect")  // Path of Peace: destroyed permanent's owner gains N
        return "OwnerGainsLifeEffect(${findInteger(args)})"
    }
    val inner = innerAction(node) ?: return null
    val ptv = refTarget(args, tvar)  // the player the action applies to
    when (inner.strField("_Action")) {
        "DiscardACard", "DiscardNumberCards", "DiscardAnyNumberOfCards" -> {
            used.add("EffectPatterns")
            val n = (findInteger(inner["args"]) as? Int) ?: 1
            return if (ptv != null) "EffectPatterns.discardCards($n, $ptv)" else "EffectPatterns.discardCards($n)"
        }
        "DrawNumberCards", "DrawACard" -> {
            used.add("DrawCardsEffect")
            val amt = if (inner.strField("_Action") == "DrawACard") "1" else amount(inner["args"])
            if (amt == null) return null
            return if (ptv != null) "DrawCardsEffect($amt, $ptv)" else "DrawCardsEffect($amt)"
        }
        "GainLife" -> {
            val amt = amount(inner["args"])
            used.add("GainLifeEffect")
            return if (amt != null && ptv != null) "GainLifeEffect($amt, $ptv)" else if (amt != null) "GainLifeEffect($amt)" else null
        }
        "LoseLife" -> {
            val amt = amount(inner["args"])
            if (amt == null || ptv == null) return null
            used.add("LoseLifeEffect"); return "LoseLifeEffect($amt, $ptv)"
        }
        "DiscardACardAtRandom" -> {
            used.add("EffectPatterns")
            return if (ptv != null) "EffectPatterns.discardRandom(1, $ptv)" else "EffectPatterns.discardRandom(1)"
        }
        "SkipAllCombatPhasesTheirNextTurn" -> {
            used.add("SkipCombatPhasesEffect")
            return if (ptv != null) "SkipCombatPhasesEffect($ptv)" else "SkipCombatPhasesEffect()"
        }
        "RevealHand" -> {
            used.add("RevealHandEffect")
            return if (ptv != null) "RevealHandEffect($ptv)" else "RevealHandEffect()"
        }
    }
    return null
}

internal fun EmitCtx.renderEachPlayer(node: JsonObject): String? {
    used.add("EffectPatterns")
    val blob = compact(node)
    if (jsonContains(node, "_Players", "Opponent") && "Discard" in blob) return "EffectPatterns.eachOpponentDiscards(1)"  // Noxious Toad
    if (jsonContains(node, "_Action", "DrawNumberCards") || jsonContains(node, "_GameNumber", "ValueX"))
        return "EffectPatterns.eachPlayerDrawsX(includeController = true, includeOpponents = true)"
    return null
}

package com.wingedsheep.tooling.coverage.emitter

import com.wingedsheep.tooling.coverage.compact
import com.wingedsheep.tooling.coverage.findInteger
import com.wingedsheep.tooling.coverage.jsonContains
import com.wingedsheep.tooling.coverage.strField
import kotlinx.serialization.json.JsonObject

/** Player-delegated actions ("target player does X" / each-player), the `you may` wrapper, and the
 *  duration-scoped trigger/replacement creators + group rule grants. */
internal val playerContinuousHandlers: Map<String, ActionHandler> = actionHandlers {

    on("MayAction") { node, _, tvar ->  // "you may X" -> MayEffect wrapper
        val inner = innerAction(node) ?: return@on null
        val rendered = renderAction(inner, tvar) ?: return@on null
        "MayEffect($rendered)"
    }

    on("EachPlayerAction") { node, _, _ -> renderEachPlayer(node) }
    on("PlayerAction", "HavePlayerTakeAction") { node, _, tvar -> renderPlayerAction(node, tvar) }

    on("CreateReplaceWouldDealDamageUntil") { node, _, _ ->
        val blob = compact(node)
        when {
            // "prevent all damage attacking creatures would deal to you this turn" (Deep Wood)
            "IsAttacking" in blob && "PreventThatDamage" in blob && jsonContains(node, "_Player", "You") ->
                "Effects.PreventDamageFromAttackingCreatures()"
            // "prevent all combat damage that would be dealt this turn" (Leery Fogbeast): the unrestricted
            // CombatDamageWouldBeDealt event (no source/recipient filter) + PreventThatDamage until EOT.
            jsonContains(node, "_ReplacableEventWouldDealDamage", "CombatDamageWouldBeDealt") &&
                "PreventThatDamage" in blob && jsonContains(node, "_Expiration", "UntilEndOfTurn") ->
                "Effects.PreventAllCombatDamage()"
            else -> null
        }
    }
    on("CreateTriggerUntil") { node, _, _ ->
        // Harsh Justice: reflect attackers' combat damage back to their controller.
        val blob = compact(node)
        if ("WhenACreatureDealsCombatDamageToAPlayer" in blob && "ControllerOfPermanent" in blob && "Trigger_ThatMuch" in blob) {
            "ReflectCombatDamageEffect()"
        } else null
    }
    on("CreateEachPermanentRuleEffectUntil") { node, _, tvar -> renderGrantToGroup(node, tvar) }
}

/** A spell that grants a combat rule to a group / target (Alluring Scent, Taunt, Dread Charge). */
internal fun EmitCtx.renderGrantToGroup(node: JsonObject, tvar: String?): String? {
    if (jsonContains(node, "_PermanentRule", "MustBlockAttacker")) {  // "all creatures must block target"
        val tgt = refTarget(node["args"], tvar) ?: return null
        return "MustBeBlockedEffect($tgt)"
    }
    if (jsonContains(node, "_PermanentRule", "MustAttackPlayer")) {  // Taunt
        return if (tvar != null) "TauntEffect($tvar)" else "TauntEffect()"
    }
    val blob = compact(node)
    if (("CantBeBlockedExceptByColor" in blob || "CantBeBlockedExceptByDefenders" in blob) && "\"_Color\"" in blob) {
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
        // Path of Peace: destroyed permanent's owner gains N
        return "OwnerGainsLifeEffect(${findInteger(args)})"
    }
    val inner = innerAction(node) ?: return null
    val ptv = refTarget(args, tvar)  // the player the action applies to
    when (inner.strField("_Action")) {
        "DiscardACard", "DiscardNumberCards", "DiscardAnyNumberOfCards" -> {
            // discardCards takes a fixed Int — a derived/X count (Arcane Omens' "colours of mana spent")
            // can't be expressed, so scaffold rather than default to a wrong fixed amount.
            val n = if (inner.strField("_Action") == "DiscardACard") "1" else strictCardCount(inner["args"])
            if (n == null) return null
            return if (ptv != null) "Patterns.Hand.discardCards($n, $ptv)" else "Patterns.Hand.discardCards($n)"
        }
        "DrawNumberCards", "DrawACard" -> {
            // Only a fixed Integer or X count renders; a derived count (Mathemagics' "2ˣ") scaffolds.
            val amt = if (inner.strField("_Action") == "DrawACard") "1"
                      else strictCardCount(inner["args"], forX = "DynamicAmount.XValue")
            if (amt == null) return null
            return if (ptv != null) "DrawCardsEffect($amt, $ptv)" else "DrawCardsEffect($amt)"
        }
        "GainLife" -> {
            val amt = amount(inner["args"])
            return if (amt != null && ptv != null) "GainLifeEffect($amt, $ptv)" else if (amt != null) "GainLifeEffect($amt)" else null
        }
        "LoseLife" -> {
            val amt = amount(inner["args"])
            if (amt == null || ptv == null) return null
            return "LoseLifeEffect($amt, $ptv)"
        }
        "DiscardACardAtRandom" -> {
            return if (ptv != null) "Patterns.Hand.discardRandom(1, $ptv)" else "Patterns.Hand.discardRandom(1)"
        }
        "SkipAllCombatPhasesTheirNextTurn" -> {
            return if (ptv != null) "SkipCombatPhasesEffect($ptv)" else "SkipCombatPhasesEffect()"
        }
        "RevealHand" -> {
            return if (ptv != null) "RevealHandEffect($ptv)" else "RevealHandEffect()"
        }
        "ShuffleGraveyardIntoLibrary" -> {
            // "target player shuffles their graveyard into their library" (Reminisce). Only the
            // bound-target-player form renders; an untargeted/you form scaffolds rather than guess.
            return if (ptv != null) "Patterns.Library.shuffleGraveyardIntoLibrary($ptv)" else null
        }
    }
    return null
}

internal fun EmitCtx.renderEachPlayer(node: JsonObject): String? {
    val blob = compact(node)
    if (jsonContains(node, "_Players", "Opponent") && "Discard" in blob) return "Patterns.Hand.eachOpponentDiscards(1)"  // Noxious Toad
    if (jsonContains(node, "_Action", "DrawNumberCards") || jsonContains(node, "_GameNumber", "ValueX"))
        return "Patterns.Hand.eachPlayerDrawsX(includeController = true, includeOpponents = true)"
    return null
}

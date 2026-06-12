package com.wingedsheep.tooling.coverage.emitter

import com.wingedsheep.tooling.coverage.Dsl
import com.wingedsheep.tooling.coverage.Lit
import com.wingedsheep.tooling.coverage.arg
import com.wingedsheep.tooling.coverage.asArr
import com.wingedsheep.tooling.coverage.call
import com.wingedsheep.tooling.coverage.compact
import com.wingedsheep.tooling.coverage.findInteger
import com.wingedsheep.tooling.coverage.jsonContains
import com.wingedsheep.tooling.coverage.strField
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject

/** Player-delegated actions ("target player does X" / each-player), the `you may` wrapper, and the
 *  duration-scoped trigger/replacement creators + group rule grants. */
internal val playerContinuousHandlers: Map<String, ActionHandler> = actionHandlers {

    on("MayAction") { node, _, tvar ->  // "you may X" -> MayEffect wrapper
        val inner = innerAction(node) ?: return@on null
        val rendered = renderAction(inner, tvar) ?: return@on null
        call("MayEffect", arg(rendered))
    }

    // "you may [X and Y]" — a single optional choice gating a sequence of actions (Gustcloak cycle's
    // "you may untap it and remove it from combat"). Renders the whole sequence as one MayEffect, so a
    // partial render (only one arm) declines via renderEffectList rather than dropping an action.
    on("MayActions") { node, _, tvar ->
        val inner = node["args"].asArr?.filterIsInstance<JsonObject>() ?: return@on null
        val edsl = renderEffectList(inner, tvar) ?: return@on null
        call("MayEffect", arg(edsl))
    }

    on("FlipACoin_OnLose") { _, args, tvar ->
        // "Flip a coin. If you lose the flip, [actions]." (Ydwen Efreet). `args` is the on-lose action
        // list, rendered as the FlipCoinEffect's lostEffect; if any on-lose action can't render the
        // whole flip scaffolds rather than emit a partial coin flip.
        val lost = args.asArr?.filterIsInstance<JsonObject>() ?: return@on null
        val edsl = renderEffectList(lost, tvar) ?: return@on null
        call("FlipCoinEffect", arg("lostEffect", edsl))
    }

    on("EachPlayerAction") { node, _, _ -> renderEachPlayer(node) }
    on("PlayerAction", "HavePlayerTakeAction") { node, _, tvar -> renderPlayerAction(node, tvar) }

    on("CreateReplaceWouldDealDamageUntil") { node, _, tvar ->
        val blob = compact(node)
        when {
            // "Prevent all combat damage that would be dealt to and dealt by that creature this turn"
            // (Maze of Shadows): an Or of the to-recipient + by-creature combat-damage events, both over
            // the bound target permanent, + PreventThatDamage until EOT -> PreventCombatDamageToAndBy.
            // Require the bound target so a self/untargeted variant doesn't slip through.
            tvar != null &&
                "CombatDamageWouldBeDealtToRecipient" in blob &&
                "CombatDamageWouldBeDealtByCreature" in blob &&
                jsonContains(node, "_Permanent", "Ref_TargetPermanent") &&
                "PreventThatDamage" in blob && jsonContains(node, "_Expiration", "UntilEndOfTurn") ->
                call("Effects.PreventCombatDamageToAndBy", arg(Lit(tvar)))
            // "prevent all damage attacking creatures would deal to you this turn" (Deep Wood)
            "IsAttacking" in blob && "PreventThatDamage" in blob && jsonContains(node, "_Player", "You") ->
                call("Effects.PreventDamageFromAttackingCreatures")
            // "prevent all combat damage that would be dealt this turn" (Leery Fogbeast): the unrestricted
            // CombatDamageWouldBeDealt event (no source/recipient filter) + PreventThatDamage until EOT.
            jsonContains(node, "_ReplacableEventWouldDealDamage", "CombatDamageWouldBeDealt") &&
                "PreventThatDamage" in blob && jsonContains(node, "_Expiration", "UntilEndOfTurn") ->
                call("Effects.PreventAllCombatDamage")
            else -> null
        }
    }

    // PREVENTION twin of CreateReplaceWouldDealDamageUntil (mtgish prevention/replacement split). Verified
    // against the post-split IR for all three shapes: Cephalid Illusionist (to-and-by `Or`, `_Permanent`
    // = Ref_TargetPermanent), Deep Wood (attacking-creatures-to-you), Angelsong (all combat damage). The
    // split renamed the event field `_ReplacableEventWouldDealDamage` -> `_EventPreventDamage` and the action
    // field `_ReplacementActionWouldDealDamage` -> `_ActionPreventDamage`, but preserved every value string
    // and the `_Expiration` / `_Permanent` / `_Player` discriminators — so the original matching still holds.
    // The `_ActionPreventDamage` = PreventThatDamage payload survived, so we keep the `"PreventThatDamage"`
    // guard (a prevention-with-rider scaffolds). The unrestricted (Angelsong) case is matched key-agnostically
    // — `CombatDamageWouldBeDealt` present with neither narrower variant — instead of keying off the renamed
    // event field. Anything that doesn't match an exact shape declines (→ SCAFFOLD).
    on("CreatePreventDamageUntil") { node, _, tvar ->
        val blob = compact(node)
        when {
            tvar != null &&
                "CombatDamageWouldBeDealtToRecipient" in blob &&
                "CombatDamageWouldBeDealtByCreature" in blob &&
                jsonContains(node, "_Permanent", "Ref_TargetPermanent") &&
                "PreventThatDamage" in blob && jsonContains(node, "_Expiration", "UntilEndOfTurn") ->
                call("Effects.PreventCombatDamageToAndBy", arg(Lit(tvar)))
            "IsAttacking" in blob && "PreventThatDamage" in blob && jsonContains(node, "_Player", "You") ->
                call("Effects.PreventDamageFromAttackingCreatures")
            "CombatDamageWouldBeDealt" in blob &&
                "CombatDamageWouldBeDealtToRecipient" !in blob &&
                "CombatDamageWouldBeDealtByCreature" !in blob &&
                "PreventThatDamage" in blob && jsonContains(node, "_Expiration", "UntilEndOfTurn") ->
                call("Effects.PreventAllCombatDamage")
            else -> null
        }
    }
    on("CreateTriggerUntil") { node, _, _ ->
        // Harsh Justice: reflect attackers' combat damage back to their controller.
        val blob = compact(node)
        if ("WhenACreatureDealsCombatDamageToAPlayer" in blob && "ControllerOfPermanent" in blob && "Trigger_ThatMuch" in blob) {
            call("ReflectCombatDamageEffect")
        } else null
    }
    on("CreateEachPermanentRuleEffectUntil") { node, _, tvar -> renderGrantToGroup(node, tvar) }
}

/** A spell that grants a combat rule to a group / target (Alluring Scent, Taunt, Dread Charge). */
internal fun EmitCtx.renderGrantToGroup(node: JsonObject, tvar: String?): Dsl? {
    if (jsonContains(node, "_PermanentRule", "MustBlockAttacker")) {  // "all creatures must block target"
        val tgt = refTarget(node["args"], tvar) ?: return null
        return call("MustBeBlockedEffect", arg(Lit(tgt)))
    }
    if (jsonContains(node, "_PermanentRule", "MustAttackPlayer")) {  // Taunt
        return if (tvar != null) call("TauntEffect", arg(Lit(tvar))) else call("TauntEffect")
    }
    val blob = compact(node)
    if (("CantBeBlockedExceptByColor" in blob || "CantBeBlockedExceptByDefenders" in blob) && "\"_Color\"" in blob) {
        val m = Regex(""""_Color":\s*"(\w+)"""").find(blob)
        val color = if (m != null) "Color.${m.groupValues[1].uppercase()}" else "Color.BLACK"
        val filter = groupFilterExpr(node["args"]) ?: return null
        return call("GrantCantBeBlockedExceptByColorEffect", arg("filter", filter), arg("canOnlyBeBlockedByColor", color))
    }
    return null
}

/** `target player does X` — render X scoped to the referenced player. */
internal fun EmitCtx.renderPlayerAction(node: JsonObject, tvar: String?): Dsl? {
    val args = node["args"]
    if (jsonContains(node, "_Player", "OwnerOfPermanent") && jsonContains(node, "_Action", "GainLife")) {
        // Path of Peace: destroyed permanent's owner gains N
        return call("OwnerGainsLifeEffect", arg("${findInteger(args)}"))
    }
    // "Its controller creates …" — the acting player is the CONTROLLER of the targeted permanent
    // (ControllerOfPermanent(Ref_TargetPermanent)). After "Destroy target …", the destroyed permanent's
    // controller resolves at resolution time via EffectTarget.TargetController (Beast Within precedent),
    // so the wrapped CreateTokens renders with `controller = EffectTarget.TargetController` (Bovine
    // Intervention). Only the bare controller-creates-tokens shape renders; any other action under a
    // ControllerOfPermanent ref declines -> SCAFFOLD rather than mis-attribute it.
    if (jsonContains(node, "_Player", "ControllerOfPermanent") && jsonContains(node, "_Action", "CreateTokens")) {
        val createTokens = (args as? JsonArray)?.firstOrNull { it is JsonObject && it.containsKey("_Action") } as? JsonObject
        val spec = createTokens?.get("args").asArr?.firstOrNull() as? JsonObject ?: return null
        return createTokenDsl(spec, controller = "EffectTarget.TargetController")
    }
    val inner = innerAction(node) ?: return null
    val ptv = refTarget(args, tvar)  // the player the action applies to
    when (inner.strField("_Action")) {
        "DiscardACard", "DiscardNumberCards", "DiscardAnyNumberOfCards" -> {
            // discardCards takes a fixed Int — a derived/X count (Arcane Omens' "colours of mana spent")
            // can't be expressed, so scaffold rather than default to a wrong fixed amount.
            val n = if (inner.strField("_Action") == "DiscardACard") "1" else strictCardCount(inner["args"])
            if (n == null) return null
            return if (ptv != null) call("Patterns.Hand.discardCards", arg(Lit(n)), arg(Lit(ptv))) else call("Patterns.Hand.discardCards", arg(Lit(n)))
        }
        "DrawNumberCards", "DrawACard" -> {
            // Only a fixed Integer or X count renders; a derived count (Mathemagics' "2ˣ") scaffolds.
            val amt = if (inner.strField("_Action") == "DrawACard") "1"
                      else strictCardCount(inner["args"], forX = "DynamicAmount.XValue")
            if (amt == null) return null
            return if (ptv != null) call("DrawCardsEffect", arg(Lit(amt)), arg(Lit(ptv))) else call("DrawCardsEffect", arg(Lit(amt)))
        }
        "GainLife" -> {
            val amt = amount(inner["args"])
            return if (amt != null && ptv != null) call("GainLifeEffect", arg(Lit(amt)), arg(Lit(ptv))) else if (amt != null) call("GainLifeEffect", arg(Lit(amt))) else null
        }
        "LoseLife" -> {
            val amt = amount(inner["args"])
            if (amt == null || ptv == null) return null
            return call("LoseLifeEffect", arg(Lit(amt)), arg(Lit(ptv)))
        }
        "DiscardACardAtRandom" -> {
            return if (ptv != null) call("Patterns.Hand.discardRandom", arg("1"), arg(Lit(ptv))) else call("Patterns.Hand.discardRandom", arg("1"))
        }
        "SkipAllCombatPhasesTheirNextTurn" -> {
            return if (ptv != null) call("SkipCombatPhasesEffect", arg(Lit(ptv))) else call("SkipCombatPhasesEffect")
        }
        "RevealHand" -> {
            return if (ptv != null) call("RevealHandEffect", arg(Lit(ptv))) else call("RevealHandEffect")
        }
        "ShuffleGraveyardIntoLibrary" -> {
            // "target player shuffles their graveyard into their library" (Reminisce). Only the
            // bound-target-player form renders; an untargeted/you form scaffolds rather than guess.
            return if (ptv != null) call("Patterns.Library.shuffleGraveyardIntoLibrary", arg(Lit(ptv))) else null
        }
        "SacrificeAPermanent" -> {
            // "target player sacrifices a <filter> of their choice" — the edict (Diabolic Edict,
            // Gatekeeper of Malakir). The sacrificing player (who chooses) is the bound target player;
            // render ForceSacrificeEffect(filter, 1, targetPlayer). Only the bound-target-player form
            // renders — an untargeted/each-player form scaffolds rather than guess the actor.
            val filter = gameObjectFilterExpr(inner["args"]) ?: return null
            return if (ptv != null) call("ForceSacrificeEffect", arg(filter), arg("1"), arg(Lit(ptv))) else null
        }
        "TakeAnExtraTurn" -> {
            // "Target player takes an extra turn after this one" (Time Warp). Only the
            // bound-target-player form renders here — the untargeted "take an extra turn"
            // (Time Walk) is the standalone `simple("TakeAnExtraTurn", …)` handler, and the
            // "lose at end step" variant (Last Chance) is the `extraTurnEffect` spell shortcut.
            return if (ptv != null) call("TakeExtraTurnEffect", arg("target", Lit(ptv))) else null
        }
        "GainControlOfPermanent" -> {
            // "Target opponent gains control of this permanent" (Jinxed Idol's sacrifice ability).
            // The recipient is the bound target player; the granted permanent must be the source
            // itself (ThisPermanent). Only that exact shape renders — a non-self permanent or an
            // untargeted/each-player actor scaffolds rather than guess who gains what.
            if (ptv == null) return null
            if (!jsonContains(inner["args"], "_Permanent", "ThisPermanent")) return null
            return call(
                "GiveControlToTargetPlayerEffect",
                arg("permanent", "EffectTarget.Self"),
                arg("newController", Lit(ptv)),
            )
        }
    }
    return null
}

internal fun EmitCtx.renderEachPlayer(node: JsonObject): Dsl? {
    val blob = compact(node)
    // "each player returns a permanent they control to its owner's hand" (Words of Wind's replacement).
    if (jsonContains(node, "_Players", "AnyPlayer") && "PutAPermanentIntoItsOwnersHand" in blob)
        return call("Effects.EachPlayerReturnPermanentToHand")
    if (jsonContains(node, "_Players", "Opponent") && "Discard" in blob) return call("Patterns.Hand.eachOpponentDiscards", arg("1"))  // Noxious Toad
    if (jsonContains(node, "_Action", "DrawNumberCards") || jsonContains(node, "_GameNumber", "ValueX"))
        return call("Patterns.Hand.eachPlayerDrawsX", arg("includeController", "true"), arg("includeOpponents", "true"))
    return null
}

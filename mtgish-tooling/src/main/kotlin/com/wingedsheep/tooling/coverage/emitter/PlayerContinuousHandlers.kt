package com.wingedsheep.tooling.coverage.emitter

import com.wingedsheep.tooling.coverage.Composite
import com.wingedsheep.tooling.coverage.Dsl
import com.wingedsheep.tooling.coverage.Lit
import com.wingedsheep.tooling.coverage.amountNode
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

    on("EachPlayerAction", "EachPlayerActions") { node, _, _ -> renderEachPlayer(node) }
    on("PlayerAction", "HavePlayerTakeAction") { node, _, tvar -> renderPlayerAction(node, tvar) }
    on("PlayerActions") { node, _, tvar -> renderPlayerActions(node, tvar) }

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
    on("CreateTriggerUntil") { node, _, tvar ->
        // Harsh Justice: reflect attackers' combat damage back to their controller.
        val blob = compact(node)
        if ("WhenACreatureDealsCombatDamageToAPlayer" in blob && "ControllerOfPermanent" in blob && "Trigger_ThatMuch" in blob) {
            return@on call("ReflectCombatDamageEffect")
        }
        // "When it dies this turn, <actions>." — a self-scoped delayed dies trigger watching the
        // spell's bound target until end of turn (Turn Inside Out: +3/+0 then manifest dread on death;
        // the Desperate Measures `CreateDelayedTriggerEffect(trigger = Triggers.Dies, watchedTarget =
        // t)` shape). Renders only when the trigger is `WhenAPermanentDies` scoped to the
        // bound `Ref_TargetPermanent`, the expiry is UntilEndOfTurn, and the body renders whole.
        whenThatPermanentDiesDelayedTrigger(node, tvar)
    }
    on("CreateEachPermanentRuleEffectUntil") { node, _, tvar -> renderGrantToGroup(node, tvar) }
}

/**
 * `CreateTriggerUntil(WhenAPermanentDies(SinglePermanent(Ref_TargetPermanent)),
 * [actions], UntilEndOfTurn)` → a watched-entity delayed dies trigger on the spell's bound target:
 * `CreateDelayedTriggerEffect(effect = <body>, trigger = Triggers.Dies, watchedTarget = <tvar>,
 * expiry = DelayedTriggerExpiry.EndOfTurn)`.
 *
 * This is the "Target creature gets +X/+Y until end of turn. When it dies this turn, <do something>"
 * shape (Turn Inside Out, Desperate Measures). Only renders when:
 *  - the trigger is `WhenAPermanentDies` scoped to `SinglePermanent(Ref_TargetPermanent)`
 *    (i.e. "when *that* creature dies", the spell's bound target — not some other group),
 *  - the expiry is `UntilEndOfTurn`,
 *  - the body action list renders whole (sharing the ability's bound `tvar`).
 * Any other trigger, subject, expiry, or an unrenderable body declines → SCAFFOLD.
 */
internal fun EmitCtx.whenThatPermanentDiesDelayedTrigger(node: JsonObject, tvar: String?): Dsl? {
    if (tvar == null) return null
    val args = node["args"].asArr ?: return null
    val trigger = args.getOrNull(0) as? JsonObject ?: return null
    val actionList = args.getOrNull(1) as? JsonObject ?: return null
    val expiration = args.getOrNull(2) as? JsonObject ?: return null

    if (trigger.strField("_Trigger") != "WhenAPermanentDies") return null
    if (expiration.strField("_Expiration") != "UntilEndOfTurn") return null

    // The trigger must watch the bound target specifically: SinglePermanent(Ref_TargetPermanent).
    val perms = trigger["args"] as? JsonObject ?: return null
    if (perms.strField("_Permanents") != "SinglePermanent") return null
    val single = perms["args"] as? JsonObject ?: return null
    if (single.strField("_Permanent") != "Ref_TargetPermanent") return null

    if (actionList.strField("_Actions") != "ActionList") return null
    val inner = actionList["args"].asArr?.filterIsInstance<JsonObject>() ?: return null
    if (inner.isEmpty()) return null
    val body = renderEffectList(inner, tvar) ?: return null

    return call(
        "CreateDelayedTriggerEffect",
        arg("effect", body),
        arg("trigger", "Triggers.Dies"),
        arg("watchedTarget", Lit(tvar)),
        arg("expiry", "DelayedTriggerExpiry.EndOfTurn"),
    )
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
    // "<chosen creatures> can't block this turn." — `CantBlock` applied to the 1-2 (or N) chosen
    // creatures (`Ref_TargetPermanents`). Untimely Malfunction's "one or two target creatures can't
    // block this turn" mode. Apply the per-target restriction to EVERY creature the spell chose via
    // ForEachTargetEffect (the same shape Amazing Acrobatics uses for "tap one or two target
    // creatures"). Only the chosen-targets subject renders; a group/filter subject declines below.
    if (jsonContains(node, "_PermanentRule", "CantBlock") &&
        jsonContains(node, "_Permanents", "Ref_TargetPermanents")
    ) {
        return call(
            "ForEachTargetEffect",
            arg(Lit("listOf(Effects.CantBlock(EffectTarget.ContextTarget(0)))")),
        )
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
    // "Target permanent's owner puts it on their choice of the top or bottom of their library" (Vanish
    // from Sight). PutOnTopOrBottomOfLibrary already pauses for the permanent's OWNER to choose
    // top/bottom, so the OwnerOfPermanent wrapper collapses onto the bound target. (The single-action
    // Run Behind shape is also recognised whole-spell by runBehindOwnerTopOrBottomEffect; this per-action
    // case additionally lets it compose with siblings, e.g. Vanish's "Surveil 1".) Only the
    // owner-of-the-bound-target shape renders; any other subject declines via the OwnerOfPermanent catch.
    if (jsonContains(node, "_Player", "OwnerOfPermanent") &&
        jsonContains(node, "_Action", "ReturnPermanentToTopOrBottomOfLibrary")
    ) {
        val inner = innerAction(node) ?: return null
        val target = refTarget(inner["args"], tvar) ?: return null
        return call("Effects.PutOnTopOrBottomOfLibrary", arg(Lit(target)))
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
    // "that creature's controller loses N life" — ControllerOfPermanent(Ref_TargetPermanent) + LoseLife
    // (Foolish Fate's Infusion drain). Like the controller-creates-tokens shape above, the destroyed
    // permanent's controller resolves at resolution time via EffectTarget.TargetController. Only a fixed
    // life amount renders; a derived/X amount declines -> SCAFFOLD.
    if (jsonContains(node, "_Player", "ControllerOfPermanent") && jsonContains(node, "_Action", "LoseLife")) {
        val loseLife = (args as? JsonArray)?.firstOrNull { it is JsonObject && it.containsKey("_Action") } as? JsonObject
        val amt = amount(loseLife?.get("args")) ?: return null
        return call("LoseLifeEffect", arg(Lit(amt)), arg("EffectTarget.TargetController"))
    }
    // A relational player ref — the OWNER/CONTROLLER of the targeted permanent — is only modeled for the
    // specific shapes handled above (owner-gains-life, controller-creates-tokens, controller-loses-life).
    // The generic path below resolves the acting player via refTarget, which mis-maps such a ref to the
    // permanent's OWN bound target — e.g. aiming "then that player discards a card" at the bounced
    // permanent rather than its owner (Compelling Deterrence). Decline (-> SCAFFOLD) rather than
    // mis-attribute the action.
    if (jsonContains(node, "_Player", "OwnerOfPermanent") || jsonContains(node, "_Player", "ControllerOfPermanent")) return null
    val inner = innerAction(node) ?: return null
    val ptv = refTarget(args, tvar)  // the player the action applies to
    return renderPlayerInnerAction(inner, ptv)
}

/**
 * `PlayerActions` (plural) — "target player draws two cards and loses 2 life." The IR carries a
 * `Ref_TargetPlayer` actor plus a *list* of inner actions; render each action scoped to that player
 * via [renderPlayerInnerAction] and combine them with a Composite. Every action must render exactly
 * (no lossy drops) or the whole shape declines -> SCAFFOLD. Decorum Dissertation (SOS).
 */
internal fun EmitCtx.renderPlayerActions(node: JsonObject, tvar: String?): Dsl? {
    val args = node["args"].asArr ?: return null
    // args = [ {_Player: <ref>}, [ <action>, <action>, ... ] ]
    val playerRef = args.getOrNull(0)
    // Only the bound-target-player actor is modeled (a relational owner/controller ref would
    // mis-attribute the actions, as in the singular handler above).
    if (jsonContains(playerRef, "_Player", "OwnerOfPermanent") ||
        jsonContains(playerRef, "_Player", "ControllerOfPermanent")
    ) return null
    val ptv = refTarget(playerRef, tvar)
    val actionList = args.getOrNull(1).asArr ?: return null
    val rendered = actionList.mapNotNull { it as? JsonObject }
        .map { renderPlayerInnerAction(it, ptv) ?: return null }
    if (rendered.isEmpty()) return null
    return if (rendered.size == 1) rendered.single() else Composite(rendered)
}

/** Render a single action applied to the player resolved as [ptv] (null = no bound target). */
private fun EmitCtx.renderPlayerInnerAction(inner: JsonObject, ptv: String?): Dsl? {
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
        "MillNumberCards" -> {
            // "target player mills N cards" (Desperate Bloodseeker's enters trigger). The milled player
            // is the bound target; the count is a fixed Integer or a recognised dynamic amount. An
            // unrenderable count scaffolds rather than guessing.
            val amt = amountExpr(inner["args"]) ?: dynamicAmountExpr(amountNode(inner["args"])) ?: return null
            return if (ptv != null) call("Patterns.Library.mill", arg(amt), arg(Lit(ptv))) else call("Patterns.Library.mill", arg(amt))
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
    // "any number of target players each <action(s)>" — EachPlayerAction / EachPlayerActions whose
    // player scope is Ref_TargetPlayers (the chosen targets of an AnyNumberOfTargetPlayers requirement,
    // Tinybones Joins Up). This is NOT a static Player.Each fan-out — each affected player is a *target*,
    // so it renders as ForEachTargetEffect over the chosen players, each inner action bound to
    // EffectTarget.PlayerRef(Player.ContextPlayer(0)). Only the per-player actions we can render exactly
    // (discard 1, mill 1, lose N) are supported; anything else declines (-> SCAFFOLD).
    if (jsonContains(node, "_Players", "Ref_TargetPlayers")) {
        renderForEachTargetPlayerBody(node)?.let { return it }
        return null
    }
    // "each player returns a permanent they control to its owner's hand" (Words of Wind's replacement).
    if (jsonContains(node, "_Players", "AnyPlayer") && "PutAPermanentIntoItsOwnersHand" in blob)
        return call("Effects.EachPlayerReturnPermanentToHand")
    // Noxious Toad: `EachPlayerAction(Opponent, DiscardACard)` — the opponent's ONLY action is the
    // discard. Arbiter of Woe is `EachPlayerActions(Opponent, [DiscardACard, LoseLife])`; collapsing to
    // eachOpponentDiscards would silently drop the life loss, so inspect the opponent-scoped action
    // list (args[1], a single action or a nested list) and render only when its sole action is the
    // discard — otherwise decline (-> SCAFFOLD) rather than emit a lossy card.
    if (jsonContains(node, "_Players", "Opponent") && "Discard" in blob) {
        val opponentActions = when (val second = node["args"].asArr?.getOrNull(1)) {
            is JsonArray -> second.filterIsInstance<JsonObject>()
            is JsonObject -> listOf(second)
            else -> emptyList()
        }
        return if (opponentActions.singleOrNull()?.strField("_Action") == "DiscardACard")
            call("Patterns.Hand.eachOpponentDiscards", arg("1"))
        else null
    }
    // "Each opponent sacrifices a <filter> of their choice" (Lorehold Charm). The sacrificing player
    // chooses, scoped to every opponent via ForceSacrificeEffect over EffectTarget.PlayerRef(EachOpponent).
    // Only a renderable filter and a count-of-one form render; anything else scaffolds.
    if (jsonContains(node, "_Players", "Opponent") && jsonContains(node, "_Action", "SacrificeAPermanent")) {
        val inner = node["args"].asArr?.filterIsInstance<JsonObject>()
            ?.firstOrNull { it.strField("_Action") == "SacrificeAPermanent" } ?: return null
        val filter = gameObjectFilterExpr(inner["args"]) ?: return null
        return call("Effects.Sacrifice", arg("filter", filter), arg("target", "EffectTarget.PlayerRef(Player.EachOpponent)"))
    }
    // "each opponent loses N life" (Raven of Fell Omens). Only a fixed Integer amount renders, scoped to
    // every opponent via EffectTarget.PlayerRef(Player.EachOpponent); a derived/X amount scaffolds.
    if (jsonContains(node, "_Players", "Opponent") && jsonContains(node, "_Action", "LoseLife")) {
        val inner = node["args"].asArr?.filterIsInstance<JsonObject>()
            ?.firstOrNull { it.strField("_Action") == "LoseLife" } ?: return null
        val amt = findInteger(inner["args"]) as? Int ?: return null
        return call("Effects.LoseLife", arg("$amt"), arg("EffectTarget.PlayerRef(Player.EachOpponent)"))
    }
    // "each player loses N life" (Conciliator's Duelist). Like the opponent shape above but scoped to
    // every player (controller included) via EffectTarget.PlayerRef(Player.Each); a derived/X amount scaffolds.
    if (jsonContains(node, "_Players", "AnyPlayer") && jsonContains(node, "_Action", "LoseLife")) {
        val inner = node["args"].asArr?.filterIsInstance<JsonObject>()
            ?.firstOrNull { it.strField("_Action") == "LoseLife" } ?: return null
        val amt = findInteger(inner["args"]) as? Int ?: return null
        return call("Effects.LoseLife", arg("$amt"), arg("EffectTarget.PlayerRef(Player.Each)"))
    }
    // "each player discards N cards" (Rankle's Prank's first mode). Symmetric twin of the
    // Opponent-scoped shape above, fanned over every player by `Patterns.Hand.eachPlayerDiscards`.
    // Same lossiness guard as the opponent branch: the discard must be the player's SOLE action
    // (an `EachPlayerActions` list carrying a second clause would be silently dropped), and only a
    // fixed Integer count renders — a derived/X count scaffolds.
    if (jsonContains(node, "_Players", "AnyPlayer") && "Discard" in blob) {
        val playerActions = when (val second = node["args"].asArr?.getOrNull(1)) {
            is JsonArray -> second.filterIsInstance<JsonObject>()
            is JsonObject -> listOf(second)
            else -> emptyList()
        }
        val inner = playerActions.singleOrNull()
        val n = when (inner?.strField("_Action")) {
            "DiscardACard" -> 1
            "DiscardNumberCards" -> findInteger(inner["args"]) as? Int
            else -> null
        }
        // Fall through (don't consume the node) when this isn't a lone renderable discard — a
        // discard bundled with another clause is still handled by the shapes further down.
        if (n != null) return call("Patterns.Hand.eachPlayerDiscards", arg("$n"))
    }
    // "each player sacrifices N <filter>s of their choice" (Rankle's Prank's third mode). Like the
    // Opponent-scoped sacrifice above but scoped to every player via PlayerRef(Player.Each); each
    // player picks their own. Only a renderable filter and a fixed Integer count render.
    if (jsonContains(node, "_Players", "AnyPlayer") &&
        (jsonContains(node, "_Action", "SacrificeAPermanent") || jsonContains(node, "_Action", "SacrificeNumberPermanents"))
    ) {
        val inner = node["args"].asArr?.filterIsInstance<JsonObject>()
            ?.firstOrNull { it.strField("_Action") in setOf("SacrificeAPermanent", "SacrificeNumberPermanents") }
        val rendered = if (inner == null) {
            null
        } else if (inner.strField("_Action") == "SacrificeAPermanent") {
            gameObjectFilterExpr(inner["args"])?.let { filter ->
                call("Effects.Sacrifice", arg("filter", filter), arg("target", "EffectTarget.PlayerRef(Player.Each)"))
            }
        } else {
            // args = [<Integer N>, <permanent filter>]
            val outer = inner["args"].asArr
            val amt = findInteger(outer?.getOrNull(0)) as? Int
            val filter = outer?.getOrNull(1)?.let { gameObjectFilterExpr(it) }
            if (amt != null && filter != null) {
                call(
                    "Effects.Sacrifice",
                    arg("filter", filter),
                    arg("count", "$amt"),
                    arg("target", "EffectTarget.PlayerRef(Player.Each)"),
                )
            } else {
                null
            }
        }
        if (rendered != null) return rendered
    }
    // "each opponent mills N cards" (Deepmuck Desperado). Mill is a pipeline composite, so it can't be
    // scoped via a PlayerRef target the way LoseLife/Sacrifice are — it is fanned over every opponent
    // with ForEachPlayerEffect wrapping the standard mill pipeline. Only a fixed Integer count renders;
    // a derived/X amount scaffolds rather than guessing.
    if (jsonContains(node, "_Players", "Opponent") && jsonContains(node, "_Action", "MillNumberCards")) {
        val inner = node["args"].asArr?.filterIsInstance<JsonObject>()
            ?.firstOrNull { it.strField("_Action") == "MillNumberCards" } ?: return null
        val amt = findInteger(inner["args"]) as? Int ?: return null
        return call(
            "ForEachPlayerEffect",
            arg("players", "Player.EachOpponent"),
            arg("effects", "Patterns.Library.mill($amt).effects"),
        )
    }
    // "each player draws a card" (Friendly Teddy's dies trigger) — AnyPlayer scope + DrawACard (a
    // fixed single draw). Scoped to every player via EffectTarget.PlayerRef(Player.Each); the
    // dynamic/X-count variants are handled by the eachPlayerDrawsX shape below.
    if (jsonContains(node, "_Players", "AnyPlayer") && jsonContains(node, "_Action", "DrawACard")) {
        val inner = node["args"].asArr?.filterIsInstance<JsonObject>()
            ?.firstOrNull { it.strField("_Action") == "DrawACard" } ?: return null
        // Only the bare "draw a card" renders here; any richer draw action declines.
        if (inner["args"] != null) return null
        return call("Effects.DrawCards", arg("1"), arg("EffectTarget.PlayerRef(Player.Each)"))
    }
    if (jsonContains(node, "_Action", "DrawNumberCards") || jsonContains(node, "_GameNumber", "ValueX"))
        return call("Patterns.Hand.eachPlayerDrawsX", arg("includeController", "true"), arg("includeOpponents", "true"))
    return null
}

/**
 * "any number of target players each <action(s)>" body → `ForEachTargetEffect(listOf(<per-player
 * effects>))`. The acting players are the chosen targets (Ref_TargetPlayers), so each inner action is
 * applied to `EffectTarget.PlayerRef(Player.ContextPlayer(0))` — the iterated target. Handles both the
 * single-action `EachPlayerAction` (args = [players, action]) and the list `EachPlayerActions`
 * (args = [players, [action, …]]). Only the per-player actions we can render exactly are supported
 * (Tinybones Joins Up: "discard a card"; "mill a card and lose 1 life"); any other inner action
 * declines so the card scaffolds rather than dropping a clause.
 */
internal fun EmitCtx.renderForEachTargetPlayerBody(node: JsonObject): Dsl? {
    val args = node["args"].asArr ?: return null
    // args[0] is the {_Players: Ref_TargetPlayers} scope; args[1] is a single action object or a list.
    val second = args.getOrNull(1)
    val innerActions: List<JsonObject> = when (second) {
        is JsonArray -> second.filterIsInstance<JsonObject>()
        is JsonObject -> listOf(second)
        else -> return null
    }
    if (innerActions.isEmpty()) return null
    val player = "EffectTarget.PlayerRef(Player.ContextPlayer(0))"
    val effects = innerActions.map { action ->
        when (action.strField("_Action")) {
            "DiscardACard" -> call("Effects.Discard", arg("1"), arg(Lit(player)))
            "MillACard" -> call("Patterns.Library.mill", arg("1"), arg(Lit(player)))
            "LoseLife" -> {
                val amt = amount(action["args"]) ?: return null
                call("Effects.LoseLife", arg(Lit(amt)), arg(Lit(player)))
            }
            else -> return null
        }
    }
    val list = call("listOf", *effects.map { arg(it) }.toTypedArray())
    return call("ForEachTargetEffect", arg(list))
}

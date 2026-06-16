package com.wingedsheep.tooling.coverage.emitter

import com.wingedsheep.tooling.coverage.Composite
import com.wingedsheep.tooling.coverage.Lit
import com.wingedsheep.tooling.coverage.amountNode
import com.wingedsheep.tooling.coverage.arg
import com.wingedsheep.tooling.coverage.asArr
import com.wingedsheep.tooling.coverage.call
import com.wingedsheep.tooling.coverage.findInteger
import com.wingedsheep.tooling.coverage.jsonContains
import com.wingedsheep.tooling.coverage.objects
import com.wingedsheep.tooling.coverage.strField
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** Direct effects on life totals, damage, and the player's own cards (draw / discard / look). */
internal val damageDrawLifeHandlers: Map<String, ActionHandler> = actionHandlers {

    on("SpellDealsDamage", "PermanentDealsDamage") { node, args, tvar ->
        val amt = amountExpr(args) ?: dynamicAmountExpr(amountNode(args)) ?: return@on null
        if (jsonContains(args, "_DamageRecipient", "EachPermanent")) {  // mass: deal N to each creature
            val filter = groupFilterExpr(args) ?: return@on null
            val eachPermanent = call(
                "Effects.ForEachInGroup", arg(filter),
                arg(call("DealDamageEffect", arg(amt), arg("EffectTarget.Self"))),
            )
            if (jsonContains(args, "_DamageRecipient", "EachPlayer")) {
                return@on Composite(listOf(
                    eachPermanent,
                    call(
                        "ForEachPlayerEffect", arg("Player.Each"),
                        arg(call("listOf", arg(call("DealDamageEffect", arg(amt), arg("EffectTarget.Controller"))))),
                    ),
                ))
            }
            return@on eachPermanent
        }
        val tgt = damageRecipientTarget(args, tvar) ?: return@on null
        // For `PermanentDealsDamage`, the acting permanent (the first arg's `_Permanent` ref) is the
        // damage source — when it's a bound target ("target creature you control … deals damage equal to
        // its power", Burrog Barrage), thread it through `damageSource = …` so the damage is attributed
        // correctly. `SpellDealsDamage` (the spell itself is the source) needs no source attribution.
        if (node.strField("_Action") == "PermanentDealsDamage") {
            val srcRef = (args.asArr?.firstOrNull() as? JsonObject)?.strField("_Permanent")
            val src = if (srcRef != null) refTargetFromRef(srcRef, tvar) else null
            // Only thread an EXPLICIT damage source — a BOUND TARGET other than the recipient (Burrog
            // Barrage's "target creature you control … deals damage equal to its power"). The implicit
            // `EffectTarget.Self` source (the acting permanent is `ThisPermanent`, the common Fire
            // Imp / Fire Dragon shape) is the engine default and must NOT be emitted, or it diverges
            // from golden trees that omit it.
            if (src != null && src != tgt && src != "EffectTarget.Self") {
                return@on call("DealDamageEffect", arg(amt), arg(Lit(tgt)), arg("damageSource", Lit(src)))
            }
        }
        call("DealDamageEffect", arg(amt), arg(Lit(tgt)))
    }

    on("SpellDealsDamageCantBePrevented") { _, args, tvar ->
        // "[This] deals N damage … that can't be prevented" (Pinpoint Avalanche). Same shape as
        // SpellDealsDamage, with the can't-be-prevented flag set on the DealDamageEffect.
        val amt = amountExpr(args) ?: dynamicAmountExpr(amountNode(args)) ?: return@on null
        val tgt = refTargetIn(args, "_DamageRecipient", tvar) ?: return@on null
        call("DealDamageEffect", arg(amt), arg(Lit(tgt)), arg("cantBePrevented", "true"))
    }

    on("HavePermanentDealDamage") { _, args, tvar ->
        // "<permanent> deals N damage to <recipient>" (Skirk Commando, Snapping Thragg, Aether Charge).
        // The acting permanent is the trigger source; the golden models it as a plain DealDamageEffect to
        // the recipient (the damage-source attribution isn't carried), so render that and decline anything
        // we can't resolve to a recipient exactly.
        val amt = amountExpr(args) ?: dynamicAmountExpr(amountNode(args)) ?: return@on null
        val tgt = refTargetIn(args, "_DamageRecipient", tvar) ?: return@on null
        call("DealDamageEffect", arg(amt), arg(Lit(tgt)))
    }

    on("ExiledCardDealsDamage") { _, args, tvar ->
        // "it deals N damage to <recipient>" where "it" is the card sitting face up in exile (Longhorn
        // Sharpshooter's "When this card becomes plotted, it deals 2 damage to any target"). The damage
        // source is the plotted card; the engine attributes it via the ability source (the default), so —
        // like HavePermanentDealDamage — the golden carries no damageSource. IR args are
        // [<CardInExile>, N, <recipient>]. Render only a fixed amount + an exactly-resolvable recipient.
        val amt = amountExpr(args) ?: dynamicAmountExpr(amountNode(args)) ?: return@on null
        val tgt = refTargetIn(args, "_DamageRecipient", tvar) ?: return@on null
        call("DealDamageEffect", arg(amt), arg(Lit(tgt)))
    }

    on("SpellDealsDistributedDamage") { _, args, _ ->
        val total = findInteger(args)
        if (total !is Int) return@on null
        call("DividedDamageEffect", arg("totalDamage", "$total"))
    }

    on("GainLifeForEach") { _, args, _ ->
        val dyn = dynamicAmountExpr(gainForEachAmount(args)) ?: return@on null
        call("GainLifeEffect", arg(dyn))
    }

    on("DrawNumberCards", "DrawACard") { node, args, _ ->
        // A fixed Integer / X renders directly; a recognised dynamic count (e.g. "draw a card for each
        // tapped creature target opponent controls") renders via dynamicAmount. A derived count we model
        // neither way (Mathemagics' "2ˣ") scaffolds rather than emit a misleading literal dug out of the
        // expression — strictCardCount reads only the TOP-LEVEL game number so it can't be fooled.
        val amt = if (node.strField("_Action") == "DrawACard") "1"
                  else strictCardCount(amountNode(args), forX = "DynamicAmount.XValue")
                      ?: dynamicAmount(amountNode(args))
        if (amt != null) call("DrawCardsEffect", arg(Lit(amt))) else null
    }

    on("PlayerAction") { node, _, tvar ->
        val arr = node["args"] as? JsonArray ?: return@on null
        val playerRef = arr.firstOrNull() as? JsonObject
        val action = arr.firstOrNull { it is JsonObject && it.containsKey("_Action") } as? JsonObject ?: return@on null
        val targetsRef = jsonContains(playerRef, "_Player", "Ref_TargetPlayer")
        when (action.strField("_Action")) {
            "RevealHand" -> {
                val target = if (targetsRef) tvar else null
                if (target != null) call("RevealHandEffect", arg(Lit(target))) else call("RevealHandEffect")
            }
            // "Target player discards a card at random" (Mindwhip Sliver). discardRandom is a fixed,
            // no-choice discard (the engine picks), so it renders exactly against the target player.
            "DiscardACardAtRandom" -> {
                if (!targetsRef || tvar == null) return@on null
                call("Patterns.Hand.discardRandom", arg("1"), arg(Lit(tvar)))
            }
            else -> null
        }
    }

    simple("CounterSpell", dsl = "CounterEffect()")
    // "Copy target instant/sorcery spell, activated ability, or triggered ability. You may choose new
    // targets for the copy." (Return the Favor mode 1). The four-way target is recovered by
    // `Targets.InstantSorcerySpellOrAbility`; the effect copies whichever stack-object kind was chosen.
    simple("CopySpellOrAbilityAndMayChooseNewTargets", dsl = "Effects.CopyTargetSpellOrAbility()")
    // "Copy that spell" on a cast trigger — CopySpell(Trigger_ThatSpell) (Double Down). The copy is
    // made of the triggering spell itself; copies of permanent spells become tokens (handled by the
    // engine's copy executor). Only the triggering-spell subject renders; any other CopySpell subject
    // (a chosen target, a stored spell) declines -> SCAFFOLD.
    on("CopySpell") { node, _, _ ->
        val subject = (node["args"] as? JsonObject)?.strField("_Spell")
            ?: (node["args"].asArr?.firstOrNull() as? JsonObject)?.strField("_Spell")
        if (subject == "Trigger_ThatSpell")
            Lit("Effects.CopyTargetSpell(target = EffectTarget.TriggeringEntity)")
        else null
    }
    // "Change the target of target spell or ability with a single target." (Return the Favor mode 2 /
    // Willbender). Pairs with `Targets.SpellOrAbilityWithSingleTarget`.
    simple("ChangeTargetsOfSpellOrAbility", dsl = "Effects.ChangeTarget()")
    simple("Shuffle", dsl = "ShuffleLibraryEffect()")
    // Investigate (keyword action, CR 701.36): create a Clue token. Argument-free constant action
    // (Malcolm, the Eyes — "investigate"). "Investigate N times" appears as N stacked actions.
    simple("Investigate", dsl = "Effects.Investigate()")
    simple("TakeAnExtraTurn", dsl = "TakeExtraTurnEffect()")
    simple("DiscardACardAtRandom", dsl = "Patterns.Hand.discardRandom(1)")

    on("GainLife") { _, args, _ ->
        // A fixed Integer renders `GainLifeEffect(1)`; a recognised dynamic amount (e.g. a damage
        // trigger's "gain that much life") renders `GainLifeEffect(DynamicAmount…)`.
        val amt = lifeAmountExpr(args) ?: return@on null
        call("GainLifeEffect", arg(amt))
    }
    on("LoseLife") { _, args, _ ->
        val amt = lifeAmountExpr(args) ?: return@on null
        call("LoseLifeEffect", arg(amt), arg("EffectTarget.Controller"))
    }

    on("DiscardACard", "DiscardNumberCards", "DiscardAnyNumberOfCards") { node, args, _ ->
        // discardCards takes a fixed Int; a derived/X count can't be expressed, so scaffold.
        val n = if (node.strField("_Action") == "DiscardACard") "1" else strictCardCount(amountNode(args))
        if (n != null) call("Patterns.Hand.discardCards", arg(Lit(n))) else null
    }

    on("LookAtPlayersHand") { _, args, tvar ->
        val tgt = refTarget(args, tvar)
        if (tgt != null) call("LookAtTargetHandEffect", arg(Lit(tgt))) else call("LookAtTargetHandEffect")
    }

    // "{cost}: The next time you would draw a card this turn, [do X] instead." (the Onslaught Words
    // cycle.) Only the controller's-own-next-draw shape maps to ReplaceNextDraw; the replacement
    // action(s) reuse the normal action vocabulary under a `_ReplacementActionWouldDraw` discriminator,
    // so [asReplacementActions] rekeys them and the shared action renderer renders the inner effect.
    // (The activated-ability emitter pairs this with `promptOnDraw = true`.)
    on("CreateFutureReplaceWouldDraw") { _, args, tvar ->
        val a = args.asArr ?: return@on null
        val event = a.getOrNull(0)
        if (!jsonContains(event, "_FutureReplacableEventWouldDraw", "NextTimePlayerWouldDrawACardThisTurn") ||
            !jsonContains(event, "_Player", "You")) return@on null
        // A targeted replacement (Words of War's "deals 2 damage to any target") threads the ability's
        // bound target into the deferred effect; the golden labels that target with card-specific prompt
        // text we can't reconstruct, so decline rather than emit a non-equivalent target name.
        if (tvar != null) return@on null
        val inner = asReplacementActions(a.getOrNull(1)) ?: return@on null
        val edsl = renderEffectList(inner, tvar) ?: return@on null
        call("Effects.ReplaceNextDraw", arg(edsl))
    }

    // "{cost}: Prevent the next N damage that would be dealt to <recipient> this turn." (Daru Healer,
    // Samite Healer, the Circles' "to you" mode, Healing Salve, …) — the damage twin of the draw
    // replacement above. Only the fixed-amount "prevent that damage" shape maps to PreventNextDamage:
    //  - the event is the next-AMOUNT-to-recipient form (the next-TIME / by-source / distributed events
    //    are different prevention shapes we don't render here),
    //  - the amount is a literal (a cast-time `X` prevention can't be threaded — see the creator's note),
    //  - the single replacement is PreventThatDamage (redirect / gain-life / instead variants scaffold),
    //  - the recipient resolves to a target we render exactly.
    on("CreateFutureReplaceWouldDealDamage") { _, args, tvar ->
        val a = args.asArr ?: return@on null
        val event = a.getOrNull(0) as? JsonObject ?: return@on null
        if (!jsonContains(event, "_FutureReplacableEventWouldDealDamage",
                "NextAmountOfDamageThatWouldBeDealtThisTurnToRecipient")) return@on null
        val repl = a.getOrNull(1) as? JsonArray ?: return@on null
        if (repl.size != 1 ||
            (repl[0] as? JsonObject)?.strField("_ReplacementActionWouldDealDamage") != "PreventThatDamage")
            return@on null
        val amount = findInteger(event) as? Int ?: return@on null  // X / "all" -> SCAFFOLD
        val target = damagePreventionRecipient(event, tvar) ?: return@on null
        call("Effects.PreventNextDamage", arg("$amount"), arg(Lit(target)))
    }

    // PREVENTION twin of CreateFutureReplaceWouldDealDamage (mtgish prevention/replacement split). Same
    // "Prevent the next N damage that would be dealt to <recipient> this turn" render as above. Verified
    // against the post-split IR: the split renamed the discriminators but preserved the structure —
    //   event field `_FutureReplacableEventWouldDealDamage` -> `_FutureEventPreventDamage` (value unchanged),
    //   action field `_ReplacementActionWouldDealDamage`    -> `_ActionPreventDamage`        (value unchanged).
    // The args[1] action payload was NOT dropped, so we keep the single-`PreventThatDamage` guard: a
    // prevention-with-rider (redirect / gain-life / "if you do …") has a different/extra action and must
    // SCAFFOLD rather than mis-render as a plain shield. Literal-amount + exactly-resolvable-recipient
    // constraints unchanged (X / "all" / distributed / by-source events decline).
    on("CreateFuturePreventDamage") { _, args, tvar ->
        val a = args.asArr ?: return@on null
        val event = a.getOrNull(0) as? JsonObject ?: return@on null
        if (!jsonContains(event, "_FutureEventPreventDamage",
                "NextAmountOfDamageThatWouldBeDealtThisTurnToRecipient")) return@on null
        val repl = a.getOrNull(1) as? JsonArray ?: return@on null
        if (repl.size != 1 ||
            (repl[0] as? JsonObject)?.strField("_ActionPreventDamage") != "PreventThatDamage")
            return@on null
        val amount = findInteger(event) as? Int ?: return@on null  // X / "all" -> SCAFFOLD
        val target = damagePreventionRecipient(event, tvar) ?: return@on null
        call("Effects.PreventNextDamage", arg("$amount"), arg(Lit(target)))
    }
}

/** Resolve a `_DamageRecipient` node to an EffectTarget DSL for a direct deal-damage action.
 *
 *  First tries the shared bound-ref / self resolver ([refTargetIn]) — preserving every recipient that
 *  already renders. As a fallback it resolves the explicit, non-ref recipient shapes the ref resolver
 *  doesn't cover: a plain "you" recipient (`Player{You}` -> the controller, e.g. Jinxed Idol's "deals 2
 *  damage to you") and "this permanent" (`Permanent{ThisPermanent}` -> self). Returns null for anything
 *  else (a named/opponent player recipient with no target binding, distributed, …) so the card scaffolds
 *  rather than deal damage to the wrong recipient. */
internal fun EmitCtx.damageRecipientTarget(args: JsonElement?, tvar: String?): String? {
    // "each opponent" recipient: `_DamageRecipient: EachPlayer` with a plural `_Players: Opponent` scope
    // ("this token deals 1 damage to each opponent"). [findRef] only reads the SINGULAR `_Player`, so the
    // plural scope yields no ref and [refTargetFromRef] would fall through to its `tvar` fallback — which,
    // for a token's granted sub-ability (tvar = EffectTarget.Self), silently makes the token damage ITSELF
    // instead of each opponent. Resolve the each-opponent scope here, before the ref fallback can misfire.
    recipientNode(args)?.takeIf { it.strField("_DamageRecipient") == "EachPlayer" }?.let { recip ->
        return if (jsonContains(recip["args"], "_Players", "Opponent")) "EffectTarget.PlayerRef(Player.EachOpponent)"
        else null  // "each player" and other scopes have no exact single-target render — decline -> SCAFFOLD
    }
    refTargetIn(args, "_DamageRecipient", tvar)?.let { return it }
    val recip = recipientNode(args) ?: return null
    return when (recip.strField("_DamageRecipient")) {
        "Player" -> when (recip["args"].strField("_Player")) {
            "You" -> "EffectTarget.Controller"
            else -> null
        }
        "Permanent" -> when (recip["args"].strField("_Permanent")) {
            "ThisPermanent" -> "EffectTarget.Self"
            else -> null
        }
        else -> null
    }
}

/** The first object carrying a `_DamageRecipient` discriminator anywhere in a deal-damage action's args. */
private fun recipientNode(args: JsonElement?): JsonObject? {
    var found: JsonObject? = null
    fun walk(n: JsonElement?) {
        if (found != null) return
        when (n) {
            is JsonObject -> {
                if (n.containsKey("_DamageRecipient")) { found = n; return }
                n.values.forEach { walk(it) }
            }
            is JsonArray -> n.forEach { walk(it) }
            else -> {}
        }
    }
    walk(args)
    return found
}

/** Resolve the damage recipient of a `NextAmountOfDamage…ToRecipient` event to an EffectTarget DSL:
 *  a bound target ref -> the ability's target local [tvar]; "you" -> the controller; this permanent ->
 *  self. Returns null for recipients we can't render exactly (distributed, host permanent, or a bound
 *  target ref with no target rendered), so the card scaffolds rather than prevent damage to the wrong
 *  thing. */
private fun damagePreventionRecipient(event: JsonObject, tvar: String?): String? {
    val recip = event.objects().firstOrNull { it.containsKey("_SingleDamageRecipient") } ?: return null
    return when (recip.strField("_SingleDamageRecipient")) {
        "Ref_AnyTarget", "Ref_TargetPlayerOrPermanent" -> tvar
        "Permanent" -> when (recip["args"].strField("_Permanent")) {
            "Ref_TargetPermanent" -> tvar
            "ThisPermanent" -> "EffectTarget.Self"
            else -> null
        }
        "Player" -> when (recip["args"].strField("_Player")) {
            "You" -> "EffectTarget.Controller"
            "Ref_TargetPlayer" -> tvar
            else -> null
        }
        else -> null  // DistributedAnyTarget, host permanent, …
    }
}

/** Rekey a `_ReplacementActionWouldDraw` list into the normal `_Action` vocabulary so the shared action
 *  renderer can render it: the replacement actions ARE the ordinary actions (GainLife, CreateTokens,
 *  PermanentDealsDamage, …) under a different discriminator, plus `_DamageRecipients` for the singular
 *  `_DamageRecipient` the damage handler reads. Returns null if the arg isn't an action list. */
private fun asReplacementActions(node: JsonElement?): List<JsonObject>? =
    (node as? JsonArray)?.map { rekeyReplacementDraw(it) }?.filterIsInstance<JsonObject>()

private fun rekeyReplacementDraw(el: JsonElement): JsonElement = when (el) {
    is JsonObject -> buildJsonObject {
        for ((k, v) in el) {
            val nk = when (k) {
                "_ReplacementActionWouldDraw" -> "_Action"
                "_DamageRecipients" -> "_DamageRecipient"
                else -> k
            }
            put(nk, rekeyReplacementDraw(v))
        }
    }
    is JsonArray -> buildJsonArray { el.forEach { add(rekeyReplacementDraw(it)) } }
    else -> el
}

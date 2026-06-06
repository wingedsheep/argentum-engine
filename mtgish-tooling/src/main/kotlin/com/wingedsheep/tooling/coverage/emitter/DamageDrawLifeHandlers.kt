package com.wingedsheep.tooling.coverage.emitter

import com.wingedsheep.tooling.coverage.amountNode
import com.wingedsheep.tooling.coverage.findInteger
import com.wingedsheep.tooling.coverage.jsonContains
import com.wingedsheep.tooling.coverage.strField
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject

/** Direct effects on life totals, damage, and the player's own cards (draw / discard / look). */
internal val damageDrawLifeHandlers: Map<String, ActionHandler> = actionHandlers {

    on("SpellDealsDamage", "PermanentDealsDamage") { _, args, tvar ->
        val amt = amount(args) ?: dynamicAmount(amountNode(args)) ?: return@on null
        if (jsonContains(args, "_DamageRecipient", "EachPermanent")) {  // mass: deal N to each creature
            val filter = groupFilterDsl(args) ?: return@on null
            val eachPermanent = "ForEachInGroupEffect($filter, DealDamageEffect($amt, EffectTarget.Self))"
            if (jsonContains(args, "_DamageRecipient", "EachPlayer")) {
                return@on composite(listOf(
                    eachPermanent,
                    "ForEachPlayerEffect(Player.Each, listOf(DealDamageEffect($amt, EffectTarget.Controller)))",
                ))
            }
            return@on eachPermanent
        }
        val tgt = refTargetIn(args, "_DamageRecipient", tvar) ?: return@on null
        "DealDamageEffect($amt, $tgt)"
    }

    on("SpellDealsDistributedDamage") { _, args, _ ->
        val total = findInteger(args)
        if (total !is Int) return@on null
        "DividedDamageEffect(totalDamage = $total)"
    }

    on("GainLifeForEach") { _, args, _ ->
        val dyn = dynamicAmount(gainForEachAmount(args)) ?: return@on null
        "GainLifeEffect($dyn)"
    }

    on("DrawNumberCards", "DrawACard") { node, args, _ ->
        val amt = if (node.strField("_Action") == "DrawACard") "1" else (amount(args) ?: dynamicAmount(amountNode(args)))
        if (amt != null) "DrawCardsEffect($amt)" else null
    }

    on("PlayerAction") { node, _, tvar ->
        val arr = node["args"] as? JsonArray ?: return@on null
        val playerRef = arr.firstOrNull() as? JsonObject
        val action = arr.firstOrNull { it is JsonObject && it.containsKey("_Action") } as? JsonObject ?: return@on null
        if (action.strField("_Action") != "RevealHand") return@on null
        val target = if (jsonContains(playerRef, "_Player", "Ref_TargetPlayer")) tvar else null
        if (target != null) "RevealHandEffect($target)" else "RevealHandEffect()"
    }

    simple("CounterSpell", dsl = "CounterEffect()")
    simple("Shuffle", dsl = "ShuffleLibraryEffect()")
    simple("TakeAnExtraTurn", dsl = "TakeExtraTurnEffect()")
    simple("DiscardACardAtRandom", dsl = "EffectPatterns.discardRandom(1)")

    on("GainLife") { _, args, _ ->
        val amt = amount(args) ?: return@on null
        "GainLifeEffect($amt)"
    }
    on("LoseLife") { _, args, _ ->
        val amt = amount(args) ?: dynamicAmount(amountNode(args)) ?: return@on null
        "LoseLifeEffect($amt, EffectTarget.Controller)"
    }

    on("DiscardACard", "DiscardNumberCards", "DiscardAnyNumberOfCards") { _, args, _ ->
        "EffectPatterns.discardCards(${(findInteger(args) as? Int) ?: 1})"
    }

    on("LookAtPlayersHand") { _, args, tvar ->
        val tgt = refTarget(args, tvar)
        if (tgt != null) "LookAtTargetHandEffect($tgt)" else "LookAtTargetHandEffect()"
    }
}

package com.wingedsheep.tooling.coverage.emitter

import com.wingedsheep.tooling.coverage.amountNode
import com.wingedsheep.tooling.coverage.findInteger
import com.wingedsheep.tooling.coverage.jsonContains
import com.wingedsheep.tooling.coverage.strField
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject

/** Direct effects on life totals, damage, and the player's own cards (draw / discard / look). */
internal val damageDrawLifeHandlers: Map<String, ActionHandler> = buildMap {
    fun reg(vararg keys: String, h: ActionHandler) = keys.forEach { put(it, h) }

    reg("SpellDealsDamage", "PermanentDealsDamage") { _, args, tvar ->
        val amt = amount(args) ?: dynamicAmount(amountNode(args)) ?: return@reg null
        if (jsonContains(args, "_DamageRecipient", "EachPermanent")) {  // mass: deal N to each creature
            used.addAll(listOf("ForEachInGroupEffect", "DealDamageEffect", "EffectTarget"))
            val filter = groupFilterDsl(args) ?: return@reg null
            val eachPermanent = "ForEachInGroupEffect($filter, DealDamageEffect($amt, EffectTarget.Self))"
            if (jsonContains(args, "_DamageRecipient", "EachPlayer")) {
                used.addAll(listOf("CompositeEffect", "ForEachPlayerEffect", "Player"))
                return@reg "CompositeEffect(\n" +
                    "        listOf(\n" +
                    "            $eachPermanent,\n" +
                    "            ForEachPlayerEffect(Player.Each, listOf(DealDamageEffect($amt, EffectTarget.Controller)))\n" +
                    "        )\n" +
                    "    )"
            }
            return@reg eachPermanent
        }
        val tgt = refTargetIn(args, "_DamageRecipient", tvar) ?: return@reg null
        used.add("DealDamageEffect")
        "DealDamageEffect($amt, $tgt)"
    }

    reg("SpellDealsDistributedDamage") { _, args, _ ->
        val total = findInteger(args)
        if (total !is Int) return@reg null
        used.add("DividedDamageEffect")
        "DividedDamageEffect(totalDamage = $total)"
    }

    reg("GainLifeForEach") { _, args, _ ->
        val dyn = dynamicAmount(gainForEachAmount(args)) ?: return@reg null
        used.add("GainLifeEffect")
        "GainLifeEffect($dyn)"
    }

    reg("DrawNumberCards", "DrawACard") { node, args, _ ->
        used.add("DrawCardsEffect")
        val amt = if (node.strField("_Action") == "DrawACard") "1" else (amount(args) ?: dynamicAmount(amountNode(args)))
        if (amt != null) "DrawCardsEffect($amt)" else null
    }

    reg("PlayerAction") { node, _, tvar ->
        val arr = node["args"] as? JsonArray ?: return@reg null
        val playerRef = arr.firstOrNull() as? JsonObject
        val action = arr.firstOrNull { it is JsonObject && it.containsKey("_Action") } as? JsonObject ?: return@reg null
        if (action.strField("_Action") != "RevealHand") return@reg null
        val target = if (jsonContains(playerRef, "_Player", "Ref_TargetPlayer")) tvar else null
        used.add("RevealHandEffect")
        if (target != null) "RevealHandEffect($target)" else "RevealHandEffect()"
    }

    reg("CounterSpell") { _, _, _ -> used.add("CounterEffect"); "CounterEffect()" }
    reg("Shuffle") { _, _, _ -> used.add("ShuffleLibraryEffect"); "ShuffleLibraryEffect()" }

    reg("GainLife") { _, args, _ ->
        val amt = amount(args) ?: return@reg null
        used.add("GainLifeEffect")
        "GainLifeEffect($amt)"
    }
    reg("LoseLife") { _, args, _ ->
        val amt = amount(args) ?: dynamicAmount(amountNode(args)) ?: return@reg null
        used.addAll(listOf("LoseLifeEffect", "EffectTarget"))
        "LoseLifeEffect($amt, EffectTarget.Controller)"
    }

    reg("DiscardACard", "DiscardNumberCards", "DiscardAnyNumberOfCards") { _, args, _ ->
        used.add("EffectPatterns")
        "EffectPatterns.discardCards(${(findInteger(args) as? Int) ?: 1})"
    }
    reg("DiscardACardAtRandom") { _, _, _ -> used.add("EffectPatterns"); "EffectPatterns.discardRandom(1)" }

    reg("LookAtPlayersHand") { _, args, tvar ->
        val tgt = refTarget(args, tvar)
        used.add("LookAtTargetHandEffect")
        if (tgt != null) "LookAtTargetHandEffect($tgt)" else "LookAtTargetHandEffect()"
    }
    reg("TakeAnExtraTurn") { _, _, _ -> used.add("TakeExtraTurnEffect"); "TakeExtraTurnEffect()" }
}

package com.wingedsheep.mtg.sets.definitions.mat.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Feast of the Victorious Dead — {W}{B} Enchantment
 *
 * At the beginning of your end step, if one or more creatures died this turn, you gain that much
 * life and distribute that many +1/+1 counters among any number of creatures you control.
 *
 * "That much" counts every creature (tokens included) that died this turn under any player's
 * control, read on resolution; the intervening-if (CR 603.4) is re-checked then as well.
 */
val FeastOfTheVictoriousDead = card("Feast of the Victorious Dead") {
    manaCost = "{W}{B}"
    colorIdentity = "WB"
    typeLine = "Enchantment"
    oracleText = "At the beginning of your end step, if one or more creatures died this turn, you gain " +
        "that much life and distribute that many +1/+1 counters among any number of creatures you control."

    val died = DynamicAmounts.creaturesDiedThisTurn(Player.Each)
    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.END)
        interveningIf = Conditions.CompareAmounts(died, ComparisonOperator.GTE, 1)
        effect = Effects.GainLife(died) then
            Effects.DistributeCountersAmongFiltered(died, filter = GameObjectFilter.Creature.youControl())
        description = "At the beginning of your end step, if one or more creatures died this turn, you " +
            "gain that much life and distribute that many +1/+1 counters among any number of creatures " +
            "you control."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "30"
        artist = "Alexey Kruglov"
        imageUri = "https://cards.scryfall.io/normal/front/8/a/8a90d613-a327-4dc7-b0c1-0003fe0171ef.jpg?1783916516"
    }
}

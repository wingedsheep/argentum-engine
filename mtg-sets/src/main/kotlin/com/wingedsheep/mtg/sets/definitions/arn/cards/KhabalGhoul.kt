package com.wingedsheep.mtg.sets.definitions.arn.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Khabál Ghoul
 * {2}{B}
 * Creature — Zombie
 * 1/1
 * At the beginning of each end step, put a +1/+1 counter on this creature for each
 * creature that died this turn.
 */
val KhabalGhoul = card("Khabál Ghoul") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Zombie"
    power = 1
    toughness = 1
    oracleText = "At the beginning of each end step, put a +1/+1 counter on this creature for each creature that died this turn."

    triggeredAbility {
        trigger = Triggers.EachEndStep
        effect = Effects.AddDynamicCounters(
            Counters.PLUS_ONE_PLUS_ONE,
            DynamicAmount.Add(
                DynamicAmounts.creaturesDiedThisTurn(Player.You),
                DynamicAmounts.creaturesDiedThisTurn(Player.EachOpponent),
            ),
            EffectTarget.Self,
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "30"
        artist = "Douglas Shuler"
        imageUri = "https://cards.scryfall.io/normal/front/1/8/18607bf6-ce11-41cb-b001-0c9538406ba0.jpg?1562899601"
    }
}

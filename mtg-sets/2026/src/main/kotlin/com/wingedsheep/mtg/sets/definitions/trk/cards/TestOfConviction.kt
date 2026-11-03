package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Test of Conviction
 * {1}{W}
 * Sorcery
 *
 * Choose one —
 * • Battle Stations — Put a +1/+1 counter on each creature you control.
 * • Begin Negotiations — Target opponent draws a card. You draw two cards.
 *
 * Choosing the mode is facing a dilemma, which the engine raises for any modal spell — no extra
 * wiring needed here.
 */
val TestOfConviction = card("Test of Conviction") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"
    oracleText = "Choose one —\n" +
        "• Battle Stations — Put a +1/+1 counter on each creature you control.\n" +
        "• Begin Negotiations — Target opponent draws a card. You draw two cards."

    spell {
        modal(chooseCount = 1) {
            mode("Battle Stations — Put a +1/+1 counter on each creature you control") {
                effect = Effects.ForEachInGroup(
                    GroupFilter(GameObjectFilter.Creature.youControl()),
                    Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.IterationEntity)
                )
            }

            mode("Begin Negotiations — Target opponent draws a card. You draw two cards") {
                val opponent = target(Targets.Opponent)
                effect = Effects.DrawCards(1, opponent) then Effects.DrawCards(2)
            }
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "39"
        artist = "John Tyler Christopher"
        flavorText = "\"In our next action, we can risk neither miscalculation nor error.\"\n—Captain James T. Kirk"
        imageUri = "https://cards.scryfall.io/normal/front/8/3/837d7cec-f3f8-4555-86f2-c5ea1849d7d1.jpg?1792668317"
    }
}

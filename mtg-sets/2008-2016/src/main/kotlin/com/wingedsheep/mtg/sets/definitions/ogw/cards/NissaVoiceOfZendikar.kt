package com.wingedsheep.mtg.sets.definitions.ogw.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Nissa, Voice of Zendikar
 * {1}{G}{G}
 * Legendary Planeswalker — Nissa
 * Loyalty 3
 * +1: Create a 0/1 green Plant creature token.
 * −2: Put a +1/+1 counter on each creature you control.
 * −7: You gain X life and draw X cards, where X is the number of lands you control.
 *
 * +1 is Avenger of Zendikar's Plant token. −2 is Ajani Goldmane's counter-on-each-creature
 * `ForEachInGroup`. −7 is Joyous Respite's lands-you-control life gain followed by a draw of the
 * same count.
 */
val NissaVoiceOfZendikar = card("Nissa, Voice of Zendikar") {
    manaCost = "{1}{G}{G}"
    colorIdentity = "G"
    typeLine = "Legendary Planeswalker — Nissa"
    startingLoyalty = 3
    oracleText = "+1: Create a 0/1 green Plant creature token.\n" +
        "−2: Put a +1/+1 counter on each creature you control.\n" +
        "−7: You gain X life and draw X cards, where X is the number of lands you control."

    loyaltyAbility(+1) {
        effect = Effects.CreateToken(
            power = 0,
            toughness = 1,
            colors = setOf(Color.GREEN),
            creatureTypes = setOf("Plant"),
            imageUri = "https://cards.scryfall.io/normal/front/8/5/8501a910-e181-4f85-ad34-d2a099257148.jpg?1783937858",
        )
        description = "Create a 0/1 green Plant creature token."
    }

    loyaltyAbility(-2) {
        effect = Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Creature.youControl()),
            Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.IterationEntity),
        )
        description = "Put a +1/+1 counter on each creature you control."
    }

    loyaltyAbility(-7) {
        effect = Effects.GainLife(DynamicAmounts.landsYouControl()) then
            Effects.DrawCards(DynamicAmounts.landsYouControl())
        description = "You gain X life and draw X cards, where X is the number of lands you control."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "138"
        artist = "Raymond Swanland"
        imageUri = "https://cards.scryfall.io/normal/front/a/6/a600b97b-1851-4b1b-a2af-f80e0307c02c.jpg?1783937901"
    }
}

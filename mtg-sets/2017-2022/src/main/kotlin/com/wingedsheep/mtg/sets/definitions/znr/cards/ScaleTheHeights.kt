package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Scale the Heights {2}{G}
 * Sorcery
 *
 * Put a +1/+1 counter on up to one target creature. You gain 2 life. You may play an additional
 * land this turn.
 * Draw a card.
 */
val ScaleTheHeights = card("Scale the Heights") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Put a +1/+1 counter on up to one target creature. You gain 2 life. " +
        "You may play an additional land this turn.\nDraw a card."

    spell {
        val creature = target(TargetFilter.Creature, optional = true)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, creature) then
            Effects.GainLife(2) then
            Effects.PlayAdditionalLands(count = 1) then
            Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "202"
        artist = "Cristi Balanescu"
        flavorText = "Ever onward, ever skyward."
        imageUri = "https://cards.scryfall.io/normal/front/e/7/e781d6ad-950e-49bf-9645-e2354086ae49.jpg?1783929332"
        ruling(
            "2020-09-25",
            "If you choose a target creature and it's an illegal target by the time Scale the Heights " +
                "tries to resolve, the spell doesn't resolve. You don't gain life, draw a card, or get to " +
                "play an additional land.",
        )
        ruling(
            "2020-09-25",
            "The permission to play lands is cumulative with other effects that allow you to play " +
                "additional lands, such as that of Nahiri's Lithoforming.",
        )
        ruling(
            "2020-09-25",
            "You don't play a land as Scale the Heights resolves; Scale the Heights fully resolves first " +
                "and you draw a card, perhaps including a land you'll play later. If it's not your turn, " +
                "you won't be able to play a land this turn at all.",
        )
    }
}

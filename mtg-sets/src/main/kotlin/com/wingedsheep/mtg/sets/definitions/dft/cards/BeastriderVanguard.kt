package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Beastrider Vanguard
 * {1}{G}
 * Creature — Human Knight
 * 2/2
 * {4}{G}: Look at the top three cards of your library. You may reveal a permanent card from among
 * them and put it into your hand. Put the rest on the bottom of your library in any order.
 */
val BeastriderVanguard = card("Beastrider Vanguard") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Human Knight"
    power = 2
    toughness = 2
    oracleText = "{4}{G}: Look at the top three cards of your library. You may reveal a permanent " +
        "card from among them and put it into your hand. Put the rest on the bottom of your library " +
        "in any order."

    activatedAbility {
        cost = Costs.Mana("{4}{G}")
        effect = Patterns.Library.lookAtTopRevealMatchingToHand(
            count = DynamicAmount.Fixed(3),
            filter = GameObjectFilter.Permanent,
            prompt = "You may reveal a permanent card to put into your hand",
            restOrder = CardOrder.ControllerChooses
        )
        description = "{4}{G}: Look at the top three cards of your library. You may reveal a " +
            "permanent card from among them and put it into your hand. Put the rest on the bottom " +
            "of your library in any order."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "154"
        artist = "Andrey Kuzinskiy"
        flavorText = "\"She knows what I'm thinking before I do. That is what true partnership means.\""
        imageUri = "https://cards.scryfall.io/normal/front/f/4/f4b99a61-49f1-46a2-9eb7-b6c88c236215.jpg?1782687839"
    }
}

package com.wingedsheep.mtg.sets.definitions.ori.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Evolutionary Leap
 * {1}{G}
 * Enchantment
 * {G}, Sacrifice a creature: Reveal cards from the top of your library until you reveal a creature
 * card. Put that card into your hand and the rest on the bottom of your library in a random order.
 */
val EvolutionaryLeap = card("Evolutionary Leap") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment"
    oracleText = "{G}, Sacrifice a creature: Reveal cards from the top of your library until you " +
        "reveal a creature card. Put that card into your hand and the rest on the bottom of your " +
        "library in a random order."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{G}"), Costs.Sacrifice(GameObjectFilter.Creature))
        effect = Patterns.Library.revealUntilMatchToHand(filter = GameObjectFilter.Creature)
        description = "{G}, Sacrifice a creature: Reveal cards from the top of your library until " +
            "you reveal a creature card. Put that card into your hand and the rest on the bottom " +
            "of your library in a random order."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "176"
        artist = "Chris Rahn"
        flavorText = "The essence of nature is change."
        imageUri = "https://cards.scryfall.io/normal/front/c/2/c27f3193-3083-409e-99d8-10f5b1afe9f1.jpg?1783938323"
        ruling(
            "2021-03-19",
            "If you don't reveal a creature card, you'll reveal all the cards from your library " +
                "and then put them back in your library in a random order."
        )
    }
}

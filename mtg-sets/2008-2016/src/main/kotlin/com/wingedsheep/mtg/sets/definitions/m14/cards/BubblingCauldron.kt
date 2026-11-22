package com.wingedsheep.mtg.sets.definitions.m14.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Bubbling Cauldron
 * {2}
 * Artifact
 * {1}, {T}, Sacrifice a creature: You gain 4 life.
 * {1}, {T}, Sacrifice a creature named Festering Newt: Each opponent loses 4 life.
 * You gain life equal to the life lost this way.
 */
val BubblingCauldron = card("Bubbling Cauldron") {
    manaCost = "{2}"
    typeLine = "Artifact"
    oracleText = "{1}, {T}, Sacrifice a creature: You gain 4 life.\n" +
        "{1}, {T}, Sacrifice a creature named Festering Newt: Each opponent loses 4 life. " +
        "You gain life equal to the life lost this way."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}"), Costs.Tap, Costs.Sacrifice(GameObjectFilter.Creature))
        effect = Effects.GainLife(4)
    }

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{1}"),
            Costs.Tap,
            Costs.Sacrifice(GameObjectFilter.Creature.named("Festering Newt")),
        )
        effect = Effects.DrainLife(4)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "205"
        artist = "Eric Deschamps"
        imageUri = "https://cards.scryfall.io/normal/front/9/a/9af87c24-a534-462b-968b-dccf6ac63299.jpg?1783939897"
    }
}

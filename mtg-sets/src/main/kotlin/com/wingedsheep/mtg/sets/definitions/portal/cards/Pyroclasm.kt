package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.DealDamageToAllCreaturesEffect

/**
 * Pyroclasm
 * {1}{R}
 * Sorcery
 * Pyroclasm deals 2 damage to each creature.
 */
val Pyroclasm = card("Pyroclasm") {
    manaCost = "{1}{R}"
    typeLine = "Sorcery"

    spell {
        effect = DealDamageToAllCreaturesEffect(2)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "143"
        artist = "John Avon"
        flavorText = "A wave of fire sweeps across the land."
        imageUri = "https://cards.scryfall.io/normal/front/6/d/6de3f4a5-b6c7-d8e9-f0a1-b2c3d4e5f6a7.jpg"
    }
}

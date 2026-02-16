package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.PlayFromTopOfLibrary

/**
 * Future Sight
 * {2}{U}{U}{U}
 * Enchantment
 * Play with the top card of your library revealed.
 * You may play lands and cast spells from the top of your library.
 */
val FutureSight = card("Future Sight") {
    manaCost = "{2}{U}{U}{U}"
    typeLine = "Enchantment"
    oracleText = "Play with the top card of your library revealed.\nYou may play lands and cast spells from the top of your library."

    staticAbility {
        ability = PlayFromTopOfLibrary
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "84"
        artist = "Matt Cavotta"
        flavorText = "\"My past holds only pain and loss. I will conquer it by creating a glorious future.\"\n—Ixidor, reality sculptor"
        imageUri = "https://cards.scryfall.io/large/front/a/2/a2a4c290-0a77-40da-840e-b4e8d7043850.jpg?1562929061"
    }
}

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
        imageUri = "https://cards.scryfall.io/normal/front/6/8/688bd665-4948-4961-aec5-f17782257f9b.jpg?1562919624"
    }
}

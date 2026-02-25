package com.wingedsheep.mtg.sets.definitions.scourge.cards

import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card

val LongTermPlans = card("Long-Term Plans") {
    manaCost = "{2}{U}"
    typeLine = "Instant"
    oracleText = "Search your library for a card, then shuffle and put that card third from the top."

    spell {
        effect = Effects.SearchLibraryNthFromTop(positionFromTop = 2)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "38"
        artist = "Ben Thompson"
        flavorText = "\"Wait, it'll come to me in a minute.\""
        imageUri = "https://cards.scryfall.io/normal/front/7/e/7e0422d9-9694-45b6-9c2b-2ca31198cebf.jpg?1562531196"
        ruling("2004-10-04", "If there are fewer than 3 cards in your library, put the card on the bottom of your library.")
    }
}

package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.SearchDestination

/**
 * Gift of Estates
 * {1}{W}
 * Sorcery
 * If an opponent controls more lands than you, search your library for up to
 * three Plains cards, reveal them, put them into your hand, then shuffle.
 */
val GiftOfEstates = card("Gift of Estates") {
    manaCost = "{1}{W}"
    typeLine = "Sorcery"

    spell {
        condition = Conditions.OpponentControlsMoreLands
        effect = Effects.SearchLibrary(
            filter = Filters.PlainsCard,
            count = 3,
            destination = SearchDestination.HAND,
            reveal = true
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "17"
        artist = "Kaja Foglio"
        imageUri = "https://cards.scryfall.io/normal/front/3/4/342b5afe-544f-4fa1-a833-4e0590b41eed.jpg"
    }
}

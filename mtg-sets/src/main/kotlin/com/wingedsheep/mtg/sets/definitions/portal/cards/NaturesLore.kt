package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CardFilter
import com.wingedsheep.sdk.scripting.SearchDestination
import com.wingedsheep.sdk.scripting.SearchLibraryEffect

/**
 * Nature's Lore
 * {1}{G}
 * Sorcery
 * Search your library for a Forest card, put that card onto the battlefield, then shuffle.
 */
val NaturesLore = card("Nature's Lore") {
    manaCost = "{1}{G}"
    typeLine = "Sorcery"

    spell {
        effect = SearchLibraryEffect(
            filter = CardFilter.HasSubtype("Forest"),
            destination = SearchDestination.BATTLEFIELD,
            entersTapped = false
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "178"
        artist = "Terese Nielsen"
        flavorText = "The forest reveals its secrets to those who listen."
        imageUri = "https://cards.scryfall.io/normal/front/5/3/53a41f15-4bf1-4b08-8d06-ca0be86e4163.jpg"
    }
}

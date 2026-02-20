package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.dsl.Effects

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
        effect = Effects.SearchLibrary(
            filter = GameObjectFilter.Land.withSubtype("Forest"),
            destination = SearchDestination.BATTLEFIELD,
            entersTapped = false
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "178"
        artist = "Terese Nielsen"
        flavorText = "The forest reveals its secrets to those who listen."
        imageUri = "https://cards.scryfall.io/normal/front/d/5/d5242227-d033-4e03-b1e6-b334b17bb158.jpg"
    }
}

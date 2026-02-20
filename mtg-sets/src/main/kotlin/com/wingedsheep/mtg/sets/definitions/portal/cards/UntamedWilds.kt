package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.dsl.Effects

/**
 * Untamed Wilds
 * {2}{G}
 * Sorcery
 * Search your library for a basic land card, put that card onto the battlefield, then shuffle.
 */
val UntamedWilds = card("Untamed Wilds") {
    manaCost = "{2}{G}"
    typeLine = "Sorcery"

    spell {
        effect = Effects.SearchLibrary(
            filter = GameObjectFilter.BasicLand,
            destination = SearchDestination.BATTLEFIELD,
            entersTapped = false
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "191"
        artist = "NéNé Thomas"
        flavorText = "The wild places hold secrets waiting to be found."
        imageUri = "https://cards.scryfall.io/normal/front/1/f/1f4fd77e-ee43-4de7-9ee8-1075ff70b5e7.jpg"
    }
}

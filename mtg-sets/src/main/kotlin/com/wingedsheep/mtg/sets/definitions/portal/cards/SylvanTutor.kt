package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.dsl.Effects

/**
 * Sylvan Tutor
 * {G}
 * Sorcery
 * Search your library for a creature card, reveal that card, shuffle your library,
 * then put the card on top of it.
 */
val SylvanTutor = card("Sylvan Tutor") {
    manaCost = "{G}"
    typeLine = "Sorcery"

    spell {
        effect = Effects.SearchLibrary(
            filter = GameObjectFilter.Creature,
            destination = SearchDestination.TOP_OF_LIBRARY,
            reveal = true
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "188"
        artist = "Terese Nielsen"
        flavorText = "The forest knows where its children hide."
        imageUri = "https://cards.scryfall.io/normal/front/2/8/287ba07e-6434-4850-940f-454fcab3f535.jpg"
    }
}

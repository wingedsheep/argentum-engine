package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CardFilter
import com.wingedsheep.sdk.scripting.SearchDestination
import com.wingedsheep.sdk.scripting.SearchLibraryEffect

/**
 * Wood Elves
 * {2}{G}
 * Creature — Elf Scout
 * 1/1
 * When Wood Elves enters the battlefield, search your library for a Forest card,
 * put that card onto the battlefield, then shuffle.
 */
val WoodElves = card("Wood Elves") {
    manaCost = "{2}{G}"
    typeLine = "Creature — Elf Scout"
    power = 1
    toughness = 1

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = SearchLibraryEffect(
            filter = CardFilter.HasSubtype("Forest"),
            destination = SearchDestination.BATTLEFIELD,
            entersTapped = false
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "195"
        artist = "Quinton Hoover"
        flavorText = "They know every path through the forest, even the ones yet unmade."
        imageUri = "https://cards.scryfall.io/normal/front/e/8/e8f9a0b1-c2d3-4e5f-6a7b-8c9d0e1f2a3b.jpg"
    }
}

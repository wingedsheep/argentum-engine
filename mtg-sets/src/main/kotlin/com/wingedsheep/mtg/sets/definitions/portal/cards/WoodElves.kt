package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
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
            unifiedFilter = GameObjectFilter.Land.withSubtype("Forest"),
            destination = SearchDestination.BATTLEFIELD,
            entersTapped = false
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "195"
        artist = "Quinton Hoover"
        flavorText = "They know every path through the forest, even the ones yet unmade."
        imageUri = "https://cards.scryfall.io/normal/front/b/7/b7f1fb90-5c85-46a5-802d-248cc0250921.jpg"
    }
}

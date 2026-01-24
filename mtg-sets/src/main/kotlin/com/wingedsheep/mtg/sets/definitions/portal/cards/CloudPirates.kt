package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CanOnlyBlockCreaturesWithKeyword

/**
 * Cloud Pirates
 * {U}
 * Creature - Human Pirate
 * 1/1
 * Flying
 * Cloud Pirates can block only creatures with flying.
 */
val CloudPirates = card("Cloud Pirates") {
    manaCost = "{U}"
    typeLine = "Creature — Human Pirate"
    power = 1
    toughness = 1

    keywords(Keyword.FLYING)

    staticAbility {
        ability = CanOnlyBlockCreaturesWithKeyword(Keyword.FLYING)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "46"
        artist = "Phil Foglio"
        imageUri = "https://cards.scryfall.io/normal/front/9/f/9f7386c6-d17a-4c7d-884d-2471b87d8b8e.jpg"
    }
}

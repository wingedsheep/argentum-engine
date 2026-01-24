package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CanOnlyBlockCreaturesWithKeyword

/**
 * Cloud Spirit
 * {2}{U}
 * Creature - Spirit
 * 3/1
 * Flying
 * Cloud Spirit can block only creatures with flying.
 */
val CloudSpirit = card("Cloud Spirit") {
    manaCost = "{2}{U}"
    typeLine = "Creature — Spirit"
    power = 3
    toughness = 1

    keywords(Keyword.FLYING)

    staticAbility {
        ability = CanOnlyBlockCreaturesWithKeyword(Keyword.FLYING)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "47"
        artist = "DiTerlizzi"
        imageUri = "https://cards.scryfall.io/normal/front/c/c/cc7547aa-fcf7-4b6e-955d-cc5ebc40cd7d.jpg"
    }
}

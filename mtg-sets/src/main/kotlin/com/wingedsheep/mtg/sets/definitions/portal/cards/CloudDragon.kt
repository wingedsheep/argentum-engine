package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CanOnlyBlockCreaturesWithKeyword

/**
 * Cloud Dragon
 * {5}{U}
 * Creature - Illusion Dragon
 * 5/4
 * Flying
 * Cloud Dragon can block only creatures with flying.
 */
val CloudDragon = card("Cloud Dragon") {
    manaCost = "{5}{U}"
    typeLine = "Creature — Illusion Dragon"
    power = 5
    toughness = 4

    keywords(Keyword.FLYING)

    staticAbility {
        ability = CanOnlyBlockCreaturesWithKeyword(Keyword.FLYING)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "45"
        artist = "John Avon"
        imageUri = "https://cards.scryfall.io/normal/front/4/b/4bb7fb59-65c0-4af6-9d3a-79cd6602d004.jpg"
    }
}

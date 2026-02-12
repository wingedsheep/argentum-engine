package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CanOnlyBlockCreaturesWithKeyword

/**
 * Ascending Aven
 * {2}{U}{U}
 * Creature — Bird Soldier
 * 3/2
 * Flying
 * Ascending Aven can block only creatures with flying.
 * Morph {2}{U}
 */
val AscendingAven = card("Ascending Aven") {
    manaCost = "{2}{U}{U}"
    typeLine = "Creature — Bird Soldier"
    power = 3
    toughness = 2
    oracleText = "Flying\nAscending Aven can block only creatures with flying.\nMorph {2}{U}"

    keywords(Keyword.FLYING)
    morph = "{2}{U}"

    staticAbility {
        ability = CanOnlyBlockCreaturesWithKeyword(Keyword.FLYING)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "68"
        artist = "Ron Spencer"
        imageUri = "https://cards.scryfall.io/normal/front/b/d/bd8b17df-615c-4cc1-af1a-2fc35a985af9.jpg?1562939757"
    }
}

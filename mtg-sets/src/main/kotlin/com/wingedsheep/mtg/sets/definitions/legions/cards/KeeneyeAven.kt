package com.wingedsheep.mtg.sets.definitions.legions.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Keeneye Aven
 * {3}{U}
 * Creature — Bird Soldier
 * 2/3
 * Flying
 * Cycling {2}
 */
val KeeneyeAven = card("Keeneye Aven") {
    manaCost = "{3}{U}"
    typeLine = "Creature — Bird Soldier"
    power = 2
    toughness = 3
    oracleText = "Flying\nCycling {2}"

    keywords(Keyword.FLYING)
    keywordAbility(KeywordAbility.cycling("{2}"))

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "41"
        artist = "Greg Hildebrandt"
        flavorText = "\"I have no need of a map. The very continent itself guides my way.\""
        imageUri = "https://cards.scryfall.io/normal/front/1/a/1a355c58-cd28-4d2d-9df1-91b4196b01ef.jpg?1562900188"
    }
}

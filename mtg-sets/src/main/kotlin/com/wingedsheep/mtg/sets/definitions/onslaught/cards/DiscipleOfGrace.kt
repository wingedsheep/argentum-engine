package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Disciple of Grace
 * {1}{W}
 * Creature — Human Cleric
 * 1/2
 * Protection from black
 * Cycling {2}
 */
val DiscipleOfGrace = card("Disciple of Grace") {
    manaCost = "{1}{W}"
    typeLine = "Creature — Human Cleric"
    power = 1
    toughness = 2

    keywordAbility(KeywordAbility.ProtectionFromColor(Color.BLACK))
    keywordAbility(KeywordAbility.cycling("{2}"))

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "25"
        artist = "Thomas M. Baxa"
        flavorText = "Dread was as foreign to her as the landscape ahead."
        imageUri = "https://cards.scryfall.io/normal/front/1/d/1d1790cb-34e4-4f23-8a13-1906fd9a956f.jpg?1562901832"
    }
}

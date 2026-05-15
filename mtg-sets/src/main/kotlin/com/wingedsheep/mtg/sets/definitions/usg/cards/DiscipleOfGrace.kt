package com.wingedsheep.mtg.sets.definitions.usg.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.ProtectionScope

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
    colorIdentity = "W"
    typeLine = "Creature — Human Cleric"
    power = 1
    toughness = 2
    oracleText = "Protection from black\nCycling {2}"

    keywordAbility(KeywordAbility.Protection(ProtectionScope.Color(Color.BLACK)))
    keywordAbility(KeywordAbility.cycling("{2}"))

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "10"
        artist = "Robh Ruppel"
        imageUri = "https://cards.scryfall.io/normal/front/8/3/83fa36d2-0a60-40a5-a182-a63e1e65b2bd.jpg?1562922861"
    }
}

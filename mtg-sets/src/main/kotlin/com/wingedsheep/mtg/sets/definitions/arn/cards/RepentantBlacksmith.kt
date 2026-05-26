package com.wingedsheep.mtg.sets.definitions.arn.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.ProtectionScope

/**
 * Repentant Blacksmith
 * {1}{W}
 * Creature — Human
 * 1/2
 * Protection from red
 */
val RepentantBlacksmith = card("Repentant Blacksmith") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human"
    power = 1
    toughness = 2
    oracleText = "Protection from red"
    keywordAbility(KeywordAbility.Protection(ProtectionScope.Color(Color.RED)))

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "9"
        artist = "Drew Tucker"
        flavorText = "\"For my confession they burned me with fire And found that I was for endurance made.\" —The Arabian Nights, trans. Haddawy"
        imageUri = "https://cards.scryfall.io/normal/front/6/1/61fc30b6-1355-425b-a86f-18f59f83141c.jpg?1562913111"
    }
}

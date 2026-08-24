package com.wingedsheep.mtg.sets.definitions.mmq.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.ProtectionScope

/**
 * Thermal Glider
 * {2}{W}
 * Creature — Human Rebel
 * 2 / 1
 */
val ThermalGlider = card("Thermal Glider") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Rebel"
    oracleText = "Flying, protection from red"
    power = 2
    toughness = 1

    keywords(Keyword.FLYING)
    keywordAbility(KeywordAbility.Protection(ProtectionScope.Color(Color.RED)))

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "53"
        artist = "Mark Zug"
        flavorText = "\"The Mercadians are too busy looking down on us to see us coming.\"\n" +
            "—Cho-Arrim rebel"
        imageUri = "https://cards.scryfall.io/normal/front/f/d/fd909c26-930d-4af0-b19a-c899847338b4.jpg"
    }
}

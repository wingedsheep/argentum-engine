package com.wingedsheep.mtg.sets.definitions.mmq.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.ProtectionScope

/**
 * Nightwind Glider
 * {2}{W}
 * Creature — Human Rebel
 * 2 / 1
 */
val NightwindGlider = card("Nightwind Glider") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Rebel"
    oracleText = "Flying, protection from black"
    power = 2
    toughness = 1

    keywords(Keyword.FLYING)
    keywordAbility(KeywordAbility.Protection(ProtectionScope.Color(Color.BLACK)))

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "31"
        artist = "Randy Gallegos"
        flavorText = "Once you learn to float on the shadows, you'll never fear them again."
        imageUri = "https://cards.scryfall.io/normal/front/0/9/0968401d-522f-4def-92a1-d504471ac54e.jpg"
    }
}

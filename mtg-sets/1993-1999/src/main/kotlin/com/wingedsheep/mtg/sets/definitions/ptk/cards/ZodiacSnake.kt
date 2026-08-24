package com.wingedsheep.mtg.sets.definitions.ptk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Zodiac Snake
 * {2}{B}
 * Creature — Snake
 * 2/2
 * Swampwalk
 */
val ZodiacSnake = card("Zodiac Snake") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Snake"
    power = 2
    toughness = 2
    oracleText = "Swampwalk (This creature can't be blocked as long as defending player controls a Swamp.)"

    keywordAbility(KeywordAbility.Simple(Keyword.SWAMPWALK))

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "99"
        artist = "Qi Baocheng"
        flavorText = "\"Thrice Xuande's ardent quest led to Nanyang, / Where Sleeping Dragon unveiled Han's partition: . . .\""
        imageUri = "https://cards.scryfall.io/normal/front/6/d/6d30d6f0-ca4c-4442-a47f-ecdf52088ecc.jpg"
    }
}

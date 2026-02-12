package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Rorix Bladewing
 * {3}{R}{R}{R}
 * Legendary Creature — Dragon
 * 6/5
 * Flying, haste
 */
val RorixBladewing = card("Rorix Bladewing") {
    manaCost = "{3}{R}{R}{R}"
    typeLine = "Legendary Creature — Dragon"
    power = 6
    toughness = 5
    oracleText = "Flying, haste"

    keywords(Keyword.FLYING, Keyword.HASTE)

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "224"
        artist = "Darrell Riche"
        flavorText = "In the smoldering ashes of Shiv, a few dragons strive to rebuild their native land. The rest seek any opportunity to restore the broken pride of their race."
        imageUri = "https://cards.scryfall.io/normal/front/7/f/7f2caba5-9f30-4b5e-833e-68c85a47ef7c.jpg?1562925006"
    }
}

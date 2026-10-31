package com.wingedsheep.mtg.sets.definitions.m20.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Nimble Birdsticker (M20 #333)
 * {2}{R}  Creature — Goblin  2/3
 *
 * Reach
 */
val NimbleBirdsticker = card("Nimble Birdsticker") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Goblin"
    oracleText = "Reach (This creature can block creatures with flying.)"
    power = 2
    toughness = 3

    keywords(Keyword.REACH)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "333"
        artist = "Anna Podedworna"
        flavorText = "\"What goes up must get stuck.\""
        imageUri = "https://cards.scryfall.io/normal/front/4/f/4ff9bcbd-48fe-4a74-aae5-a447c99aa64b.jpg"
    }
}

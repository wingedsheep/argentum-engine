package com.wingedsheep.mtg.sets.definitions.khans.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Snowhorn Rider
 * {3}{G}{U}{R}
 * Creature — Human Warrior
 * 5/5
 * Trample
 * Morph {2}{G}{U}{R}
 */
val SnowhornRider = card("Snowhorn Rider") {
    manaCost = "{3}{G}{U}{R}"
    typeLine = "Creature — Human Warrior"
    power = 5
    toughness = 5
    oracleText = "Trample\nMorph {2}{G}{U}{R} (You may cast this card face down as a 2/2 creature for {3}. Turn it face up any time for its morph cost.)"

    keywords(Keyword.TRAMPLE)

    morph = "{2}{G}{U}{R}"

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "201"
        artist = "Tomasz Jedruszek"
        flavorText = "\"Sure-footed, strong-willed, and ill-tempered—and so is the ram.\""
        imageUri = "https://cards.scryfall.io/normal/front/f/5/f5d9aa9b-1ed4-46db-89d1-6c9593c29f55.jpg?1562796087"
    }
}

package com.wingedsheep.mtg.sets.definitions.ori.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Skysnare Spider
 * {4}{G}{G}
 * Creature — Spider
 * 6/6
 * Vigilance
 * Reach
 */
val SkysnareSpider = card("Skysnare Spider") {
    manaCost = "{4}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Spider"
    power = 6
    toughness = 6
    oracleText = "Vigilance (Attacking doesn't cause this creature to tap.)\nReach (This creature can block creatures with flying.)"

    keywords(Keyword.VIGILANCE, Keyword.REACH)

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "197"
        artist = "Filip Burburan"
        flavorText = "The only thing more ill-tempered than a griffin in a web is the spider that must subdue it."
        imageUri = "https://cards.scryfall.io/normal/front/f/1/f1743227-94fd-44c0-884d-fa269bcfd67d.jpg?1783938318"
    }
}

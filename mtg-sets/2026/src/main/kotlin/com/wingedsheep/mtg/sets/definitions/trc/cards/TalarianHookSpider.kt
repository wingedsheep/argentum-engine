package com.wingedsheep.mtg.sets.definitions.trc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Talarian Hook Spider
 * {1}{G}
 * Creature — Spider
 * 2/2
 * Reach
 */
val TalarianHookSpider = card("Talarian Hook Spider") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Spider"
    oracleText = "Reach (This creature can block creatures with flying.)"
    power = 2
    toughness = 2
    keywords(Keyword.REACH)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "175"
        artist = "Zoltan Boros"
        flavorText = "\"Well, I had a choice. Do I walk away and let the emitter blow itself to hell? Or do I crawl in the Jefferies tube with twenty hook spiders?\"\n—Chief Miles O'Brien"
        imageUri = "https://cards.scryfall.io/normal/front/f/4/f49df857-5488-455e-8ac1-b000f600eda7.jpg?1785981165"
    }
}

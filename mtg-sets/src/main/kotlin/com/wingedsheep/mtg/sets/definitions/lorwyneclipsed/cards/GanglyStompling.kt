package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Gangly Stompling
 * {2}{R/G}
 * Creature — Shapeshifter
 * 4/2
 *
 * Changeling (This card is every creature type.)
 * Trample
 */
val GanglyStompling = card("Gangly Stompling") {
    manaCost = "{2}{R/G}"
    typeLine = "Creature — Shapeshifter"
    power = 4
    toughness = 2
    oracleText = "Changeling (This card is every creature type.)\n" +
        "Trample"

    keywords(Keyword.CHANGELING, Keyword.TRAMPLE)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "226"
        artist = "Scott Murphy"
        flavorText = "Having manifested in the grotto at diminutive size, it now reveled in the reverberations of its own heavy footfalls."
        imageUri = "https://cards.scryfall.io/normal/front/5/0/502000a7-a3c1-4259-aea5-ff01724396a1.jpg?1767749620"
    }
}

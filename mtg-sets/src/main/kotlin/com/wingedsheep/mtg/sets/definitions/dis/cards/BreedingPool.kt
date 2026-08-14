package com.wingedsheep.mtg.sets.definitions.dis.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersTapped

/**
 * Breeding Pool
 *
 * Land — Forest Island
 * ({T}: Add {G} or {U}.)
 * As this land enters, you may pay 2 life. If you don't, it enters tapped.
 */
val BreedingPool = card("Breeding Pool") {
    manaCost = ""
    colorIdentity = "GU"
    typeLine = "Land — Forest Island"
    oracleText = "({T}: Add {G} or {U}.)\nAs this land enters, you may pay 2 life. If you don't, it enters tapped."

    // Mana abilities are intrinsic from basic land types (Forest → {G}, Island → {U})

    replacementEffect(EntersTapped(payLifeCost = 2))

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "172"
        artist = "Rob Alexander"
        imageUri = "https://cards.scryfall.io/normal/front/b/9/b98b2a35-ec2b-47fe-903d-dd292e469a3c.jpg?1783943378"
        ruling("2018-10-05", "Unlike most dual lands, this land has two basic land types. It's not basic, so cards such as District Guide can't find it, but it does have the appropriate land types for effects such as that of Drowned Catacomb (from the Ixalan set).")
        ruling("2018-10-05", "If an effect puts this land onto the battlefield tapped, you may pay 2 life, but it still enters tapped.")
    }
}

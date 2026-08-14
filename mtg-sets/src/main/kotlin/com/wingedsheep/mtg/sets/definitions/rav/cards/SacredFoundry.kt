package com.wingedsheep.mtg.sets.definitions.rav.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersTapped

/**
 * Sacred Foundry
 *
 * Land — Mountain Plains
 * ({T}: Add {R} or {W}.)
 * As this land enters, you may pay 2 life. If you don't, it enters tapped.
 */
val SacredFoundry = card("Sacred Foundry") {
    manaCost = ""
    colorIdentity = "RW"
    typeLine = "Land — Mountain Plains"
    oracleText = "({T}: Add {R} or {W}.)\nAs this land enters, you may pay 2 life. If you don't, it enters tapped."

    // Mana abilities are intrinsic from basic land types (Mountain → {R}, Plains → {W})

    replacementEffect(EntersTapped(payLifeCost = 2))

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "280"
        artist = "Rob Alexander"
        imageUri = "https://cards.scryfall.io/normal/front/1/6/168ef687-5797-4b45-b75b-393d8117cebd.jpg?1783943590"
        ruling("2018-10-05", "Unlike most dual lands, this land has two basic land types. It's not basic, so cards such as District Guide can't find it, but it does have the appropriate land types for effects such as that of Drowned Catacomb (from the Ixalan set).")
        ruling("2018-10-05", "If an effect puts this land onto the battlefield tapped, you may pay 2 life, but it still enters tapped.")
    }
}

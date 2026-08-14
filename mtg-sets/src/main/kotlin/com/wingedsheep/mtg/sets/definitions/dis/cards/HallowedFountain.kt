package com.wingedsheep.mtg.sets.definitions.dis.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersTapped

/**
 * Hallowed Fountain
 *
 * Land — Plains Island
 * ({T}: Add {W} or {U}.)
 * As this land enters, you may pay 2 life. If you don't, it enters tapped.
 */
val HallowedFountain = card("Hallowed Fountain") {
    manaCost = ""
    colorIdentity = "WU"
    typeLine = "Land — Plains Island"
    oracleText = "({T}: Add {W} or {U}.)\nAs this land enters, you may pay 2 life. If you don't, it enters tapped."

    // Mana abilities are intrinsic from basic land types (Plains → {W}, Island → {U})

    replacementEffect(EntersTapped(payLifeCost = 2))

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "174"
        artist = "Rob Alexander"
        imageUri = "https://cards.scryfall.io/normal/front/c/2/c28aea19-2a39-4934-afda-909e234fa3ba.jpg?1783943377"
        ruling("2025-11-17", "This land has two basic land types. It's not basic, so cards such as Tend the Sprigs can't find it, but it does have the appropriate land types for effects such as that of Kishla Village (from the Tarkir: Dragonstorm release).")
        ruling("2025-11-17", "If an effect puts this land onto the battlefield tapped, you may pay 2 life, but it still enters tapped.")
    }
}

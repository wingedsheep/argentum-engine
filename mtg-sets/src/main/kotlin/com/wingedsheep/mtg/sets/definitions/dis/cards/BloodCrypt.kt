package com.wingedsheep.mtg.sets.definitions.dis.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersTapped

/**
 * Blood Crypt
 *
 * Land — Swamp Mountain
 * ({T}: Add {B} or {R}.)
 * As this land enters, you may pay 2 life. If you don't, it enters tapped.
 */
val BloodCrypt = card("Blood Crypt") {
    manaCost = ""
    colorIdentity = "BR"
    typeLine = "Land — Swamp Mountain"
    oracleText = "({T}: Add {B} or {R}.)\nAs this land enters, you may pay 2 life. If you don't, it enters tapped."

    // Mana abilities are intrinsic from basic land types (Swamp → {B}, Mountain → {R})

    replacementEffect(EntersTapped(payLifeCost = 2))

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "171"
        artist = "Rob Alexander"
        imageUri = "https://cards.scryfall.io/normal/front/f/2/f281e16f-0fe1-4095-bd63-0a4479f75c11.jpg?1783943377"
        ruling("2025-11-17", "This land has two basic land types. It's not basic, so cards such as Tend the Sprigs can't find it, but it does have the appropriate land types for effects such as that of Kishla Village (from the Tarkir: Dragonstorm release).")
        ruling("2025-11-17", "If an effect puts this land onto the battlefield tapped, you may pay 2 life, but it still enters tapped.")
    }
}

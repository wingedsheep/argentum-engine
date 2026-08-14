package com.wingedsheep.mtg.sets.definitions.rav.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersTapped

/**
 * Overgrown Tomb
 *
 * Land — Swamp Forest
 * ({T}: Add {B} or {G}.)
 * As this land enters, you may pay 2 life. If you don't, it enters tapped.
 */
val OvergrownTomb = card("Overgrown Tomb") {
    manaCost = ""
    colorIdentity = "BG"
    typeLine = "Land — Swamp Forest"
    oracleText = "({T}: Add {B} or {G}.)\nAs this land enters, you may pay 2 life. If you don't, it enters tapped."

    // Mana abilities are intrinsic from basic land types (Swamp → {B}, Forest → {G})

    replacementEffect(EntersTapped(payLifeCost = 2))

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "279"
        artist = "Rob Alexander"
        imageUri = "https://cards.scryfall.io/normal/front/f/c/fce07335-cc78-4683-b2f0-9c98a06ea1d8.jpg?1783943591"
        ruling("2018-10-05", "Unlike most dual lands, this land has two basic land types. It's not basic, so cards such as District Guide can't find it, but it does have the appropriate land types for effects such as that of Drowned Catacomb (from the Ixalan set).")
        ruling("2018-10-05", "If an effect puts this land onto the battlefield tapped, you may pay 2 life, but it still enters tapped.")
    }
}

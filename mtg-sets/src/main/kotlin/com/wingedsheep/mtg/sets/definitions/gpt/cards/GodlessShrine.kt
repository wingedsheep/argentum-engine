package com.wingedsheep.mtg.sets.definitions.gpt.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersTapped

/**
 * Godless Shrine
 *
 * Land — Plains Swamp
 * ({T}: Add {W} or {B}.)
 * As this land enters, you may pay 2 life. If you don't, it enters tapped.
 */
val GodlessShrine = card("Godless Shrine") {
    manaCost = ""
    colorIdentity = "WB"
    typeLine = "Land — Plains Swamp"
    oracleText = "({T}: Add {W} or {B}.)\nAs this land enters, you may pay 2 life. If you don't, it enters tapped."

    // Mana abilities are intrinsic from basic land types (Plains → {W}, Swamp → {B})

    replacementEffect(EntersTapped(payLifeCost = 2))

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "157"
        artist = "Rob Alexander"
        imageUri = "https://cards.scryfall.io/normal/front/b/e/be010c2f-06db-47e3-80bd-df3f2a21ca34.jpg?1783943456"
        ruling("2018-10-05", "Unlike most dual lands, this land has two basic land types. It's not basic, so cards such as District Guide can't find it, but it does have the appropriate land types for effects such as that of Drowned Catacomb (from the Ixalan set).")
        ruling("2018-10-05", "If an effect puts this land onto the battlefield tapped, you may pay 2 life, but it still enters tapped.")
    }
}

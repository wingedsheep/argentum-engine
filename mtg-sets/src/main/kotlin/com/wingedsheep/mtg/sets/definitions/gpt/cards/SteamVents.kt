package com.wingedsheep.mtg.sets.definitions.gpt.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersTapped

/**
 * Steam Vents
 *
 * Land — Island Mountain
 * ({T}: Add {U} or {R}.)
 * As this land enters, you may pay 2 life. If you don't, it enters tapped.
 */
val SteamVents = card("Steam Vents") {
    manaCost = ""
    colorIdentity = "UR"
    typeLine = "Land — Island Mountain"
    oracleText = "({T}: Add {U} or {R}.)\nAs this land enters, you may pay 2 life. If you don't, it enters tapped."

    // Mana abilities are intrinsic from basic land types (Island → {U}, Mountain → {R})

    replacementEffect(EntersTapped(payLifeCost = 2))

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "164"
        artist = "Rob Alexander"
        imageUri = "https://cards.scryfall.io/normal/front/0/5/054f2276-2dd5-43da-bb26-c57c560861fe.jpg?1783943453"
        ruling("2018-10-05", "Unlike most dual lands, this land has two basic land types. It's not basic, so cards such as District Guide can't find it, but it does have the appropriate land types for effects such as that of Drowned Catacomb (from the Ixalan set).")
        ruling("2018-10-05", "If an effect puts this land onto the battlefield tapped, you may pay 2 life, but it still enters tapped.")
    }
}

package com.wingedsheep.mtg.sets.definitions.gpt.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersTapped

/**
 * Stomping Ground
 *
 * Land — Mountain Forest
 * ({T}: Add {R} or {G}.)
 * As this land enters, you may pay 2 life. If you don't, it enters tapped.
 */
val StompingGround = card("Stomping Ground") {
    manaCost = ""
    colorIdentity = "RG"
    typeLine = "Land — Mountain Forest"
    oracleText = "({T}: Add {R} or {G}.)\nAs this land enters, you may pay 2 life. If you don't, it enters tapped."

    // Mana abilities are intrinsic from basic land types (Mountain → {R}, Forest → {G})

    replacementEffect(EntersTapped(payLifeCost = 2))

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "165"
        artist = "Rob Alexander"
        imageUri = "https://cards.scryfall.io/normal/front/a/2/a2773d8f-f906-475d-aaff-b7ca3b01f188.jpg?1783943453"
        ruling("2018-10-05", "Unlike most dual lands, this land has two basic land types. It's not basic, so cards such as District Guide can't find it, but it does have the appropriate land types for effects such as that of Drowned Catacomb (from the Ixalan set).")
        ruling("2018-10-05", "If an effect puts this land onto the battlefield tapped, you may pay 2 life, but it still enters tapped.")
    }
}

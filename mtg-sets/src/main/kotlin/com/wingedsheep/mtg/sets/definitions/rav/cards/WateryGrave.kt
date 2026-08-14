package com.wingedsheep.mtg.sets.definitions.rav.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersTapped

/**
 * Watery Grave
 *
 * Land — Island Swamp
 * ({T}: Add {U} or {B}.)
 * As this land enters, you may pay 2 life. If you don't, it enters tapped.
 */
val WateryGrave = card("Watery Grave") {
    manaCost = ""
    colorIdentity = "UB"
    typeLine = "Land — Island Swamp"
    oracleText = "({T}: Add {U} or {B}.)\nAs this land enters, you may pay 2 life. If you don't, it enters tapped."

    // Mana abilities are intrinsic from basic land types (Island → {U}, Swamp → {B})

    replacementEffect(EntersTapped(payLifeCost = 2))

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "286"
        artist = "Rob Alexander"
        imageUri = "https://cards.scryfall.io/normal/front/1/3/139b90cd-8272-457a-be32-1298145345be.jpg?1783943589"
        ruling("2018-10-05", "Unlike most dual lands, this land has two basic land types. It's not basic, so cards such as District Guide can't find it, but it does have the appropriate land types for effects such as that of Drowned Catacomb (from the Ixalan set).")
        ruling("2018-10-05", "If an effect puts this land onto the battlefield tapped, you may pay 2 life, but it still enters tapped.")
    }
}

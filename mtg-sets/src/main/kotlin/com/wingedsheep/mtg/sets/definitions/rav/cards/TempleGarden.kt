package com.wingedsheep.mtg.sets.definitions.rav.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersTapped

/**
 * Temple Garden
 *
 * Land — Forest Plains
 * ({T}: Add {G} or {W}.)
 * As this land enters, you may pay 2 life. If you don't, it enters tapped.
 */
val TempleGarden = card("Temple Garden") {
    manaCost = ""
    colorIdentity = "GW"
    typeLine = "Land — Forest Plains"
    oracleText = "({T}: Add {G} or {W}.)\nAs this land enters, you may pay 2 life. If you don't, it enters tapped."

    // Mana abilities are intrinsic from basic land types (Forest → {G}, Plains → {W})

    replacementEffect(EntersTapped(payLifeCost = 2))

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "284"
        artist = "Rob Alexander"
        imageUri = "https://cards.scryfall.io/normal/front/7/9/794a2b79-8c55-4423-8843-7e6e96f84071.jpg?1783943589"
        ruling("2018-10-05", "Unlike most dual lands, this land has two basic land types. It's not basic, so cards such as District Guide can't find it, but it does have the appropriate land types for effects such as that of Drowned Catacomb (from the Ixalan set).")
        ruling("2018-10-05", "If an effect puts this land onto the battlefield tapped, you may pay 2 life, but it still enters tapped.")
    }
}

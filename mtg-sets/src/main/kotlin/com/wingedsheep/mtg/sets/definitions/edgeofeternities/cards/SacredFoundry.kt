package com.wingedsheep.mtg.sets.definitions.edgeofeternities.cards

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
    typeLine = "Land — Mountain Plains"
    oracleText = "({T}: Add {R} or {W}.)\nAs this land enters, you may pay 2 life. If you don't, it enters tapped."

    // Mana abilities are intrinsic from basic land types (Mountain → {R}, Plains → {W})

    // As this land enters, you may pay 2 life. If you don't, it enters tapped.
    replacementEffect(EntersTapped(payLifeCost = 2))

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "256"
        artist = "Titus Lunter"
        flavorText = "\"Log 2.2.119: Kiln, known for its blazing forges and ancient lava seeps. The footing may be treacherous, but the metallurgy is grand!\"\n—*Maisie's Edge Chronicles*"
        imageUri = "https://cards.scryfall.io/normal/front/8/b/8b4e2642-3c87-4708-b9b4-2e7f7359ac7d.jpg?1752947600"
    }
}

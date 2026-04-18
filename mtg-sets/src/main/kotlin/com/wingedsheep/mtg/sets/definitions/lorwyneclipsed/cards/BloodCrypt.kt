package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersTapped

/**
 * Blood Crypt
 * Land — Swamp Mountain
 *
 * ({T}: Add {B} or {R}.)
 * As this land enters, you may pay 2 life. If you don't, it enters tapped.
 */
val BloodCrypt = card("Blood Crypt") {
    typeLine = "Land — Swamp Mountain"
    oracleText = "({T}: Add {B} or {R}.)\nAs this land enters, you may pay 2 life. If you don't, it enters tapped."

    // Mana abilities are intrinsic from basic land types (Swamp → {B}, Mountain → {R})

    replacementEffect(EntersTapped(payLifeCost = 2))

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "262"
        artist = "Adam Paquette"
        flavorText = "The boggart was playfully prodding a critter when the skies darkened, then he gobbled it down with glee."
        imageUri = "https://cards.scryfall.io/normal/front/6/d/6da63cc5-4624-4491-abd9-9b600c3fefe2.jpg?1759144844"
    }
}

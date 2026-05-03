package com.wingedsheep.mtg.sets.definitions.edgeofeternities.cards

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
    typeLine = "Land — Island Swamp"
    oracleText = "({T}: Add {U} or {B}.)\nAs this land enters, you may pay 2 life. If you don't, it enters tapped."

    // Mana abilities are intrinsic from basic land types (Island → {U}, Swamp → {B})

    // As this land enters, you may pay 2 life. If you don't, it enters tapped.
    replacementEffect(EntersTapped(payLifeCost = 2))

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "261"
        artist = "Sergey Glushakov"
        flavorText = "\"Log 2.9.089: Lammuat, the cemetery moon of salvage and secrets. Could be profitable for those brave enough to plumb its recesses.\"\n—*Maisie's Edge Chronicles*"
        imageUri = "https://cards.scryfall.io/normal/front/5/b/5b8170dc-6a90-46fc-9989-7575f3d402b5.jpg?1752947617"
    }
}

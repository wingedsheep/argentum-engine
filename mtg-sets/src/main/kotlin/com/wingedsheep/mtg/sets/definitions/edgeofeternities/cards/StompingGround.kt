package com.wingedsheep.mtg.sets.definitions.edgeofeternities.cards

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
    typeLine = "Land — Mountain Forest"
    oracleText = "({T}: Add {R} or {G}.)\nAs this land enters, you may pay 2 life. If you don't, it enters tapped."

    // Mana abilities are intrinsic from basic land types (Mountain → {R}, Forest → {G})

    // As this land enters, you may pay 2 life. If you don't, it enters tapped.
    replacementEffect(EntersTapped(payLifeCost = 2))

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "258"
        artist = "Bruce Brenneise"
        flavorText = "\"Log 2.5.325: A newborn moon, Aqqat still churns with the fires of creation. I'm eager to see what it will become!\"\n—*Maisie's Edge Chronicles*"
        imageUri = "https://cards.scryfall.io/normal/front/6/9/69be21b4-c613-47c6-ba57-f4785861af3e.jpg?1752947608"
    }
}

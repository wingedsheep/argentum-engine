package com.wingedsheep.mtg.sets.definitions.edgeofeternities.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersTapped

/**
 * Breeding Pool
 * 
 * Land — Forest Island
 * ({T}: Add {G} or {U}.)
 * As this land enters, you may pay 2 life. If you don't, it enters tapped.
 */
val BreedingPool = card("Breeding Pool") {
    manaCost = ""
    typeLine = "Land — Forest Island"
    oracleText = "({T}: Add {G} or {U}.)\nAs this land enters, you may pay 2 life. If you don't, it enters tapped."

    // Mana abilities are intrinsic from basic land types (Forest → {G}, Island → {U})

    // As this land enters, you may pay 2 life. If you don't, it enters tapped.
    replacementEffect(EntersTapped(payLifeCost = 2))

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "251"
        artist = "Constantin Marin"
        flavorText = "\"Log 1.4.778: Granove, a tropical moon teeming with endemic biota. A must see for any aspiring astrobiologist!\"\n—*Maisie's Edge Chronicles*"
        imageUri = "https://cards.scryfall.io/normal/front/3/c/3c750d5a-f743-41ff-b5ba-02025ca0bec2.jpg?1752947580"
    }
}

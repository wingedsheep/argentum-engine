package com.wingedsheep.mtg.sets.definitions.edgeofeternities.cards

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
    typeLine = "Land — Plains Swamp"
    oracleText = "({T}: Add {W} or {B}.)\nAs this land enters, you may pay 2 life. If you don't, it enters tapped."

    // Mana abilities are intrinsic from basic land types (Plains → {W}, Swamp → {B})

    // As this land enters, you may pay 2 life. If you don't, it enters tapped.
    replacementEffect(EntersTapped(payLifeCost = 2))

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "254"
        artist = "Rob Rey"
        flavorText = "\"Log 1.7.618: Starshadow, a relic of a culture lost to calamity. Its people were eradicated in a single day—but by what?\"\n—*Maisie's Edge Chronicles*"
        imageUri = "https://cards.scryfall.io/normal/front/8/c/8c542ea4-98c3-4c2d-9066-205ab7aa697a.jpg?1752947593"
    }
}

package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersTapped

/**
 * Hallowed Fountain
 * Land — Plains Island
 *
 * ({T}: Add {W} or {U}.)
 * As this land enters, you may pay 2 life. If you don't, it enters tapped.
 */
val HallowedFountain = card("Hallowed Fountain") {
    typeLine = "Land — Plains Island"
    oracleText = "({T}: Add {W} or {U}.)\nAs this land enters, you may pay 2 life. If you don't, it enters tapped."

    // Mana abilities are intrinsic from basic land types (Plains → {W}, Island → {U})

    replacementEffect(EntersTapped(payLifeCost = 2))

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "265"
        artist = "Adam Paquette"
        flavorText = "The merrow was vending her wares when the skies darkened, then she sharply raised her prices."
        imageUri = "https://cards.scryfall.io/normal/front/e/0/e056b55f-82ed-4fe0-ab0c-bb20fa4a218a.jpg?1759144845"
    }
}

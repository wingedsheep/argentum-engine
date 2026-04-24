package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersTapped

/**
 * Temple Garden
 * Land — Forest Plains
 *
 * ({T}: Add {G} or {W}.)
 * As this land enters, you may pay 2 life. If you don't, it enters tapped.
 */
val TempleGarden = card("Temple Garden") {
    typeLine = "Land — Forest Plains"
    oracleText = "({T}: Add {G} or {W}.)\nAs this land enters, you may pay 2 life. If you don't, it enters tapped."

    // Mana abilities are intrinsic from basic land types (Forest → {G}, Plains → {W})

    replacementEffect(EntersTapped(payLifeCost = 2))

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "268"
        artist = "Adam Paquette"
        flavorText = "The kithkin was admiring the flowers when the skies darkened, then they ripped up the roots for the next season."
        imageUri = "https://cards.scryfall.io/normal/front/6/c/6cdd2a74-63b3-4ff2-9c5a-a85dee63c3c9.jpg?1759144838"
    }
}

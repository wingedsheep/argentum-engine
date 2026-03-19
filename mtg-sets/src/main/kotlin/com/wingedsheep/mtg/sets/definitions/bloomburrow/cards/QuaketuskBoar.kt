package com.wingedsheep.mtg.sets.definitions.bloomburrow.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Quaketusk Boar
 * {3}{R}{R}
 * Creature — Elemental Boar
 * 5/5
 * Reach, trample, haste
 */
val QuaketuskBoar = card("Quaketusk Boar") {
    manaCost = "{3}{R}{R}"
    typeLine = "Creature — Elemental Boar"
    power = 5
    toughness = 5
    keywords(Keyword.REACH, Keyword.TRAMPLE, Keyword.HASTE)

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "146"
        artist = "Andrew Mar"
        flavorText = "\"Calamity Beasts are running through Valley, leaving death and destruction in their wake. I have a plan to stop them.\" —Glarb, ruler of Fountainport"
        imageUri = "https://cards.scryfall.io/normal/front/2/f/2f2b7fd3-a139-49ea-8a89-b64261e868ef.jpg?1721426673"
    }
}

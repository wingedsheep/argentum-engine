package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Foot Soldiers
 * {3}{W}
 * Creature — Human Soldier
 * 2/4
 * (No abilities - vanilla creature)
 */
val FootSoldiers = card("Foot Soldiers") {
    manaCost = "{3}{W}"
    typeLine = "Creature — Human Soldier"
    power = 2
    toughness = 4

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "16"
        artist = "Kev Walker"
        flavorText = "Infantry deployment is the art of putting troops in the wrong place at the right time."
        imageUri = "https://cards.scryfall.io/normal/front/4/5/458ddb33-66c4-4753-b1eb-8937ab812a81.jpg"
    }
}

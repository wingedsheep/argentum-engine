package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Knight Errant
 * {1}{W}
 * Creature — Human Knight
 * 2/2
 * (No abilities - vanilla creature)
 */
val KnightErrant = card("Knight Errant") {
    manaCost = "{1}{W}"
    typeLine = "Creature — Human Knight"
    power = 2
    toughness = 2

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "20"
        artist = "Dan Frazier"
        flavorText = "Before honor is humility."
        imageUri = "https://cards.scryfall.io/normal/front/9/c/9c31b4b4-18fc-4a6e-8d74-fd5340964320.jpg"
    }
}

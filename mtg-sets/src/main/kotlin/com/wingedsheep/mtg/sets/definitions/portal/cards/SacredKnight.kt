package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBeBlockedByColor

/**
 * Sacred Knight
 * {3}{W}
 * Creature - Human Knight
 * 3/2
 * Sacred Knight can't be blocked by black and/or red creatures.
 */
val SacredKnight = card("Sacred Knight") {
    manaCost = "{3}{W}"
    typeLine = "Creature — Human Knight"
    power = 3
    toughness = 2

    staticAbility {
        ability = CantBeBlockedByColor(setOf(Color.BLACK, Color.RED))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "24"
        artist = "Donato Giancola"
        flavorText = "\"No flame, no horror can sway me from my cause.\""
        imageUri = "https://cards.scryfall.io/normal/front/5/7/57b19990-c4a2-433d-a9ce-005216e9e4ac.jpg"
    }
}

package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBlock

/**
 * Craven Knight
 * {1}{B}
 * Creature — Human Knight
 * 2/2
 * Craven Knight can't block.
 */
val CravenKnight = card("Craven Knight") {
    manaCost = "{1}{B}"
    typeLine = "Creature — Human Knight"
    power = 2
    toughness = 2

    staticAbility {
        ability = CantBlock()
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "85"
        artist = "Dan Frazier"
        imageUri = "https://cards.scryfall.io/normal/front/3/9/39a5a4c8-d40b-45e2-8f2c-9e2e0d0a5f1d.jpg"
    }
}

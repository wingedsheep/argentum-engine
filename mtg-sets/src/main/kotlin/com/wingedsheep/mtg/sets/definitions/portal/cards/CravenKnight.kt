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
        imageUri = "https://cards.scryfall.io/normal/front/d/4/d4cbae27-4a1a-4e16-8876-9a2925c45302.jpg"
    }
}

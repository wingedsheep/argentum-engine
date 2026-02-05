package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBlock

/**
 * Craven Giant
 * {2}{R}
 * Creature — Giant
 * 4/1
 * Craven Giant can't block.
 */
val CravenGiant = card("Craven Giant") {
    manaCost = "{2}{R}"
    typeLine = "Creature — Giant"
    power = 4
    toughness = 1

    staticAbility {
        ability = CantBlock()
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "121"
        artist = "Greg Staples"
        flavorText = "Size does not equal courage."
        imageUri = "https://cards.scryfall.io/normal/front/4/a/4a2e1c12-f848-43b4-9505-851c66a509f1.jpg"
    }
}

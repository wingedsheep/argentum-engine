package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBlock
import com.wingedsheep.sdk.scripting.StaticTarget

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
        ability = CantBlock(StaticTarget.SourceCreature)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "121"
        artist = "Greg Staples"
        flavorText = "Size does not equal courage."
        imageUri = "https://cards.scryfall.io/normal/front/b/1/b1c2d3e4-f5a6-7b8c-9d0e-1f2a3b4c5d6e.jpg"
    }
}

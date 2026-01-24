package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBlock
import com.wingedsheep.sdk.scripting.StaticTarget

/**
 * Jungle Lion
 * {G}
 * Creature — Cat
 * 2/1
 * Jungle Lion can't block.
 */
val JungleLion = card("Jungle Lion") {
    manaCost = "{G}"
    typeLine = "Creature — Cat"
    power = 2
    toughness = 1

    staticAbility {
        ability = CantBlock(StaticTarget.SourceCreature)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "171"
        artist = "Janine Johnston"
        flavorText = "The lion lurks in the deepest jungle, waiting for unwary prey."
        imageUri = "https://cards.scryfall.io/normal/front/f/3/f3f0a586-d65a-4da6-a6bb-88a29c6ea6d8.jpg"
    }
}

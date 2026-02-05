package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBlock

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
        ability = CantBlock()
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "171"
        artist = "Janine Johnston"
        flavorText = "The lion lurks in the deepest jungle, waiting for unwary prey."
        imageUri = "https://cards.scryfall.io/normal/front/6/1/613ceee3-92c7-46f1-8267-d6229ab15df5.jpg"
    }
}

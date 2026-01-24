package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBlock
import com.wingedsheep.sdk.scripting.StaticTarget

/**
 * Hulking Cyclops
 * {3}{R}{R}
 * Creature — Cyclops
 * 5/5
 * Hulking Cyclops can't block.
 */
val HulkingCyclops = card("Hulking Cyclops") {
    manaCost = "{3}{R}{R}"
    typeLine = "Creature — Cyclops"
    power = 5
    toughness = 5

    staticAbility {
        ability = CantBlock(StaticTarget.SourceCreature)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "134"
        artist = "Randy Gallegos"
        flavorText = "Sometimes a foe's greatest weakness is itself."
        imageUri = "https://cards.scryfall.io/normal/front/d/4/d4e5f6a7-b8c9-d0e1-f2a3-b4c5d6e7f8a9.jpg"
    }
}

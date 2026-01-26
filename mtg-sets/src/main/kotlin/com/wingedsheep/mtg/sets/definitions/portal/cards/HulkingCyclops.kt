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
        imageUri = "https://cards.scryfall.io/normal/front/f/2/f20ae982-8a70-4dd3-8254-0d447954f580.jpg"
    }
}

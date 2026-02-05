package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBlock

/**
 * Hulking Goblin
 * {1}{R}
 * Creature — Goblin
 * 2/2
 * Hulking Goblin can't block.
 */
val HulkingGoblin = card("Hulking Goblin") {
    manaCost = "{1}{R}"
    typeLine = "Creature — Goblin"
    power = 2
    toughness = 2

    staticAbility {
        ability = CantBlock()
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "135"
        artist = "Kev Walker"
        flavorText = "Bigger goblins don't flee from danger—they just can't turn around."
        imageUri = "https://cards.scryfall.io/normal/front/8/e/8e3eead8-7e07-4463-9e67-c396d2d7931e.jpg"
    }
}

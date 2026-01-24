package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBlock
import com.wingedsheep.sdk.scripting.StaticTarget

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
        ability = CantBlock(StaticTarget.SourceCreature)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "135"
        artist = "Kev Walker"
        flavorText = "Bigger goblins don't flee from danger—they just can't turn around."
        imageUri = "https://cards.scryfall.io/normal/front/e/5/e5f6a7b8-c9d0-e1f2-a3b4-c5d6e7f8a9b0.jpg"
    }
}

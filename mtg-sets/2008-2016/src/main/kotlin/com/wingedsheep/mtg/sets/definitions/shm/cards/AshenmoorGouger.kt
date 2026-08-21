package com.wingedsheep.mtg.sets.definitions.shm.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBlock

/**
 * Ashenmoor Gouger
 * {B/R}{B/R}{B/R}
 * Creature — Elemental Warrior
 * 4 / 4
 *
 * This creature can't block.
 *
 * - "This creature can't block" is a self-scoped combat static, so [CantBlock] keeps its default
 *   `GroupFilter.source()` filter rather than taking a battlefield-wide one.
 */
val AshenmoorGouger = card("Ashenmoor Gouger") {
    manaCost = "{B/R}{B/R}{B/R}"
    typeLine = "Creature — Elemental Warrior"
    power = 4
    toughness = 4
    oracleText = "This creature can't block."

    staticAbility {
        ability = CantBlock()
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "180"
        artist = "Matt Cavotta"
        flavorText = "After his hands had crumbled away, leaving only wickedly sharp points, he decided his only purpose was war."
        imageUri = "https://cards.scryfall.io/normal/front/8/0/80c7931f-e979-4f43-81dd-c34166526f87.jpg?1783942728"
    }
}

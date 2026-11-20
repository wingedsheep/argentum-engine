package com.wingedsheep.mtg.sets.definitions.dgm.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Mindstatic
 * {3}{U}
 * Instant
 * Counter target spell unless its controller pays {6}.
 *
 * Canonical printing: Dragon's Maze, the card's earliest real printing.
 */
val Mindstatic = card("Mindstatic") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Counter target spell unless its controller pays {6}."

    spell {
        target(TargetFilter.SpellOnStack)
        effect = Effects.CounterUnlessPays("{6}")
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "14"
        artist = "Johann Bodin"
        flavorText = "\"It's always satisfying to stymie the best efforts of a rival, but the reactions of the Gruul are particularly amusing.\"\n—Bori Andon, Izzet blastseeker"
        imageUri = "https://cards.scryfall.io/normal/front/5/5/55d3fad5-a12a-4b41-9c7b-c1af5e0b5ca8.jpg"
    }
}

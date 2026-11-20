package com.wingedsheep.mtg.sets.definitions.dgm.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Mending Touch
 * {G}
 * Instant
 * Regenerate target creature.
 *
 * Canonical printing: Dragon's Maze, the card's earliest real printing.
 */
val MendingTouch = card("Mending Touch") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Regenerate target creature."

    spell {
        val t = target(TargetFilter.Creature)
        effect = Effects.Regenerate(t)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "44"
        artist = "Karla Ortiz"
        flavorText = "\"The gift of life must be freely given. Anything less would diminish us all.\"\n—Arin, Selesnya healer"
        imageUri = "https://cards.scryfall.io/normal/front/c/0/c042c7ee-0e74-4ca5-bbb9-2898b0576f0a.jpg"
    }
}

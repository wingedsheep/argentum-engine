package com.wingedsheep.mtg.sets.definitions.dgm.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Fatal Fumes
 * {3}{B}
 * Instant
 * Target creature gets -4/-2 until end of turn.
 *
 * Canonical printing: Dragon's Maze, the card's earliest real printing.
 */
val FatalFumes = card("Fatal Fumes") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Target creature gets -4/-2 until end of turn."

    spell {
        val t = target(TargetFilter.Creature)
        effect = Effects.ModifyStats(-4, -2, t)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "24"
        artist = "Kev Walker"
        flavorText = "Travelers in the undercity dread the horrors and assassins of the Dimir, but nearly as many die from more mundane hazards."
        imageUri = "https://cards.scryfall.io/normal/front/9/6/967aa636-a11d-4c5c-ba85-648734b295c2.jpg"
    }
}

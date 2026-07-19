package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Hour of Defeat
 * {3}{B}
 * Instant
 * Destroy target creature. Surveil 1.
 *
 * The surveil happens on resolution even if the destroy is redirected/prevented, but the whole
 * spell fizzles if the single target is illegal — modelled by keeping both halves in one
 * composite resolution effect.
 */
val HourOfDefeat = card("Hour of Defeat") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Destroy target creature. Surveil 1. (Look at the top card of your library. You may put it into your graveyard.)"

    spell {
        val creature = target("target creature", Targets.Creature)
        effect = Effects.Composite(
            Effects.Destroy(creature),
            Patterns.Library.surveil(1)
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "99"
        artist = "Jake Murray"
        flavorText = "\"Tomorrow's sun will rise on a new king of Wakanda!\"\n—Erik Killmonger"
        imageUri = "https://cards.scryfall.io/normal/front/9/e/9e0034dd-396e-46af-b931-0daa25da4406.jpg?1783902942"
    }
}

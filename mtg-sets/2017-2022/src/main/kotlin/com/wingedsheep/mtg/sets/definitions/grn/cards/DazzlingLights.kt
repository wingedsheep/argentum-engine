package com.wingedsheep.mtg.sets.definitions.grn.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Dazzling Lights
 * {U}
 * Instant
 * Target creature gets -3/-0 until end of turn.
 * Surveil 2.
 */
val DazzlingLights = card("Dazzling Lights") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Target creature gets -3/-0 until end of turn.\nSurveil 2. (Look at the top two cards of your " +
        "library, then put any number of them into your graveyard and the rest on top of your library in any order.)"

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.ModifyStats(-3, 0, creature) then
            Patterns.Library.surveil(2)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "34"
        artist = "Robbie Trevino"
        imageUri = "https://cards.scryfall.io/normal/front/a/9/a9d4639a-b634-469e-a848-35e3a3dfd47e.jpg?1783934191"
    }
}

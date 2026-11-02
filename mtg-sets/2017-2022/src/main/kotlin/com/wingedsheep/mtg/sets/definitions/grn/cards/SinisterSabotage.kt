package com.wingedsheep.mtg.sets.definitions.grn.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Sinister Sabotage
 * {1}{U}{U}
 * Instant
 * Counter target spell.
 * Surveil 1.
 */
val SinisterSabotage = card("Sinister Sabotage") {
    manaCost = "{1}{U}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Counter target spell.\nSurveil 1. (Look at the top card of your library. You may put it into your graveyard.)"

    spell {
        target(TargetFilter.SpellOnStack)
        effect = Effects.CounterSpell() then
            Patterns.Library.surveil(1)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "54"
        artist = "Mathias Kollros"
        imageUri = "https://cards.scryfall.io/normal/front/6/c/6cbef36d-7170-424f-8fb1-8e7e112b7f0b.jpg?1783934181"
    }
}

package com.wingedsheep.mtg.sets.definitions.grn.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Unexplained Disappearance
 * {1}{U}
 * Instant
 * Return target creature to its owner's hand.
 * Surveil 1.
 */
val UnexplainedDisappearance = card("Unexplained Disappearance") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Return target creature to its owner's hand.\nSurveil 1. (Look at the top card of your library. " +
        "You may put that card into your graveyard.)"

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.ReturnToHand(creature) then
            Patterns.Library.surveil(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "56"
        artist = "Izzy"
        imageUri = "https://cards.scryfall.io/normal/front/9/c/9c99b239-50d2-4393-80ec-94dbfaa6ae70.jpg?1783934181"
    }
}

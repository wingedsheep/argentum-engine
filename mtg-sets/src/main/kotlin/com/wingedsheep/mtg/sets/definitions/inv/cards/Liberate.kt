package com.wingedsheep.mtg.sets.definitions.inv.cards

import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.dsl.ExilePatterns

/**
 * Liberate
 * {1}{W}
 * Instant
 * Exile target creature you control. Return that card to the battlefield under its
 * owner's control at the beginning of the next end step.
 *
 * A "blink" of your own creature: [ExilePatterns.exileUntilEndStep] exiles the target
 * and schedules a delayed trigger at the next end step that returns it to the
 * battlefield. Cards return to the battlefield under their owner's control by default,
 * matching the oracle wording.
 */
val Liberate = card("Liberate") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Exile target creature you control. Return that card to the battlefield " +
        "under its owner's control at the beginning of the next end step."

    spell {
        val creature = target("target creature you control", Targets.CreatureYouControl)
        effect = ExilePatterns.exileUntilEndStep(creature)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "21"
        artist = "Alan Pollack"
        imageUri = "https://cards.scryfall.io/normal/front/9/6/96794470-31ea-478f-b11c-dc8342a508e2.jpg?1562925324"
    }
}

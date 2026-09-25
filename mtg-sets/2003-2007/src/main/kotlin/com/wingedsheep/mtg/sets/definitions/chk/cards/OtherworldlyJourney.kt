package com.wingedsheep.mtg.sets.definitions.chk.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Otherworldly Journey
 * {1}{W}
 * Instant — Arcane
 * Exile target creature. At the beginning of the next end step, return that card to the
 * battlefield under its owner's control with a +1/+1 counter on it.
 *
 * Galepowder Mage's blink shape — exile, then a delayed end-step `Move` of the same card — with
 * `addCounterType` putting the +1/+1 counter on as the card enters rather than after it, so
 * it returns as a new object that already has the counter (2004-12-01 ruling). "Under its
 * owner's control" is the battlefield move's default for a returning card. A token that's
 * exiled ceases to exist and so never comes back.
 */
val OtherworldlyJourney = card("Otherworldly Journey") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Instant — Arcane"
    oracleText = "Exile target creature. At the beginning of the next end step, return that card to " +
        "the battlefield under its owner's control with a +1/+1 counter on it."

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.Exile(creature) then
            Effects.CreateDelayedTrigger(
                step = Step.END,
                effect = Effects.Move(
                    creature,
                    Zone.BATTLEFIELD,
                    addCounterType = CounterType.PLUS_ONE_PLUS_ONE
                )
            )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "37"
        artist = "Vance Kovacs"
        flavorText = "\"The landscape shimmered and I felt a chill breeze. When my vision cleared, I found myself alone among the corpses of my fallen friends.\"\n—Journal found in Numai"
        imageUri = "https://cards.scryfall.io/normal/front/a/2/a293db52-49f2-4a9c-8ee0-054d994b7299.jpg?1783944334"
        ruling("2005-08-01", "Any Aura attached to the creature is put into its owner's graveyard as a state-based action. Equipment attached to the creature become unattached but remain on the battlefield.")
        ruling("2005-08-01", "If Otherworldly Journey is cast during an end step, the creature won't return until the beginning of the next end step.")
        ruling("2004-12-01", "The creature comes back onto the battlefield as a new creature, with one +1/+1 counter on it (plus any other counters that it would normally enter with).")
    }
}

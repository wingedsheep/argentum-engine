package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Test of Priorities
 * {1}{U}
 * Instant
 *
 * Choose one —
 * • Power to Engines — Surveil 2, then draw a card.
 * • Power to Life Support — Exile target creature you control. Return that card to the battlefield
 *   under its owner's control at the beginning of the next end step.
 *
 * The second mode is Otherworldly Journey's blink shape — exile, then a delayed end-step `Move` of
 * the same card, which returns under its owner's control by default. An exiled token ceases to
 * exist and never comes back.
 */
val TestOfPriorities = card("Test of Priorities") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Choose one —\n" +
        "• Power to Engines — Surveil 2, then draw a card.\n" +
        "• Power to Life Support — Exile target creature you control. Return that card to the " +
        "battlefield under its owner's control at the beginning of the next end step."

    spell {
        modal(chooseCount = 1) {
            mode("Power to Engines — Surveil 2, then draw a card") {
                effect = Effects.Surveil(2) then Effects.DrawCards(1)
            }

            mode("Power to Life Support — Exile target creature you control; it returns at the next end step") {
                val creature = target(TargetFilter.CreatureYouControl)
                effect = Effects.Exile(creature) then
                    Effects.CreateDelayedTrigger(
                        step = Step.END,
                        effect = Effects.Move(creature, Zone.BATTLEFIELD)
                    )
            }
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "85"
        artist = "Paolo Puggioni"
        imageUri = "https://cards.scryfall.io/normal/front/8/9/89e3ec23-e4dc-4d58-aeda-9f73ed4504a5.jpg?1793013095"
    }
}

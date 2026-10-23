package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.EmitLibrarySearchedEventEffect
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Emergent Sequence — Strixhaven: School of Mages #129 (canonical printing)
 * {1}{G} · Sorcery
 *
 * Search your library for a basic land card, put it onto the battlefield tapped, then shuffle.
 * That land becomes a 0/0 green and blue Fractal creature that's still a land. Put a +1/+1
 * counter on it for each land you had enter the battlefield under your control this turn.
 *
 * Modelling notes:
 * - The search is an explicit pipeline (gather → chooseUpTo(1) → moveTracked) so the land that
 *   actually entered is a handle the later steps can name.
 * - The land enters first and only then becomes a creature (ruling: it isn't a creature as it
 *   enters, so "creature enters" triggers don't fire). [Effects.BecomeCreature] with
 *   `Duration.Permanent` adds the creature type in Layer 4 while keeping it a land (and its basic
 *   land type), sets its colors to green and blue, and its base P/T to 0/0, with the STX Fractal
 *   token's art as a display override.
 * - The counter count is the per-turn entry log ([DynamicAmounts.landsEnteredUnderControlThisTurn]),
 *   read after the land has entered, so it counts the searched land itself (ruling).
 * - Failing to find leaves nothing to animate or count onto; the library is still shuffled.
 */
val EmergentSequence = card("Emergent Sequence") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Search your library for a basic land card, put it onto the battlefield tapped, " +
        "then shuffle. That land becomes a 0/0 green and blue Fractal creature that's still a " +
        "land. Put a +1/+1 counter on it for each land you had enter the battlefield under your " +
        "control this turn."

    spell {
        effect = Effects.Pipeline {
            val searchable = gather(
                CardSource.FromZone(Zone.LIBRARY, Player.You, GameObjectFilter.BasicLand),
                search = true
            )
            val found = chooseUpTo(
                1,
                from = searchable,
                prompt = "Search your library for a basic land card to put onto the battlefield tapped"
            )
            val land = moveTracked(
                found,
                CardDestination.ToZone(Zone.BATTLEFIELD, placement = ZonePlacement.Tapped)
            )
            run(Effects.ShuffleLibrary())
            run(EmitLibrarySearchedEventEffect)
            run(Effects.BecomeCreature(
                target = land.asTarget,
                power = 0,
                toughness = 0,
                creatureTypes = setOf("Fractal"),
                colors = setOf(Color.GREEN.name, Color.BLUE.name),
                imageUri = "https://cards.scryfall.io/normal/front/9/1/910f48ab-b04e-4874-b31d-a86a7bc5af14.jpg?1783927192",
                duration = Duration.Permanent
            ))
            run(Effects.AddDynamicCounters(
                CounterType.PLUS_ONE_PLUS_ONE,
                DynamicAmounts.landsEnteredUnderControlThisTurn(Player.You),
                land.asTarget
            ))
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "129"
        artist = "Andreas Zafiratos"
        imageUri = "https://cards.scryfall.io/normal/front/0/4/04a81975-5f0e-4593-a2da-b9689f9a985f.jpg?1783927343"
        ruling("2021-04-16", "The land won't be a creature as it enters the battlefield unless some other effect is making it one. Abilities that trigger whenever a creature enters the battlefield won't trigger.")
        ruling("2021-04-16", "The last sentence of Emergent Sequence's ability counts the land it put onto the battlefield.")
    }
}

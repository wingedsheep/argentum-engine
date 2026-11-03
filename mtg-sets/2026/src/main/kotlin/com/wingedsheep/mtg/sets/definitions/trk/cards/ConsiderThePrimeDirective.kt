package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Consider the Prime Directive
 * {2}{U}
 * Instant
 *
 * Choose one —
 * • Interfere — Counter target spell unless its controller pays {3}.
 * • Observe — Tap up to one target creature and put a stun counter on it. Draw a card.
 */
val ConsiderThePrimeDirective = card("Consider the Prime Directive") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Choose one —\n" +
        "• Interfere — Counter target spell unless its controller pays {3}.\n" +
        "• Observe — Tap up to one target creature and put a stun counter on it. Draw a card. " +
        "(If a permanent with a stun counter would become untapped, remove one from it instead.)"

    spell {
        modal(chooseCount = 1) {
            mode("Interfere — Counter target spell unless its controller pays {3}") {
                target(TargetFilter.SpellOnStack)
                effect = Effects.CounterUnlessPays("{3}")
            }

            mode("Observe — Tap up to one target creature and put a stun counter on it. Draw a card") {
                val creature = target(TargetFilter.Creature, optional = true)
                effect = Effects.Tap(creature) then
                    Effects.AddCounters(CounterType.STUN, 1, creature) then
                    Effects.DrawCards(1)
            }
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "51"
        artist = "Zoltan Boros"
        imageUri = "https://cards.scryfall.io/normal/front/4/e/4e8f75b8-cca7-4ed3-90db-bcac02e74611.jpg?1784036751"
    }
}

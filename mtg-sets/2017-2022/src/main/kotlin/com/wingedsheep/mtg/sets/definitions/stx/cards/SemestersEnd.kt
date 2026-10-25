package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.EntryCounter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Semester's End
 * {3}{W}
 * Instant
 * Exile any number of target creatures and/or planeswalkers you control. At the beginning of the
 * next end step, return each of them to the battlefield under its owner's control. Each of them
 * enters with an additional +1/+1 counter on it if it's a creature and an additional loyalty
 * counter on it if it's a planeswalker.
 *
 * Eerie Interlude's multi-blink shape. Both counters ride the return move as [EntryCounter]s, each
 * gated on the card's characteristics as it returns, per the ruling (a creature planeswalker gets
 * both). The loyalty counter joins the printed loyalty in the same entry event.
 */
val SemestersEnd = card("Semester's End") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText =
        "Exile any number of target creatures and/or planeswalkers you control. At the beginning of " +
            "the next end step, return each of them to the battlefield under its owner's control. Each " +
            "of them enters with an additional +1/+1 counter on it if it's a creature and an additional " +
            "loyalty counter on it if it's a planeswalker."

    spell {
        targets(TargetFilter(GameObjectFilter.CreatureOrPlaneswalker.youControl()), optional = true, unlimited = true)
        effect = Effects.ForEachTarget(
            Effects.Move(EffectTarget.ContextTarget(0), Zone.EXILE),
            Effects.CreateDelayedTrigger(
                step = Step.END,
                effect = Effects.Move(
                    target = EffectTarget.ContextTarget(0),
                    destination = Zone.BATTLEFIELD,
                    entryCounters = listOf(
                        EntryCounter(CounterType.PLUS_ONE_PLUS_ONE, onlyIf = GameObjectFilter.Creature),
                        EntryCounter(CounterType.LOYALTY, onlyIf = GameObjectFilter.Planeswalker),
                    ),
                ),
            ),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "27"
        artist = "Alayna Danner"
        imageUri = "https://cards.scryfall.io/normal/front/e/f/ef4743f7-28db-4d72-bfce-09cfbe413514.jpg?1783927387"
    }
}

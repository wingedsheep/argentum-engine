package com.wingedsheep.mtg.sets.definitions.ncc.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.predicates.ControllerPredicate
import com.wingedsheep.sdk.scripting.predicates.StatePredicate

/**
 * Shield Broker
 * {3}{U}{U}
 * Creature — Octopus Advisor
 * 3/4
 * When this creature enters, put a shield counter on target noncommander creature you don't
 * control. You gain control of that creature for as long as it has a shield counter on it.
 *
 * The control change is a floating effect keyed on `WhileAffectedHasCounter(SHIELD)`: the
 * projector gates it per frame and `EndedDurationExpiryCheck` latches it off for good once the
 * counter leaves (CR 611.2b), so a later shield counter doesn't hand the creature back. The
 * effect doesn't depend on Shield Broker staying on the battlefield.
 */
val ShieldBroker = card("Shield Broker") {
    manaCost = "{3}{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Octopus Advisor"
    oracleText = "When this creature enters, put a shield counter on target noncommander creature you don't control. " +
        "You gain control of that creature for as long as it has a shield counter on it. " +
        "(If it would be dealt damage or destroyed, remove a shield counter from it instead.)"
    power = 3
    toughness = 4

    triggeredAbility {
        trigger = Triggers.self.enters()
        val t = target(
            TargetFilter(
                GameObjectFilter.Creature
                    .withStatePredicate(StatePredicate.Not(StatePredicate.IsCommander))
                    .withControllerPredicate(ControllerPredicate.Not(ControllerPredicate.ControlledByYou))
            )
        )
        effect = Effects.AddCounters(CounterType.SHIELD, 1, t) then
            Effects.GainControl(t, Duration.WhileAffectedHasCounter(CounterType.SHIELD))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "29"
        artist = "Daniel Ljunggren"
        imageUri = "https://cards.scryfall.io/normal/front/a/3/a319afae-019c-408d-a57b-b7bf0b1598bd.jpg?1783923369"
        ruling("2022-04-29", "If the creature loses its shield counter during combat, it will be removed from combat when it changes controllers. If it hasn't already assigned combat damage by that time, it won't get to deal combat damage.")
    }
}

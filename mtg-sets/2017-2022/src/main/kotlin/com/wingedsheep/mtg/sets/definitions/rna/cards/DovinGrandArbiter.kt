package com.wingedsheep.mtg.sets.definitions.rna.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.DelayedTriggerExpiry
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Dovin, Grand Arbiter
 * {1}{W}{U}
 * Legendary Planeswalker — Dovin
 * Starting Loyalty: 3
 *
 * +1: Until end of turn, whenever a creature you control deals combat damage to a player, put a
 * loyalty counter on Dovin.
 * −1: Create a 1/1 colorless Thopter artifact creature token with flying. You gain 1 life.
 * −7: Look at the top ten cards of your library. Put three of them into your hand and the rest on
 * the bottom of your library in a random order.
 *
 * The +1 is Subira's turn-scoped delayed trigger (`expiry = EndOfTurn`, `fireOnce = false`); it
 * triggers once per creature that deals combat damage to a player, and each resolution adds one
 * loyalty counter to the delayed trigger's source — Dovin — regardless of the damage dealt (ruling).
 */
val DovinGrandArbiter = card("Dovin, Grand Arbiter") {
    manaCost = "{1}{W}{U}"
    colorIdentity = "WU"
    typeLine = "Legendary Planeswalker — Dovin"
    startingLoyalty = 3
    oracleText = "+1: Until end of turn, whenever a creature you control deals combat damage to a player, " +
        "put a loyalty counter on Dovin.\n" +
        "−1: Create a 1/1 colorless Thopter artifact creature token with flying. You gain 1 life.\n" +
        "−7: Look at the top ten cards of your library. Put three of them into your hand and the rest on " +
        "the bottom of your library in a random order."

    // +1: Until end of turn, whenever a creature you control deals combat damage to a player,
    // put a loyalty counter on Dovin.
    loyaltyAbility(+1) {
        effect = Effects.CreateDelayedTrigger(
            trigger = Triggers.a(GameObjectFilter.Creature.youControl())
                .dealsCombatDamage(Recipient.AnyPlayer),
            expiry = DelayedTriggerExpiry.EndOfTurn,
            fireOnce = false,
            effect = Effects.AddCounters(CounterType.LOYALTY, 1, EffectTarget.Self),
        )
    }

    // −1: Create a 1/1 colorless Thopter artifact creature token with flying. You gain 1 life.
    loyaltyAbility(-1) {
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            creatureTypes = setOf("Thopter"),
            keywords = setOf(Keyword.FLYING),
            artifactToken = true,
            imageUri = "https://cards.scryfall.io/normal/front/8/d/8dd768c3-f278-41f8-b329-8403391d6326.jpg?1783933601",
        ) then Effects.GainLife(1)
    }

    // −7: Look at the top ten cards of your library. Put three of them into your hand and the rest
    // on the bottom of your library in a random order.
    loyaltyAbility(-7) {
        effect = Patterns.Library.lookAtTopAndKeep(
            count = 10,
            keepCount = 3,
            keepDestination = CardDestination.ToZone(Zone.HAND),
            restDestination = CardDestination.ToZone(Zone.LIBRARY, placement = ZonePlacement.Bottom),
            restOrder = CardOrder.Random,
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "167"
        artist = "Kieran Yanner"
        imageUri = "https://cards.scryfall.io/normal/front/e/6/e6784910-0204-4a39-bb38-50daa03e94c2.jpg?1783933653"
        ruling(
            "2019-01-25",
            "The delayed triggered ability created by Dovin's first ability triggers for each creature you control that deals combat damage to a player this turn. Dovin gets only one loyalty counter as each ability resolves, no matter how much damage was dealt.",
        )
        ruling(
            "2019-01-25",
            "If you have fewer than ten cards in your library as Dovin's last ability resolves, look at however many cards you have, put three of them into your hand (or all of them if you have fewer than three cards in your library), and then put the rest back in a random order.",
        )
    }
}

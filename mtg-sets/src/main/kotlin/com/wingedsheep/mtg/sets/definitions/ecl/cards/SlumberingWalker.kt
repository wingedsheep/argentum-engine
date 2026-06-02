package com.wingedsheep.mtg.sets.definitions.ecl.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ReflexiveTriggerEffect
import com.wingedsheep.sdk.scripting.events.CounterTypeFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Slumbering Walker
 * {3}{W}{W}
 * Creature — Giant Warrior
 * 4/7
 *
 * This creature enters with two -1/-1 counters on it.
 * At the beginning of your end step, you may remove a counter from this creature.
 * When you do, return target creature card with power 2 or less from your graveyard
 * to the battlefield.
 */
val SlumberingWalker = card("Slumbering Walker") {
    manaCost = "{3}{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Giant Warrior"
    power = 4
    toughness = 7
    oracleText = "This creature enters with two -1/-1 counters on it.\n" +
        "At the beginning of your end step, you may remove a counter from this creature. " +
        "When you do, return target creature card with power 2 or less from your graveyard " +
        "to the battlefield."

    replacementEffect(EntersWithCounters(
        counterType = CounterTypeFilter.MinusOneMinusOne,
        count = 2,
        selfOnly = true
    ))

    triggeredAbility {
        trigger = Triggers.YourEndStep
        triggerCondition = Conditions.SourceHasCounter(CounterTypeFilter.MinusOneMinusOne)
        effect = ReflexiveTriggerEffect(
            action = Effects.RemoveCounters(Counters.MINUS_ONE_MINUS_ONE, 1, EffectTarget.Self),
            optional = true,
            reflexiveEffect = Effects.Move(
                target = EffectTarget.ContextTarget(0),
                destination = Zone.BATTLEFIELD
            ),
            reflexiveTargetRequirements = listOf(
                TargetObject(
                    filter = TargetFilter(
                        GameObjectFilter.Creature.ownedByYou().powerAtMost(2),
                        zone = Zone.GRAVEYARD
                    )
                )
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "35"
        artist = "Jakub Kasper"
        imageUri = "https://cards.scryfall.io/normal/front/8/1/81e82915-e734-4754-829f-f5da6a7c550d.jpg?1767862426"
        ruling(
            "2025-11-17",
            "You don't choose a target for Slumbering Walker's ability at the time it " +
                "triggers. Rather, a second \"reflexive\" ability triggers when you remove " +
                "a counter from Slumbering Walker this way. You choose a target for that " +
                "ability as it goes on the stack. Each player may respond to this " +
                "triggered ability as normal."
        )
    }
}

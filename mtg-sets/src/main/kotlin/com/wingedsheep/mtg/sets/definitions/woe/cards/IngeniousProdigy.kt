package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBeBlockedBy
import com.wingedsheep.sdk.scripting.EntersWithDynamicCounters
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.scripting.effects.RemoveCountersEffect
import com.wingedsheep.sdk.scripting.events.CounterTypeFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.values.EntityReference

/**
 * Ingenious Prodigy
 * {X}{U}
 * Creature — Human Wizard
 * 0/1
 *
 * Skulk
 * This creature enters with X +1/+1 counters on it.
 * At the beginning of your upkeep, if this creature has one or more +1/+1 counters on it, you may
 * remove a +1/+1 counter from it. If you do, draw a card.
 *
 * Skulk is expressed by its rules text rather than a keyword badge: projected blocker power is
 * compared with the Prodigy's projected power. The upkeep clause is an intervening-if condition,
 * checked both when it would trigger and again on resolution. The optional effect keeps removing
 * the counter and drawing the card in one yes/no branch.
 */
val IngeniousProdigy = card("Ingenious Prodigy") {
    manaCost = "{X}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Wizard"
    oracleText = "Skulk (This creature can't be blocked by creatures with greater power.)\n" +
        "This creature enters with X +1/+1 counters on it.\n" +
        "At the beginning of your upkeep, if this creature has one or more +1/+1 counters on it, " +
        "you may remove a +1/+1 counter from it. If you do, draw a card."
    power = 0
    toughness = 1

    staticAbility {
        ability = CantBeBlockedBy(
            GameObjectFilter.Creature.powerGreaterThanEntity(EntityReference.Source)
        )
    }

    replacementEffect(
        EntersWithDynamicCounters(
            counterType = CounterTypeFilter.PlusOnePlusOne,
            count = DynamicAmount.CastX,
        )
    )

    triggeredAbility {
        trigger = Triggers.YourUpkeep
        triggerCondition = Conditions.SourceHasCounter(CounterTypeFilter.PlusOnePlusOne)
        effect = MayEffect(
            Effects.Composite(
                RemoveCountersEffect(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self),
                Effects.DrawCards(1),
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "56"
        artist = "Brian Valeza"
        imageUri = "https://cards.scryfall.io/normal/front/c/f/cf224968-b676-40dd-83c1-a9ee2ceba574.jpg?1783915119"
    }
}

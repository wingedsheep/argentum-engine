package com.wingedsheep.mtg.sets.definitions.aer.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Scrounging Bandar
 * {1}{G}
 * Creature — Cat Monkey
 * 0/0
 * This creature enters with two +1/+1 counters on it.
 * At the beginning of your upkeep, you may move any number of +1/+1 counters from this creature
 * onto another target creature.
 *
 * The target is chosen as the trigger goes on the stack; how many counters move (0 = decline the
 * "may") is chosen on resolution, capped at the +1/+1 counters the Bandar then carries. Only
 * +1/+1 counters move — `MoveCounters` is single-kind, unlike `MoveChosenCountersToTarget`.
 */
val ScroungingBandar = card("Scrounging Bandar") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Cat Monkey"
    power = 0
    toughness = 0
    oracleText = "This creature enters with two +1/+1 counters on it.\n" +
        "At the beginning of your upkeep, you may move any number of +1/+1 counters from this " +
        "creature onto another target creature."

    replacementEffect(
        EntersWithCounters(
            counterType = CounterType.PLUS_ONE_PLUS_ONE,
            count = 2,
            selfOnly = true
        )
    )

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        val creature = target(TargetFilter.OtherCreature)
        effect = Effects.ChooseNumberThen(
            maxValue = DynamicAmounts.countersOnSelf(CounterType.PLUS_ONE_PLUS_ONE),
            prompt = "Move how many +1/+1 counters from Scrounging Bandar?",
            then = Effects.MoveCounters(
                counterType = CounterType.PLUS_ONE_PLUS_ONE,
                amount = DynamicAmounts.xValue(),
                source = EffectTarget.Self,
                destination = creature
            )
        )
        description = "At the beginning of your upkeep, you may move any number of +1/+1 counters " +
            "from this creature onto another target creature."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "124"
        artist = "Shreya Shetty"
        flavorText = "\"It was right here a second ago . . .\""
        imageUri = "https://cards.scryfall.io/normal/front/7/6/761a11a7-175d-440d-b09d-918572c8e5d3.jpg?1783936737"
        ruling("2020-11-10", "You choose a target creature as Scrounging Bandar's triggered ability is put onto the stack. You choose how many counters to move (if any) as that ability resolves. If that creature becomes an illegal target or if Scrounging Bandar has left the battlefield, you can't move any counters.")
        ruling("2020-11-10", "To move a counter from one creature to another, the counter is removed from the first creature and put onto the second. Any abilities that care about a counter being removed from or placed on a creature will apply.")
    }
}

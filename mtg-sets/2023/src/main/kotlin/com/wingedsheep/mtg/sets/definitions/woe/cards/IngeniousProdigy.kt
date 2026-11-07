package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithDynamicCounters
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.core.Step

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
 * The upkeep clause is an intervening-if condition,
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

    keywords(Keyword.SKULK)

    replacementEffect(
        EntersWithDynamicCounters(
            counterType = CounterType.PLUS_ONE_PLUS_ONE,
            count = DynamicAmounts.castX(),
        )
    )

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        interveningIf = Conditions.SourceHasCounter(CounterType.PLUS_ONE_PLUS_ONE)
        effect = Effects.May(
            Effects.RemoveCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self) then
                Effects.DrawCards(1)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "56"
        artist = "Brian Valeza"
        imageUri = "https://cards.scryfall.io/normal/front/c/f/cf224968-b676-40dd-83c1-a9ee2ceba574.jpg?1783915119"
    }
}

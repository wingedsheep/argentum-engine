package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.div
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithDynamicCounters
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Ochre Jelly
 * {X}{G}
 * Creature — Ooze
 * 0/0
 * Trample
 * This creature enters with X +1/+1 counters on it.
 * Split — When this creature dies, if it had two or more +1/+1 counters on it, create a token
 * that's a copy of it at the beginning of the next end step. The token enters with half that many
 * +1/+1 counters on it, rounded down.
 *
 * "That many" is the dying Jelly's last-known +1/+1 counter count; the delayed trigger freezes both
 * that count and the Jelly's copiable values when it is scheduled, so a dying Jelly *token* (which
 * ceases to exist) still splits. The token copy's own "enters with X counters" sees X = 0.
 */
val OchreJelly = card("Ochre Jelly") {
    manaCost = "{X}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Ooze"
    power = 0
    toughness = 0
    oracleText = "Trample\n" +
        "This creature enters with X +1/+1 counters on it.\n" +
        "Split — When this creature dies, if it had two or more +1/+1 counters on it, create a " +
        "token that's a copy of it at the beginning of the next end step. The token enters with " +
        "half that many +1/+1 counters on it, rounded down."

    keywords(Keyword.TRAMPLE)

    replacementEffect(EntersWithDynamicCounters(count = DynamicAmounts.xValue()))

    triggeredAbility {
        trigger = Triggers.self.dies()
        interveningIf = Conditions.CompareAmounts(
            DynamicAmounts.lastKnownSourceCounters(CounterType.PLUS_ONE_PLUS_ONE),
            ComparisonOperator.GTE,
            2
        )
        effect = Effects.CreateDelayedTrigger(
            step = Step.END,
            effect = Effects.CreateTokenCopyOfTarget(
                target = EffectTarget.Self,
                initialCounters = mapOf(
                    CounterType.PLUS_ONE_PLUS_ONE to
                        DynamicAmounts.lastKnownSourceCounters(CounterType.PLUS_ONE_PLUS_ONE) / 2
                )
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "196"
        artist = "Daarken"
        imageUri = "https://cards.scryfall.io/normal/front/9/f/9f23cfb7-c6b6-4f04-abba-5b2a6117ea12.jpg?1783926458"
    }
}

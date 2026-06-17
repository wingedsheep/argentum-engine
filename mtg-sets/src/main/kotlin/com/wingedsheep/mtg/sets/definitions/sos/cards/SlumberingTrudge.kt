package com.wingedsheep.mtg.sets.definitions.sos.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.EntersWithDynamicCounters
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.events.CounterTypeFilter
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Slumbering Trudge — Secrets of Strixhaven #160
 * {X}{G} · Creature — Plant Beast · 6/6
 *
 * This creature enters with a number of stun counters on it equal to three minus X.
 * If X is 2 or less, it enters tapped. (If a permanent with a stun counter would become
 * untapped, remove one from it instead.)
 *
 * Both clauses read the cast-time X via [DynamicAmount.CastX], which rides the spell's stable
 * entity onto the battlefield so the enters-with replacement and the enters-tapped replacement
 * see the same value:
 *  - [EntersWithDynamicCounters] adds `3 - X` stun counters (clamped to 0 at or above X = 3 by
 *    the replacement executor's non-negative count), using [CounterTypeFilter.Named] with the
 *    [Counters.STUN] kind so the engine's stun-counter untap replacement (CR 122.1c) keeps it
 *    tapped.
 *  - [EntersTapped] taps it on entry *unless* X is 3 or more (the inverse of "X is 2 or less").
 */
val SlumberingTrudge = card("Slumbering Trudge") {
    manaCost = "{X}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Plant Beast"
    power = 6
    toughness = 6
    oracleText = "This creature enters with a number of stun counters on it equal to three minus X. " +
        "If X is 2 or less, it enters tapped. (If a permanent with a stun counter would become " +
        "untapped, remove one from it instead.)"

    replacementEffect(
        EntersWithDynamicCounters(
            counterType = CounterTypeFilter.Named(Counters.STUN),
            count = DynamicAmount.Subtract(DynamicAmount.Fixed(3), DynamicAmount.CastX),
        )
    )
    replacementEffect(
        EntersTapped(
            unlessCondition = Conditions.CompareAmounts(
                DynamicAmount.CastX,
                ComparisonOperator.GTE,
                DynamicAmount.Fixed(3),
            )
        )
    )

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "160"
        artist = "Tuan Duong Chu"
        flavorText = "\"Yes, she is blocking the path. Are you volunteering to move her?\"\n—Tam, to Kirol"
        imageUri = "https://cards.scryfall.io/normal/front/3/a/3a925370-58ac-4181-9acc-db7b0e0abf17.jpg?1776584117"
    }
}

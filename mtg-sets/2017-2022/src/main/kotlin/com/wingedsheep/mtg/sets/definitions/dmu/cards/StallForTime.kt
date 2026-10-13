package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Stall for Time — Dominaria United #34
 * {2}{W} · Instant
 *
 * "Up to two target creatures" is one requirement taking zero to two objects (Out Cold's shape).
 * The kick check branches outside the per-target iteration: kicked, each chosen creature is tapped
 * and gets a stun counter; unkicked, each is only tapped. The draw happens either way.
 */
val StallForTime = card("Stall for Time") {
    manaCost = "{2}{W}"
    colorIdentity = "WU"
    typeLine = "Instant"
    oracleText = "Kicker {1}{U} (You may pay an additional {1}{U} as you cast this spell.)\n" +
        "Tap up to two target creatures. If this spell was kicked, put a stun counter on each of " +
        "those creatures. (If a permanent with a stun counter would become untapped, remove one " +
        "from it instead.)\n" +
        "Draw a card."

    keywordAbility(KeywordAbility.kicker("{1}{U}"))

    spell {
        targets(TargetFilter.Creature, count = 2, optional = true)
        effect = Effects.If(
            Conditions.WasKicked,
            Effects.ForEachTarget(
                Effects.Tap(EffectTarget.ContextTarget(0)),
                Effects.AddCounters(CounterType.STUN, 1, EffectTarget.ContextTarget(0))
            ),
            Effects.ForEachTarget(Effects.Tap(EffectTarget.ContextTarget(0)))
        ) then Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "34"
        artist = "Ryan Alexander Lee"
        imageUri = "https://cards.scryfall.io/normal/front/f/3/f346defa-ee4a-404e-8154-0e33ebe9102c.jpg?1783921358"
    }
}

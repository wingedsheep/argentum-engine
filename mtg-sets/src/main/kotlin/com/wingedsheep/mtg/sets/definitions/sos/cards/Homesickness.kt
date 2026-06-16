package com.wingedsheep.mtg.sets.definitions.sos.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.AddCountersEffect
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.effects.DrawCardsEffect
import com.wingedsheep.sdk.scripting.effects.ForEachTargetEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetCreature
import com.wingedsheep.sdk.scripting.targets.TargetPlayer

/**
 * Homesickness
 * {4}{U}{U}
 * Instant
 * Target player draws two cards. Tap up to two target creatures. Put a stun counter on each of them.
 * (If a permanent with a stun counter would become untapped, remove one from it instead.)
 *
 * Three distinct target requirements: one player (index 0) plus up to two creatures (indices 1–2).
 * The player draw uses `ContextTarget(0)` directly. The tap + stun-counter body runs once per chosen
 * target via [ForEachTargetEffect], but [IterationSpace.Targets] iterates *every* target — including
 * the player — so the body is gated with [Conditions.TargetMatchesFilter]`(Creature)`. On the player
 * iteration the gate is false and nothing happens; on each creature iteration it taps that creature
 * and puts a stun counter on it. This keeps the player draw and the per-creature effects in one
 * resolution without tapping/stunning the player. Stun-counter untap replacement (CR 122.1d) is
 * already wired via `untapOrConsumeStun`.
 */
val Homesickness = card("Homesickness") {
    manaCost = "{4}{U}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Target player draws two cards. Tap up to two target creatures. Put a stun counter " +
        "on each of them. (If a permanent with a stun counter would become untapped, remove one from " +
        "it instead.)"

    spell {
        target("target player", TargetPlayer())
        target("up to two target creatures", TargetCreature(count = 2, optional = true))
        effect = DrawCardsEffect(2, EffectTarget.ContextTarget(0)).then(
            ForEachTargetEffect(
                listOf(
                    ConditionalEffect(
                        condition = Conditions.TargetMatchesFilter(GameObjectFilter.Creature),
                        effect = Effects.Tap(EffectTarget.ContextTarget(0))
                            .then(AddCountersEffect(Counters.STUN, 1, EffectTarget.ContextTarget(0))),
                    )
                )
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "53"
        artist = "Caroline Gariba"
        flavorText = "\"We know you're working hard. Dress warm and get enough sleep. Dad says hi!\"\n" +
            "—Love, Mom"
        imageUri = "https://cards.scryfall.io/normal/front/6/e/6e4a1f82-b0b1-4608-91f8-130bee731435.jpg?1775937280"
    }
}

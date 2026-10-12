package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Involuntary Cooldown
 * {3}{U}
 * Sorcery
 * Tap up to two target artifacts and/or creatures. Put two stun counters on each of them.
 *
 * Scroll of Isildur's chapter II shape: zero to two optional targets, each tapped and then given
 * two stun counters by [Effects.ForEachTarget].
 */
val InvoluntaryCooldown = card("Involuntary Cooldown") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "Tap up to two target artifacts and/or creatures. Put two stun counters on each of them. " +
        "(If a permanent with a stun counter would become untapped, remove one from it instead.)"

    spell {
        targets(TargetFilter.CreatureOrArtifact, count = 2, optional = true)
        effect = Effects.ForEachTarget(
            Effects.Tap(EffectTarget.ContextTarget(0)),
            Effects.AddCounters(CounterType.STUN, 2, EffectTarget.ContextTarget(0))
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "53"
        artist = "Eelis Kyttanen"
        flavorText = "\"Back to the ivory towers! This won't hold for long!\"\n—Vayor, Third Path cryomancer"
        imageUri = "https://cards.scryfall.io/normal/front/d/4/d4fcaf6d-6491-4459-98b0-b3c7d43df59e.jpg?1783920111"
    }
}

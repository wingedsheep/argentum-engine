package com.wingedsheep.mtg.sets.definitions.spe.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Pumpkin Bombs
 * {1}{R}
 * Artifact
 * {T}, Discard two cards: Draw three cards, then put a fuse counter on this artifact. It deals
 * damage equal to the number of fuse counters on it to target opponent. They gain control of this
 * artifact.
 *
 * The bomb keeps its fuse counters as it changes hands, so each activation hits harder than the
 * last. Damage is counted after the new counter lands; control passes to the damaged opponent.
 */
val PumpkinBombs = card("Pumpkin Bombs") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Artifact"
    oracleText = "{T}, Discard two cards: Draw three cards, then put a fuse counter on this artifact. " +
        "It deals damage equal to the number of fuse counters on it to target opponent. " +
        "They gain control of this artifact."

    activatedAbility {
        cost = Costs.Composite(Costs.Tap, Costs.Discard(count = 2))
        val opponent = target(Targets.Opponent)
        effect = Effects.DrawCards(3) then
            Effects.AddCounters(CounterType.FUSE, 1, EffectTarget.Self) then
            Effects.DealDamage(DynamicAmounts.countersOnSelf(CounterType.FUSE), opponent) then
            Effects.GiveControl(permanent = EffectTarget.Self, newController = opponent)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "26"
        artist = "Dan Dos Santos"
        imageUri = "https://cards.scryfall.io/normal/front/6/e/6e9a83b6-2881-4ba3-96d4-0fd89110837d.jpg?1783905367"
    }
}

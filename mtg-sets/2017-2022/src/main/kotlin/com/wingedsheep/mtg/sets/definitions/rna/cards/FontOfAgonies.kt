package com.wingedsheep.mtg.sets.definitions.rna.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Font of Agonies — Ravnica Allegiance #74
 * {B} · Enchantment
 *
 * Whenever you pay life, put that many blood counters on this enchantment.
 * {1}{B}, Remove four blood counters from this enchantment: Destroy target creature.
 *
 * `Triggers.you.paysLife()` fires once per life payment only — damage and "loses N life" effects
 * aren't payments — and the amount paid is the life the payment took off your total.
 */
val FontOfAgonies = card("Font of Agonies") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Enchantment"
    oracleText = "Whenever you pay life, put that many blood counters on this enchantment.\n" +
        "{1}{B}, Remove four blood counters from this enchantment: Destroy target creature."

    triggeredAbility {
        trigger = Triggers.you.paysLife()
        effect = Effects.AddDynamicCounters(CounterType.BLOOD, DynamicAmounts.triggerLifeLost(), EffectTarget.Self)
    }

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{1}{B}"),
            Costs.RemoveCounterFromSelf(CounterType.BLOOD, 4)
        )
        val creature = target(TargetFilter.Creature)
        effect = Effects.Destroy(creature)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "74"
        artist = "Jason A. Engle"
        flavorText = "\"No worthwhile reward is easily gained.\"\n—Nicol Bolas"
        imageUri = "https://cards.scryfall.io/normal/front/d/7/d7c02aec-4a89-4ccc-8525-6f979c10d799.jpg?1783933694"
    }
}

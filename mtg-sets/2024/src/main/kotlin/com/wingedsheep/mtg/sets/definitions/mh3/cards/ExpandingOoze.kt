package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.predicates.StatePredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Expanding Ooze {1}{B}{G}
 * Creature — Ooze
 * 3/3
 * {B}{G}: Adapt 1.
 * Whenever this creature attacks, put a +1/+1 counter on target modified creature you control.
 *
 * Adapt is the zero-counter gate over AddCounters (CR 701.46a). "Modified" (CR 700.9) is
 * [StatePredicate.IsModified] — counters, Equipment, or Auras its controller controls — checked on
 * the target at declaration and again on resolution. The Ooze itself is a legal target once it has
 * a counter.
 */
val ExpandingOoze = card("Expanding Ooze") {
    manaCost = "{1}{B}{G}"
    colorIdentity = "BG"
    typeLine = "Creature — Ooze"
    oracleText = "{B}{G}: Adapt 1. (If this creature has no +1/+1 counters on it, put a +1/+1 counter on it.)\n" +
        "Whenever this creature attacks, put a +1/+1 counter on target modified creature you control. " +
        "(Equipment, Auras you control, and counters are modifications.)"
    power = 3
    toughness = 3

    // {B}{G}: Adapt 1.
    activatedAbility {
        cost = Costs.Mana("{B}{G}")
        effect = Effects.If(
            Conditions.Not(Conditions.SourceHasCounter(CounterType.PLUS_ONE_PLUS_ONE)),
            Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self),
        )
    }

    triggeredAbility {
        trigger = Triggers.self.attacks()
        val modified = target(
            TargetFilter(GameObjectFilter.Creature.youControl().withStatePredicate(StatePredicate.IsModified))
        )
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, modified)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "184"
        artist = "Randy Gallegos"
        flavorText = "\"It eats the royal garbage. Of course it's growing!\"\n—Prince Bolbuss"
        imageUri = "https://cards.scryfall.io/normal/front/b/b/bbdb095d-b826-4e3e-8c61-0d408e52d6b8.jpg?1783911251"
    }
}

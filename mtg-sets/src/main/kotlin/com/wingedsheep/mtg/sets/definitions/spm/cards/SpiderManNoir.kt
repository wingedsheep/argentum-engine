package com.wingedsheep.mtg.sets.definitions.spm.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.events.AttackPredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Spider-Man Noir — Marvel's Spider-Man #67
 * {4}{B} · Legendary Creature — Spider Human Hero · 4/4
 *
 * Menace
 * Whenever a creature you control attacks alone, put a +1/+1 counter on it. Then surveil X,
 * where X is the number of counters on it.
 *
 * The trigger is the Squall / Thoughtweft Imbuer "attacks alone" shape — an ANY-bound
 * [Triggers.attacks] with [AttackPredicate.Alone] over "creature you control" — so "it" is the
 * lone attacker ([EffectTarget.TriggeringEntity]), not Spider-Man Noir. The +1/+1 counter lands
 * first, then "surveil X" reads the number of counters on that same triggering creature *after*
 * the counter is added (so X is always at least 1). X counts counters of every kind
 * ([CounterTypeFilter.Any] via [DynamicAmounts.countersOnTriggering]), matching the reminder-free
 * "number of counters on it". Surveil X uses the dynamic [Effects.Surveil] overload.
 */
val SpiderManNoir = card("Spider-Man Noir") {
    manaCost = "{4}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Creature — Spider Human Hero"
    power = 4
    toughness = 4
    oracleText = "Menace\nWhenever a creature you control attacks alone, put a +1/+1 counter on it. Then surveil X, where X is the number of counters on it. (Look at the top X cards of your library, then put any number of them into your graveyard and the rest on top of your library in any order.)"

    keywords(Keyword.MENACE)

    triggeredAbility {
        trigger = Triggers.attacks(
            filter = GameObjectFilter.Creature.youControl(),
            requires = setOf(AttackPredicate.Alone),
            binding = TriggerBinding.ANY,
        )
        effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.TriggeringEntity) then
            Effects.Surveil(DynamicAmounts.countersOnTriggering())
        description = "Whenever a creature you control attacks alone, put a +1/+1 counter on it. " +
            "Then surveil X, where X is the number of counters on it."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "67"
        artist = "Xabi Gaztelua"
        imageUri = "https://cards.scryfall.io/normal/front/b/c/bc64366c-2691-48cd-bb4b-a4b088c6f16b.jpg?1783905339"
    }
}

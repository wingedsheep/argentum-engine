package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantDynamicStats
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Evolving Adaptive — Phyrexia: All Will Be One #167
 * {G} · Creature — Phyrexian Warrior · 0/0 · Uncommon
 *
 * This creature enters with an oil counter on it.
 * This creature gets +1/+1 for each oil counter on it.
 * Whenever another creature you control enters, if that creature has greater power or toughness
 * than this creature, put an oil counter on this creature.
 *
 * Necrosquito's oil-counter body plus Hulkling, Burgeoning Bruiser's intervening-if (CR 603.4):
 * the triggering creature's projected power or toughness is compared against this creature's,
 * both when the trigger would trigger and again on resolution.
 */
val EvolvingAdaptive = card("Evolving Adaptive") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Creature — Phyrexian Warrior"
    power = 0
    toughness = 0
    oracleText = "This creature enters with an oil counter on it.\n" +
        "This creature gets +1/+1 for each oil counter on it.\n" +
        "Whenever another creature you control enters, if that creature has greater power or " +
        "toughness than this creature, put an oil counter on this creature."

    replacementEffect(EntersWithCounters(counterType = CounterType.OIL, count = 1, selfOnly = true))

    staticAbility {
        val oil = DynamicAmounts.countersOnSelf(CounterType.OIL)
        ability = GrantDynamicStats(
            filter = GroupFilter.source(),
            powerBonus = oil,
            toughnessBonus = oil
        )
    }

    triggeredAbility {
        trigger = Triggers.another(GameObjectFilter.Creature.youControl()).enters()
        interveningIf = Conditions.Any(
            Conditions.CompareAmounts(
                DynamicAmounts.triggeringPower(),
                ComparisonOperator.GT,
                DynamicAmounts.sourcePower()
            ),
            Conditions.CompareAmounts(
                DynamicAmounts.triggeringToughness(),
                ComparisonOperator.GT,
                DynamicAmounts.sourceToughness()
            )
        )
        effect = Effects.AddCounters(CounterType.OIL, 1, EffectTarget.Self)
        description = "Whenever another creature you control enters, if that creature has greater " +
            "power or toughness than this creature, put an oil counter on this creature."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "167"
        artist = "John Di Giovanni"
        imageUri = "https://cards.scryfall.io/normal/front/1/3/13366c60-1200-4ad2-bbf4-77596121fcd2.jpg?1783918017"
    }
}

package com.wingedsheep.mtg.sets.definitions.ths.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Ordeal of Thassa
 * {1}{U}
 * Enchantment — Aura
 *
 * Enchant creature
 * Whenever enchanted creature attacks, put a +1/+1 counter on it. Then if it has three or more
 * +1/+1 counters on it, sacrifice this Aura.
 * When you sacrifice this Aura, draw two cards.
 */
val OrdealOfThassa = card("Ordeal of Thassa") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "Whenever enchanted creature attacks, put a +1/+1 counter on it. Then if it has three or more " +
        "+1/+1 counters on it, sacrifice this Aura.\n" +
        "When you sacrifice this Aura, draw two cards."

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    // Whenever enchanted creature attacks, put a +1/+1 counter on it.
    // Then if it has three or more +1/+1 counters on it, sacrifice this Aura.
    triggeredAbility {
        trigger = Triggers.attached.attacks()
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.EnchantedCreature) then
            Effects.If(
                condition = Conditions.CompareAmounts(
                    left = DynamicAmounts.countersOnTriggering(CounterType.PLUS_ONE_PLUS_ONE),
                    operator = ComparisonOperator.GTE,
                    right = 3
                ),
                then = Effects.SacrificeTarget(EffectTarget.Self)
            )
    }

    // When you sacrifice this Aura, draw two cards.
    triggeredAbility {
        trigger = Triggers.self.isSacrificed()
        effect = Effects.DrawCards(2)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "58"
        artist = "Howard Lyon"
        imageUri = "https://cards.scryfall.io/normal/front/f/a/fa764244-c5d5-4f27-9c56-866566a1404a.jpg?1783939794"
    }
}

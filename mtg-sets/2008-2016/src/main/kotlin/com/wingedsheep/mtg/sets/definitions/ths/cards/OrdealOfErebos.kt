package com.wingedsheep.mtg.sets.definitions.ths.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Ordeal of Erebos
 * {1}{B}
 * Enchantment — Aura
 *
 * Enchant creature
 * Whenever enchanted creature attacks, put a +1/+1 counter on it. Then if it has three or more
 * +1/+1 counters on it, sacrifice this Aura.
 * When you sacrifice this Aura, target player discards two cards.
 */
val OrdealOfErebos = card("Ordeal of Erebos") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "Whenever enchanted creature attacks, put a +1/+1 counter on it. Then if it has three or more " +
        "+1/+1 counters on it, sacrifice this Aura.\n" +
        "When you sacrifice this Aura, target player discards two cards."

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

    // When you sacrifice this Aura, target player discards two cards.
    triggeredAbility {
        trigger = Triggers.self.isSacrificed()
        val victim = target(Targets.Player)
        effect = Patterns.Hand.discardCards(2, victim)
        description = "When you sacrifice this Aura, target player discards two cards."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "99"
        artist = "Tyler Jacobson"
        imageUri = "https://cards.scryfall.io/normal/front/9/e/9e02ddba-fdc1-4d7b-89eb-00a0417f8963.jpg?1783939773"
    }
}

package com.wingedsheep.mtg.sets.definitions.ddl.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Ordeal of Purphoros
 * {1}{R}
 * Enchantment — Aura
 *
 * Enchant creature
 * Whenever enchanted creature attacks, put a +1/+1 counter on it. Then if it has three or more
 * +1/+1 counters on it, sacrifice this Aura.
 * When you sacrifice this Aura, it deals 3 damage to any target.
 */
val OrdealOfPurphoros = card("Ordeal of Purphoros") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "Whenever enchanted creature attacks, put a +1/+1 counter on it. Then if it has three or more " +
        "+1/+1 counters on it, sacrifice this Aura.\n" +
        "When you sacrifice this Aura, it deals 3 damage to any target."

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

    // When you sacrifice this Aura, it deals 3 damage to any target.
    triggeredAbility {
        trigger = Triggers.self.isSacrificed()
        val victim = target(Targets.Any)
        effect = Effects.DealDamage(3, victim)
        description = "When you sacrifice this Aura, it deals 3 damage to any target."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "23"
        artist = "Maciej Kuciara"
        imageUri = "https://cards.scryfall.io/normal/front/1/6/16d4e46d-7aae-4fe5-9b0b-9a39e02e4883.jpg?1783939855"
        ruling(
            "2013-09-15",
            "The check of whether the enchanted creature has three or more +1/+1 counters on it happens as part " +
                "of the resolution of the attack triggered ability. If the third +1/+1 counter is put on the " +
                "enchanted creature any other way, you won't sacrifice the Ordeal until the next time the creature attacks."
        )
        ruling("2013-09-15", "If you sacrifice the Ordeal in some other way, its last ability will trigger.")
    }
}

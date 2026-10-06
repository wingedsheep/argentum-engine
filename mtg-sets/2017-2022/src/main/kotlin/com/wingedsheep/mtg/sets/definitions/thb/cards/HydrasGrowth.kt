package com.wingedsheep.mtg.sets.definitions.thb.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Hydra's Growth
 * {2}{G}
 * Enchantment — Aura
 *
 * Enchant creature
 * When this Aura enters, put a +1/+1 counter on enchanted creature.
 * At the beginning of your upkeep, double the number of +1/+1 counters on enchanted creature.
 *
 * Doubling is "put as many +1/+1 counters on it as it already has" (ruling 2020-01-24).
 * [EffectTarget.EnchantedCreature] falls back to the last-enchanted host once the Aura has left,
 * so the ETB counter still lands per the first ruling.
 */
val HydrasGrowth = card("Hydra's Growth") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "When this Aura enters, put a +1/+1 counter on enchanted creature.\n" +
        "At the beginning of your upkeep, double the number of +1/+1 counters on enchanted creature."

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.EnchantedCreature)
    }

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        effect = Effects.AddDynamicCounters(
            counterType = CounterType.PLUS_ONE_PLUS_ONE,
            amount = DynamicAmounts.countersOn(EffectTarget.EnchantedCreature, CounterType.PLUS_ONE_PLUS_ONE),
            target = EffectTarget.EnchantedCreature
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "172"
        artist = "Bayard Wu"
        imageUri = "https://cards.scryfall.io/normal/front/5/1/5112ee2a-a6f3-4280-a915-000a97a9cdef.jpg?1783931539"
        ruling("2020-01-24", "If Hydra's Growth leaves the battlefield before its enters-the-battlefield triggered ability resolves, the creature it last enchanted before it left gets the +1/+1 counter.")
        ruling("2020-01-24", "To double the number of +1/+1 counters on a creature, put a number of +1/+1 counters on it equal to the number it already has. Other cards that interact with putting counters on it will interact with this effect accordingly.")
    }
}

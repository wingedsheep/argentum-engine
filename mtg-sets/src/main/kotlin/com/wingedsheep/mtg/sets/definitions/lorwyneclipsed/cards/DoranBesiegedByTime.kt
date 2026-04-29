package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameEvent.AttackEvent
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ReduceSpellCostByFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.TriggerSpec
import com.wingedsheep.sdk.scripting.effects.ModifyStatsEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.values.EntityNumericProperty
import com.wingedsheep.sdk.scripting.values.EntityReference

// Absolute difference between the triggering creature's power and toughness:
// max(toughness - power, power - toughness).
private val TriggeringPower: DynamicAmount =
    DynamicAmount.EntityProperty(EntityReference.Triggering, EntityNumericProperty.Power)
private val TriggeringToughness: DynamicAmount =
    DynamicAmount.EntityProperty(EntityReference.Triggering, EntityNumericProperty.Toughness)
private val TriggeringPowerToughnessDifference: DynamicAmount = DynamicAmount.Max(
    DynamicAmount.Subtract(TriggeringToughness, TriggeringPower),
    DynamicAmount.Subtract(TriggeringPower, TriggeringToughness)
)

/**
 * Doran, Besieged by Time
 * {1}{W}{B}{G}
 * Legendary Creature — Treefolk Druid
 * 0/5
 *
 * Each creature spell you cast with toughness greater than its power costs {1} less to cast.
 * Whenever a creature you control attacks or blocks, it gets +X/+X until end of turn,
 * where X is the difference between its power and toughness.
 */
val DoranBesiegedByTime = card("Doran, Besieged by Time") {
    manaCost = "{1}{W}{B}{G}"
    typeLine = "Legendary Creature — Treefolk Druid"
    power = 0
    toughness = 5
    oracleText = "Each creature spell you cast with toughness greater than its power costs {1} less to cast.\n" +
        "Whenever a creature you control attacks or blocks, it gets +X/+X until end of turn, where X is the difference between its power and toughness."

    // Each creature spell you cast with toughness greater than its power costs {1} less to cast.
    staticAbility {
        ability = ReduceSpellCostByFilter(
            filter = GameObjectFilter.Creature.toughnessGreaterThanPower(),
            amount = 1
        )
    }

    // Whenever a creature you control attacks, it gets +X/+X until end of turn,
    // where X is the difference between its power and toughness.
    triggeredAbility {
        trigger = TriggerSpec(
            AttackEvent(filter = GameObjectFilter.Creature.youControl()),
            TriggerBinding.ANY
        )
        effect = ModifyStatsEffect(
            powerModifier = TriggeringPowerToughnessDifference,
            toughnessModifier = TriggeringPowerToughnessDifference,
            target = EffectTarget.TriggeringEntity
        )
    }

    // Whenever a creature you control blocks, same.
    triggeredAbility {
        trigger = Triggers.CreatureYouControlBlocks
        effect = ModifyStatsEffect(
            powerModifier = TriggeringPowerToughnessDifference,
            toughnessModifier = TriggeringPowerToughnessDifference,
            target = EffectTarget.TriggeringEntity
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "215"
        artist = "Carl Critchlow"
        flavorText = "\"Each year that passes rots you with scars. Shelter your heart, for the world is cruel.\""
        imageUri = "https://cards.scryfall.io/normal/front/5/6/568aa70a-6765-486a-bd37-5d38b16c46de.jpg?1767732894"

        ruling("2025-11-17", "The value of X is calculated only once, as Doran's last ability resolves.")
        ruling("2025-11-17", "Doran's first ability applies only to generic mana in the total cost of creature spells you cast with toughness greater than their power.")
        ruling("2025-11-17", "To find the difference between a creature's power and its toughness, subtract the smaller of those two numbers from the larger one. For example, the difference between the power and toughness of a 3/5 creature is 2. The difference between the power and toughness of a 5/3 creature is also 2.")
    }
}

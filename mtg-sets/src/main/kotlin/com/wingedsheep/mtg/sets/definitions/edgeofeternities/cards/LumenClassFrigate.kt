package com.wingedsheep.mtg.sets.definitions.edgeofeternities.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantCardType
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStatsForCreatureGroup
import com.wingedsheep.sdk.scripting.StaticTarget
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.conditions.Compare
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.events.CounterTypeFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.values.EntityNumericProperty
import com.wingedsheep.sdk.scripting.values.EntityReference

/**
 * Lumen-Class Frigate
 * {1}{W}
 * Artifact — Spacecraft
 * Station (Tap another creature you control: Put charge counters equal to its power on this Spacecraft. Station only as a sorcery. It's an artifact creature at 12+.)
 * 2+ | Other creatures you control get +1/+1.
 * 12+ | Flying, lifelink
 * 3/5
 */
val LumenClassFrigate = card("Lumen-Class Frigate") {
    manaCost = "{1}{W}"
    typeLine = "Artifact — Spacecraft"
    power = 3
    toughness = 5
    oracleText = "Station (Tap another creature you control: Put charge counters equal to its power on this Spacecraft. Station only as a sorcery. It's an artifact creature at 12+.)\n2+ | Other creatures you control get +1/+1.\n12+ | Flying, lifelink"

    // Station activated ability: tap another creature → add charge counters equal to its power
    activatedAbility {
        cost = AbilityCost.TapPermanents(
            count = 1,
            filter = GameObjectFilter.Creature,
            excludeSelf = true
        )
        effect = Effects.AddDynamicCounters(
            counterType = Counters.CHARGE,
            amount = DynamicAmount.EntityProperty(
                entity = EntityReference.TappedAsCost(),
                numericProperty = EntityNumericProperty.Power
            ),
            target = EffectTarget.Self
        )
        timing = TimingRule.SorcerySpeed
    }

    // Conditional type change: artifact creature at 12+ charge counters
    staticAbility {
        condition = Compare(
            left = DynamicAmount.EntityProperty(
                entity = EntityReference.Source,
                numericProperty = EntityNumericProperty.CounterCount(CounterTypeFilter.Named(Counters.CHARGE))
            ),
            operator = ComparisonOperator.GTE,
            right = DynamicAmount.Fixed(12)
        )
        ability = GrantCardType("CREATURE", StaticTarget.SourceCreature)
    }

    // Conditional ability: +1/+1 to other creatures at 2+ charge counters
    staticAbility {
        condition = Compare(
            left = DynamicAmount.EntityProperty(
                entity = EntityReference.Source,
                numericProperty = EntityNumericProperty.CounterCount(CounterTypeFilter.Named(Counters.CHARGE))
            ),
            operator = ComparisonOperator.GTE,
            right = DynamicAmount.Fixed(2)
        )
        ability = ModifyStatsForCreatureGroup(
            powerBonus = 1,
            toughnessBonus = 1,
            filter = GroupFilter(GameObjectFilter.Creature.youControl(), excludeSelf = true)
        )
    }

    // Conditional keywords: flying and lifelink at 12+ charge counters
    staticAbility {
        condition = Compare(
            left = DynamicAmount.EntityProperty(
                entity = EntityReference.Source,
                numericProperty = EntityNumericProperty.CounterCount(CounterTypeFilter.Named(Counters.CHARGE))
            ),
            operator = ComparisonOperator.GTE,
            right = DynamicAmount.Fixed(12)
        )
        ability = GrantKeyword(Keyword.FLYING.name, StaticTarget.SourceCreature)
    }

    staticAbility {
        condition = Compare(
            left = DynamicAmount.EntityProperty(
                entity = EntityReference.Source,
                numericProperty = EntityNumericProperty.CounterCount(CounterTypeFilter.Named(Counters.CHARGE))
            ),
            operator = ComparisonOperator.GTE,
            right = DynamicAmount.Fixed(12)
        )
        ability = GrantKeyword(Keyword.LIFELINK.name, StaticTarget.SourceCreature)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "25"
        artist = "Zezhou Chen"
        imageUri = "https://cards.scryfall.io/normal/front/0/c/0cc59b5a-65fa-47cc-8ac7-b7c3f533a782.jpg?1755341197"
    }
}

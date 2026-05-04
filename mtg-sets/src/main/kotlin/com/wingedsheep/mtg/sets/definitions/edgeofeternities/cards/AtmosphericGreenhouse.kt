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
import com.wingedsheep.sdk.scripting.StaticTarget
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.conditions.Compare
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.effects.AddCountersEffect
import com.wingedsheep.sdk.scripting.effects.ForEachInGroupEffect
import com.wingedsheep.sdk.scripting.events.CounterTypeFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.values.EntityNumericProperty
import com.wingedsheep.sdk.scripting.values.EntityReference

/**
 * Atmospheric Greenhouse
 * {4}{G}
 * Artifact — Spacecraft
 * When this Spacecraft enters, put a +1/+1 counter on each creature you control.
 * Station (Tap another creature you control: Put charge counters equal to its power on this Spacecraft. Station only as a sorcery. It's an artifact creature at 8+.)
 * 8+ | Flying, trample
 * 5/4
 */
val AtmosphericGreenhouse = card("Atmospheric Greenhouse") {
    manaCost = "{4}{G}"
    typeLine = "Artifact — Spacecraft"
    power = 5
    toughness = 4
    oracleText = "When this Spacecraft enters, put a +1/+1 counter on each creature you control.\nStation (Tap another creature you control: Put charge counters equal to its power on this Spacecraft. Station only as a sorcery. It's an artifact creature at 8+.)\n8+ | Flying, trample"

    // ETB: Put a +1/+1 counter on each creature you control
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = ForEachInGroupEffect(
            filter = GroupFilter.AllCreaturesYouControl,
            effect = AddCountersEffect(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.ContextTarget(0))
        )
    }

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

    // Conditional type change: artifact creature at 8+ charge counters
    staticAbility {
        condition = Compare(
            left = DynamicAmount.EntityProperty(
                entity = EntityReference.Source,
                numericProperty = EntityNumericProperty.CounterCount(CounterTypeFilter.Named(Counters.CHARGE))
            ),
            operator = ComparisonOperator.GTE,
            right = DynamicAmount.Fixed(8)
        )
        ability = GrantCardType("CREATURE", StaticTarget.SourceCreature)
    }

    // Conditional keywords: flying and trample at 8+ charge counters
    staticAbility {
        condition = Compare(
            left = DynamicAmount.EntityProperty(
                entity = EntityReference.Source,
                numericProperty = EntityNumericProperty.CounterCount(CounterTypeFilter.Named(Counters.CHARGE))
            ),
            operator = ComparisonOperator.GTE,
            right = DynamicAmount.Fixed(8)
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
            right = DynamicAmount.Fixed(8)
        )
        ability = GrantKeyword(Keyword.TRAMPLE.name, StaticTarget.SourceCreature)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "171"
        artist = "Sergey Glushakov"
        imageUri = "https://cards.scryfall.io/normal/front/b/f/bf05e378-7a0c-49e3-8c6e-c0fd56796434.jpg?1755341410"
    }
}

package com.wingedsheep.mtg.sets.definitions.edgeofeternities.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.EffectPatterns
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
import com.wingedsheep.sdk.scripting.events.CounterTypeFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.values.EntityNumericProperty
import com.wingedsheep.sdk.scripting.values.EntityReference

/**
 * Sledge-Class Seedship
 * {2}{G}
 * Artifact — Spacecraft
 * Station (Tap another creature you control: Put charge counters equal to its power on this
 * Spacecraft. Station only as a sorcery. It's an artifact creature at 7+.)
 * 7+ | Flying
 * Whenever this Spacecraft attacks, you may put a creature card from your hand onto the battlefield.
 * 4/5
 */
val SledgeClassSeedship = card("Sledge-Class Seedship") {
    manaCost = "{2}{G}"
    typeLine = "Artifact — Spacecraft"
    power = 4
    toughness = 5
    oracleText = "Station (Tap another creature you control: Put charge counters equal to its power on this Spacecraft. Station only as a sorcery. It's an artifact creature at 7+.)\n7+ | Flying\nWhenever this Spacecraft attacks, you may put a creature card from your hand onto the battlefield."

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

    // Conditional type change: artifact creature at 7+ charge counters
    staticAbility {
        condition = Compare(
            left = DynamicAmount.EntityProperty(
                entity = EntityReference.Source,
                numericProperty = EntityNumericProperty.CounterCount(CounterTypeFilter.Named(Counters.CHARGE))
            ),
            operator = ComparisonOperator.GTE,
            right = DynamicAmount.Fixed(7)
        )
        ability = GrantCardType("CREATURE", StaticTarget.SourceCreature)
    }

    // Conditional keyword: flying at 7+ charge counters
    staticAbility {
        condition = Compare(
            left = DynamicAmount.EntityProperty(
                entity = EntityReference.Source,
                numericProperty = EntityNumericProperty.CounterCount(CounterTypeFilter.Named(Counters.CHARGE))
            ),
            operator = ComparisonOperator.GTE,
            right = DynamicAmount.Fixed(7)
        )
        ability = GrantKeyword(Keyword.FLYING.name, StaticTarget.SourceCreature)
    }

    // Attack trigger: you may put a creature card from hand onto the battlefield
    triggeredAbility {
        trigger = Triggers.Attacks
        effect = EffectPatterns.putFromHand(filter = GameObjectFilter.Creature)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "208"
        artist = "Leon Tukker"
        imageUri = "https://cards.scryfall.io/normal/front/d/c/dc0cb4b6-cc20-49ea-84b2-2f3af3b0a19e.jpg?1755341277"
    }
}

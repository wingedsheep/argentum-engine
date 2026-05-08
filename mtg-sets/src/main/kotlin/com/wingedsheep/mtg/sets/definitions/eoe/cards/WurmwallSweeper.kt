package com.wingedsheep.mtg.sets.definitions.eoe.cards

import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantCardType
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.conditions.Compare
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.events.CounterTypeFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.values.EntityNumericProperty
import com.wingedsheep.sdk.scripting.values.EntityReference

/**
 * Wurmwall Sweeper
 * {2}
 * Artifact — Spacecraft
 * When this Spacecraft enters, surveil 2.
 * Station (Tap another creature you control: Put charge counters equal to its power on this Spacecraft. Station only as a sorcery. It's an artifact creature at 4+.)
 * 4+ | Flying
 * 2/2
 */
val WurmwallSweeper = card("Wurmwall Sweeper") {
    manaCost = "{2}"
    typeLine = "Artifact — Spacecraft"
    power = 2
    toughness = 2
    oracleText = "When this Spacecraft enters, surveil 2.\nStation (Tap another creature you control: Put charge counters equal to its power on this Spacecraft. Station only as a sorcery. It's an artifact creature at 4+.)\n4+ | Flying"

    // ETB: surveil 2
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = EffectPatterns.surveil(2)
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

    // 4+ charge counters: becomes artifact creature and gains flying
    val charge4 = Compare(
        left = DynamicAmount.EntityProperty(
            entity = EntityReference.Source,
            numericProperty = EntityNumericProperty.CounterCount(CounterTypeFilter.Named(Counters.CHARGE))
        ),
        operator = ComparisonOperator.GTE,
        right = DynamicAmount.Fixed(4)
    )

    staticAbility {
        condition = charge4
        ability = GrantCardType("CREATURE", GroupFilter.source())
    }

    staticAbility {
        condition = charge4
        ability = GrantKeyword(Keyword.FLYING, GroupFilter.source())
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "249"
        artist = "Hardy Fowler"
        imageUri = "https://cards.scryfall.io/normal/front/9/a/9ace282a-5901-4d36-ad21-17eb88bc5138.jpg?1755341422"
    }
}

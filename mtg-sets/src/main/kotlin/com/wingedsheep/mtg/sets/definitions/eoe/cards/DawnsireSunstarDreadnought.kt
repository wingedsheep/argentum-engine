package com.wingedsheep.mtg.sets.definitions.eoe.cards

import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.scripting.targets.TargetCreatureOrPlaneswalker
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
 * Dawnsire, Sunstar Dreadnought
 * {5}
 * Legendary Artifact — Spacecraft
 * Station (Tap another creature you control: Put charge counters equal to its power on this Spacecraft. Station only as a sorcery. It's an artifact creature at 20+.)
 * 10+ | Whenever you attack, Dawnsire deals 100 damage to up to one target creature or planeswalker.
 * 20+ | Flying
 * 20/20
 */
val DawnsireSunstarDreadnought = card("Dawnsire, Sunstar Dreadnought") {
    manaCost = "{5}"
    typeLine = "Legendary Artifact — Spacecraft"
    power = 20
    toughness = 20
    oracleText = "Station (Tap another creature you control: Put charge counters equal to its power on this Spacecraft. Station only as a sorcery. It's an artifact creature at 20+.)\n10+ | Whenever you attack, Dawnsire deals 100 damage to up to one target creature or planeswalker.\n20+ | Flying"

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

    // 10+ charge counters: Whenever you attack, deal 100 damage to target
    triggeredAbility {
        trigger = Triggers.YouAttack
        val target = target("up to one target creature or planeswalker", TargetCreatureOrPlaneswalker(optional = true))
        effect = com.wingedsheep.sdk.scripting.effects.ConditionalEffect(
            condition = Compare(
                left = DynamicAmount.EntityProperty(
                    entity = EntityReference.Source,
                    numericProperty = EntityNumericProperty.CounterCount(CounterTypeFilter.Named(Counters.CHARGE))
                ),
                operator = ComparisonOperator.GTE,
                right = DynamicAmount.Fixed(10)
            ),
            effect = Effects.DealDamage(100, target)
        )
        description = "Whenever you attack, Dawnsire deals 100 damage to up to one target creature or planeswalker."
    }

    // 20+ charge counters
    val charge20 = Compare(
        left = DynamicAmount.EntityProperty(
            entity = EntityReference.Source,
            numericProperty = EntityNumericProperty.CounterCount(CounterTypeFilter.Named(Counters.CHARGE))
        ),
        operator = ComparisonOperator.GTE,
        right = DynamicAmount.Fixed(20)
    )

    staticAbility {
        condition = charge20
        ability = GrantCardType("CREATURE", GroupFilter.source())
    }

    staticAbility {
        condition = charge20
        ability = GrantKeyword(Keyword.FLYING.name, GroupFilter.source())
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "238"
        artist = "Jaime Jones"
        imageUri = "https://cards.scryfall.io/normal/front/6/1/6133355c-3dcf-466a-b771-fe6c44d4fa4d.jpg?1755341284"
    }
}

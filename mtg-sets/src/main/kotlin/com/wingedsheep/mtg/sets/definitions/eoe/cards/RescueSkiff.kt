package com.wingedsheep.mtg.sets.definitions.eoe.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantCardType
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.conditions.Compare
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.events.CounterTypeFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.values.EntityNumericProperty
import com.wingedsheep.sdk.scripting.values.EntityReference

/**
 * Rescue Skiff
 * {5}{W}
 * Artifact — Spacecraft
 * 5/6
 *
 * When this Spacecraft enters, return target creature or enchantment card from your graveyard
 * to the battlefield.
 * Station (Tap another creature you control: Put charge counters equal to its power on this
 * Spacecraft. Station only as a sorcery. It's an artifact creature at 10+.)
 * 10+ | Flying
 */
val RescueSkiff = card("Rescue Skiff") {
    manaCost = "{5}{W}"
    colorIdentity = "W"
    typeLine = "Artifact — Spacecraft"
    power = 5
    toughness = 6
    oracleText = "When this Spacecraft enters, return target creature or enchantment card from your " +
        "graveyard to the battlefield.\n" +
        "Station (Tap another creature you control: Put charge counters equal to its power on this " +
        "Spacecraft. Station only as a sorcery. It's an artifact creature at 10+.)\n10+ | Flying"

    // When this Spacecraft enters, return target creature or enchantment card from your graveyard
    // to the battlefield.
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val t = target(
            "target creature or enchantment card from your graveyard",
            TargetObject(filter = TargetFilter(GameObjectFilter.CreatureOrEnchantment.ownedByYou(), zone = Zone.GRAVEYARD))
        )
        effect = Effects.Move(target = t, destination = Zone.BATTLEFIELD)
        description = "return target creature or enchantment card from your graveyard to the battlefield"
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

    // Conditional type change: artifact creature at 10+ charge counters
    staticAbility {
        condition = Compare(
            left = DynamicAmount.EntityProperty(
                entity = EntityReference.Source,
                numericProperty = EntityNumericProperty.CounterCount(CounterTypeFilter.Named(Counters.CHARGE))
            ),
            operator = ComparisonOperator.GTE,
            right = DynamicAmount.Fixed(10)
        )
        ability = GrantCardType("CREATURE", GroupFilter.source())
    }

    // Conditional keyword: flying at 10+ charge counters
    staticAbility {
        condition = Compare(
            left = DynamicAmount.EntityProperty(
                entity = EntityReference.Source,
                numericProperty = EntityNumericProperty.CounterCount(CounterTypeFilter.Named(Counters.CHARGE))
            ),
            operator = ComparisonOperator.GTE,
            right = DynamicAmount.Fixed(10)
        )
        ability = GrantKeyword(Keyword.FLYING.name, GroupFilter.source())
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "32"
        artist = "Viko Menezes"
        imageUri = "https://cards.scryfall.io/normal/front/6/f/6fe86bfb-c67d-4df9-88c9-f083091f4cda.jpg?1755341383"
    }
}

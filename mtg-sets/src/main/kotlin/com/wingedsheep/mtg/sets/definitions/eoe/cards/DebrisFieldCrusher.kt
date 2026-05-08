package com.wingedsheep.mtg.sets.definitions.eoe.cards

import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
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
 * Debris Field Crusher
 * {4}{R}
 * Artifact — Spacecraft
 * When this Spacecraft enters, it deals 3 damage to any target.
 * Station (Tap another creature you control: Put charge counters equal to its power on this Spacecraft. Station only as a sorcery. It's an artifact creature at 8+.)
 * 8+ | Flying
 * {1}{R}: This Spacecraft gets +2/+0 until end of turn.
 * 1/5
 */
val DebrisFieldCrusher = card("Debris Field Crusher") {
    manaCost = "{4}{R}"
    typeLine = "Artifact — Spacecraft"
    power = 1
    toughness = 5
    oracleText = "When this Spacecraft enters, it deals 3 damage to any target.\nStation (Tap another creature you control: Put charge counters equal to its power on this Spacecraft. Station only as a sorcery. It's an artifact creature at 8+.)\n8+ | Flying\n{1}{R}: This Spacecraft gets +2/+0 until end of turn."

    // ETB: deals 3 damage to any target
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val target = target("any target", Targets.Any)
        effect = Effects.DealDamage(3, target)
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

    // Station threshold: 8+ charge counters on this Spacecraft
    val charge8 = Compare(
        left = DynamicAmount.EntityProperty(
            entity = EntityReference.Source,
            numericProperty = EntityNumericProperty.CounterCount(CounterTypeFilter.Named(Counters.CHARGE))
        ),
        operator = ComparisonOperator.GTE,
        right = DynamicAmount.Fixed(8)
    )

    // Conditional type change: artifact creature at 8+ charge counters
    staticAbility {
        condition = charge8
        ability = GrantCardType("CREATURE", GroupFilter.source())
    }

    // 8+ charge counters: Flying
    staticAbility {
        condition = charge8
        ability = GrantKeyword(Keyword.FLYING.name, GroupFilter.source())
    }

    // Activated ability: {1}{R}: +2/+0 until end of turn
    activatedAbility {
        cost = Costs.Mana("{1}{R}")
        effect = Effects.ModifyStats(2, 0, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "131"
        artist = "David Álvarez"
        imageUri = "https://cards.scryfall.io/normal/front/e/a/ea713f35-6442-4388-8839-2714374fb4b6.jpg?1755341403"
    }
}

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
 * Uthros Scanship
 * {3}{U}
 * Artifact — Spacecraft
 * When this Spacecraft enters, draw two cards, then discard a card.
 * Station (Tap another creature you control: Put charge counters equal to its power on this Spacecraft. Station only as a sorcery. It's an artifact creature at 8+.)
 * 8+ | Flying
 * 4/4
 */
val UthrosScanship = card("Uthros Scanship") {
    manaCost = "{3}{U}"
    typeLine = "Artifact — Spacecraft"
    power = 4
    toughness = 4
    oracleText = "When this Spacecraft enters, draw two cards, then discard a card.\nStation (Tap another creature you control: Put charge counters equal to its power on this Spacecraft. Station only as a sorcery. It's an artifact creature at 8+.)\n8+ | Flying"

    // ETB: draw two cards, then discard a card
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = EffectPatterns.loot(draw = 2, discard = 1)
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

    // 8+ charge counters: becomes artifact creature and gains flying
    val charge8 = Compare(
        left = DynamicAmount.EntityProperty(
            entity = EntityReference.Source,
            numericProperty = EntityNumericProperty.CounterCount(CounterTypeFilter.Named(Counters.CHARGE))
        ),
        operator = ComparisonOperator.GTE,
        right = DynamicAmount.Fixed(8)
    )

    staticAbility {
        condition = charge8
        ability = GrantCardType("CREATURE", GroupFilter.source())
    }

    staticAbility {
        condition = charge8
        ability = GrantKeyword(Keyword.FLYING, GroupFilter.source())
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "85"
        artist = "Sergey Glushakov"
        imageUri = "https://cards.scryfall.io/normal/front/1/f/1f93887f-35c5-472f-83d0-54227b3bd1d2.jpg?1755341393"
    }
}

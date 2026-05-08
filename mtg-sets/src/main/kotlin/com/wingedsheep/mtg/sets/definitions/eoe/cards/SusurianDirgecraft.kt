package com.wingedsheep.mtg.sets.definitions.eoe.cards

import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.conditions.Compare
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.events.CounterTypeFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.EntityNumericProperty
import com.wingedsheep.sdk.scripting.values.EntityReference
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantCardType
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Susurian Dirgecraft
 * {4}{B}
 * Artifact — Spacecraft
 * When this Spacecraft enters, each opponent sacrifices a nontoken creature of their choice.
 * Station (Tap another creature you control: Put charge counters equal to its power on this Spacecraft. Station only as a sorcery. It's an artifact creature at 7+.)
 * 7+ | Flying
 * 4/3
 */
val SusurianDirgecraft = card("Susurian Dirgecraft") {
    manaCost = "{4}{B}"
    typeLine = "Artifact — Spacecraft"
    power = 4
    toughness = 3
    oracleText = "When this Spacecraft enters, each opponent sacrifices a nontoken creature of their choice.\nStation (Tap another creature you control: Put charge counters equal to its power on this Spacecraft. Station only as a sorcery. It's an artifact creature at 7+.)\n7+ | Flying"

    // ETB: each opponent sacrifices a nontoken creature of their choice
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.Sacrifice(
            GameObjectFilter.Creature.nontoken(),
            target = EffectTarget.PlayerRef(Player.EachOpponent)
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

    // 7+ charge counters: becomes artifact creature and gains flying
    val charge7 = Compare(
        left = DynamicAmount.EntityProperty(
            entity = EntityReference.Source,
            numericProperty = EntityNumericProperty.CounterCount(CounterTypeFilter.Named(Counters.CHARGE))
        ),
        operator = ComparisonOperator.GTE,
        right = DynamicAmount.Fixed(7)
    )

    staticAbility {
        condition = charge7
        ability = GrantCardType("CREATURE", GroupFilter.source())
    }

    staticAbility {
        condition = charge7
        ability = GrantKeyword(Keyword.FLYING, GroupFilter.source())
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "117"
        artist = "Mark Poole"
        imageUri = "https://cards.scryfall.io/normal/front/b/6/b67cdb6e-9a3b-4887-924d-318faa3c443d.jpg?1755341400"
    }
}

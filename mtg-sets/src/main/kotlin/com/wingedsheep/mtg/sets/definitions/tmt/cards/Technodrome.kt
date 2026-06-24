package com.wingedsheep.mtg.sets.definitions.tmt.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantAttackUnless
import com.wingedsheep.sdk.scripting.CantBlockUnless
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.Compare
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.values.EntityNumericProperty
import com.wingedsheep.sdk.scripting.values.EntityReference

/**
 * Technodrome
 * {2}
 * Artifact Creature — Construct
 * 3/3
 *
 * Reach, trample
 * This creature can't attack or block unless its power is 6 or greater.
 * {T}, Sacrifice another artifact: Draw a card. Put a +1/+1 counter on this creature.
 */
val Technodrome = card("Technodrome") {
    manaCost = "{2}"
    typeLine = "Artifact Creature — Construct"
    oracleText = "Reach, trample\nThis creature can't attack or block unless its power is 6 or greater.\n{T}, Sacrifice another artifact: Draw a card. Put a +1/+1 counter on this creature."
    power = 3
    toughness = 3

    keywords(Keyword.REACH, Keyword.TRAMPLE)

    // "can't attack or block unless its power is 6 or greater" — compares the source's
    // own (projected) power to 6.
    val powerAtLeastSix = Compare(
        DynamicAmount.EntityProperty(EntityReference.Source, EntityNumericProperty.Power),
        ComparisonOperator.GTE,
        DynamicAmount.Fixed(6)
    )
    staticAbility {
        ability = CantAttackUnless(condition = powerAtLeastSix)
    }
    staticAbility {
        ability = CantBlockUnless(condition = powerAtLeastSix)
    }

    activatedAbility {
        cost = Costs.Composite(
            Costs.Tap,
            Costs.SacrificeAnother(GameObjectFilter.Artifact)
        )
        effect = Effects.DrawCards(1)
            .then(Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self))
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "179"
        artist = "Greg Staples"
        flavorText = "Even unfinished, the Technodrome was magnificent, a wonder of alien technology."
        imageUri = "https://cards.scryfall.io/normal/front/2/8/287f5ab0-b15b-4507-8a24-585f9a9841ad.jpg?1769006421"
    }
}

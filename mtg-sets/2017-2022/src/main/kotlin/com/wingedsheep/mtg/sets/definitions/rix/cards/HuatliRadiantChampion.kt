package com.wingedsheep.mtg.sets.definitions.rix.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedTriggeredAbility
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Huatli, Radiant Champion
 * {2}{G}{W}
 * Legendary Planeswalker — Huatli
 * Starting Loyalty: 3
 *
 * +1: Put a loyalty counter on Huatli for each creature you control.
 * −1: Target creature gets +X/+X until end of turn, where X is the number of creatures you control.
 * −8: You get an emblem with "Whenever a creature you control enters, you may draw a card."
 *
 * Both counts are taken once, on resolution (ruling): the −1's bonus is locked in for the turn.
 * The emblem is a global triggered ability (Koth, Fire of Resistance idiom), so it outlives Huatli.
 */
val HuatliRadiantChampion = card("Huatli, Radiant Champion") {
    manaCost = "{2}{G}{W}"
    colorIdentity = "GW"
    typeLine = "Legendary Planeswalker — Huatli"
    startingLoyalty = 3
    oracleText = "+1: Put a loyalty counter on Huatli for each creature you control.\n" +
        "−1: Target creature gets +X/+X until end of turn, where X is the number of creatures you control.\n" +
        "−8: You get an emblem with \"Whenever a creature you control enters, you may draw a card.\""

    loyaltyAbility(+1) {
        effect = Effects.AddDynamicCounters(
            CounterType.LOYALTY,
            DynamicAmounts.creaturesYouControl(),
            EffectTarget.Self
        )
        description = "Put a loyalty counter on Huatli for each creature you control."
    }

    loyaltyAbility(-1) {
        val creature = target(TargetFilter.Creature)
        effect = Effects.ModifyStats(
            DynamicAmounts.creaturesYouControl(),
            DynamicAmounts.creaturesYouControl(),
            creature
        )
        description = "Target creature gets +X/+X until end of turn, where X is the number of " +
            "creatures you control."
    }

    loyaltyAbility(-8) {
        effect = Effects.CreateGlobalTriggeredAbility(
            ability = grantedTriggeredAbility {
                trigger = Triggers.a(GameObjectFilter.Creature.youControl()).enters()
                effect = Effects.May(Effects.DrawCards(1))
                description = "Whenever a creature you control enters, you may draw a card."
            },
            emblem = true,
            descriptionOverride = "Whenever a creature you control enters, you may draw a card."
        )
        description = "You get an emblem with \"Whenever a creature you control enters, you may " +
            "draw a card.\""
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "159"
        artist = "Chris Rahn"
        imageUri = "https://cards.scryfall.io/normal/front/3/9/39bcc48a-efdb-443f-93b8-bc0b54aa0eb8.jpg?1783935274"
        ruling(
            "2018-01-19",
            "If you activate Huatli's first ability while you control no creatures, she'll get one " +
                "loyalty counter from the ability's activation cost and no others as it resolves.",
        )
        ruling(
            "2018-01-19",
            "The number of creatures you control is counted only as Huatli's first or second ability " +
                "resolves. Once her second ability resolves, the bonus won't change, even if the number of " +
                "creatures you control changes later in the turn.",
        )
    }
}

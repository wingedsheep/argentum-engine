package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Jett Reno, Problem Solver
 * {1}{U}
 * Legendary Creature — Human Artificer
 * 1/2
 *
 * Whenever Jett Reno becomes tapped, surveil 1. If her power is 4 or greater, instead draw a card.
 * Whenever you face a dilemma, put a +1/+1 counter on Jett Reno. (You face a dilemma as you choose
 * one or more modes for a spell or ability.)
 *
 * Her power is checked as the tap trigger resolves.
 */
val JettRenoProblemSolver = card("Jett Reno, Problem Solver") {
    manaCost = "{1}{U}"
    typeLine = "Legendary Creature — Human Artificer"
    colorIdentity = "U"
    oracleText = "Whenever Jett Reno becomes tapped, surveil 1. If her power is 4 or greater, instead draw a card.\n" +
        "Whenever you face a dilemma, put a +1/+1 counter on Jett Reno. " +
        "(You face a dilemma as you choose one or more modes for a spell or ability.)"
    power = 1
    toughness = 2

    triggeredAbility {
        trigger = Triggers.self.becomesTapped()
        effect = Effects.If(
            condition = Conditions.CompareAmounts(DynamicAmounts.sourcePower(), ComparisonOperator.GTE, 4),
            then = Effects.DrawCards(1),
            otherwise = Effects.Surveil(1)
        )
    }

    triggeredAbility {
        trigger = Triggers.you.facesADilemma()
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "67"
        artist = "Riccardo Federici"
        flavorText = "\"Awesome. I know. I built them.\""
        imageUri = "https://cards.scryfall.io/normal/front/6/d/6dc34d44-397a-4dda-a8e3-0c2ff7da110f.jpg?1793511776"
    }
}

package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.CardNumericProperty

/**
 * Ritual of Hope (Innistrad: Midnight Hunt #31)
 * {1}{W}
 * Instant
 * Creatures you control get +1/+1 until end of turn.
 * Coven — If you control three or more creatures with different powers, creatures you control get
 * +2/+1 until end of turn instead.
 *
 * Coven is checked once on resolution (distinct power values among creatures you control ≥ 3); the
 * "instead" is an [Effects.If] choosing between the +2/+1 and +1/+1 board-wide pump.
 */
val RitualOfHope = card("Ritual of Hope") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Creatures you control get +1/+1 until end of turn.\n" +
        "Coven — If you control three or more creatures with different powers, creatures you control " +
        "get +2/+1 until end of turn instead."

    spell {
        effect = Effects.If(
            condition = Conditions.CompareAmounts(
                left = DynamicAmounts.battlefield(
                    Player.You,
                    GameObjectFilter.Creature,
                ).distinctValues(CardNumericProperty.POWER),
                operator = ComparisonOperator.GTE,
                right = 3,
            ),
            then = Effects.ForEachInGroup(
                Filters.Group.creaturesYouControl,
                Effects.ModifyStats(2, 1, EffectTarget.IterationEntity),
            ),
            otherwise = Effects.ForEachInGroup(
                Filters.Group.creaturesYouControl,
                Effects.ModifyStats(1, 1, EffectTarget.IterationEntity),
            ),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "31"
        artist = "Craig J Spearing"
        flavorText = "\"Tonight, we can be anything.\""
        imageUri = "https://cards.scryfall.io/normal/front/d/8/d8293903-4604-4516-866e-27a306b6fd7c.jpg?1783925653"
        ruling("2021-09-24", "For three creatures to have different powers from one another, each of their powers needs to be different. A 1/1 creature, a 2/1 creature, and another 2/1 creature aren't three creatures with different powers, even though both 2/1 creatures have different power than the 1/1 creature.")
    }
}

package com.wingedsheep.mtg.sets.definitions.bfz.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Exert Influence
 * {4}{U}
 * Sorcery
 * Converge — Gain control of target creature if its power is less than or equal to the number of
 * colors of mana spent to cast this spell.
 *
 * Any creature is a legal target; the power check is made as the spell resolves (the Welcome to
 * the Fold shape), against [DynamicAmounts.colorsOfManaSpent] read off the resolving spell. A copy
 * spent no mana, so it compares against 0. Once taken, the creature stays taken however its power
 * changes later.
 */
val ExertInfluence = card("Exert Influence") {
    manaCost = "{4}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "Converge — Gain control of target creature if its power is less than or equal to " +
        "the number of colors of mana spent to cast this spell."

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.If(
            Conditions.CompareAmounts(
                DynamicAmounts.targetPower(),
                ComparisonOperator.LTE,
                DynamicAmounts.colorsOfManaSpent(),
            ),
            then = Effects.GainControl(creature),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "77"
        artist = "Magali Villeneuve"
        flavorText = "\"I can't control its mind—I'm not even sure it has one. But I can dominate its will.\""
        imageUri = "https://cards.scryfall.io/normal/front/c/3/c35e9d13-ee4b-42f1-a154-18ab32947ad3.jpg?1783938208"
        ruling(
            "2015-08-25",
            "Check the creature's power as Exert Influence resolves to see if it's less than or equal to " +
                "the number of colors spent to cast Exert Influence. If it's greater, Exert Influence will " +
                "finish resolving with no effect.",
        )
        ruling(
            "2015-08-25",
            "Once you've gained control of a creature with Exert Influence, it doesn't matter what happens " +
                "to its power. Raising it won't cause the control-changing effect to end.",
        )
    }
}

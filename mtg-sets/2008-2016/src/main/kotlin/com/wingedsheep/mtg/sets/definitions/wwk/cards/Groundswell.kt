package com.wingedsheep.mtg.sets.definitions.wwk.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Groundswell
 * {G}
 * Instant
 * Target creature gets +2/+2 until end of turn.
 * Landfall — If you had a land enter the battlefield under your control this turn, that creature
 * gets +4/+4 until end of turn instead.
 *
 * The landfall check is a past-event read of the `LANDS_ENTERED_UNDER_CONTROL` turn tracker,
 * evaluated as the spell resolves — the land needn't still be on the battlefield, still yours, or
 * still a land. The +4/+4 replaces the +2/+2, so it's an if/otherwise, not a stack of both.
 */
val Groundswell = card("Groundswell") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Target creature gets +2/+2 until end of turn.\n" +
        "Landfall — If you had a land enter the battlefield under your control this turn, " +
        "that creature gets +4/+4 until end of turn instead."

    spell {
        val t = target(TargetFilter.Creature)
        effect = Effects.If(
            condition = Conditions.CompareAmounts(
                DynamicAmounts.landsEnteredUnderControlThisTurn(Player.You),
                ComparisonOperator.GTE,
                1,
            ),
            then = Effects.ModifyStats(4, 4, t),
            otherwise = Effects.ModifyStats(2, 2, t),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "104"
        artist = "Chris Rahn"
        flavorText = "\"This world will not be tamed.\""
        imageUri = "https://cards.scryfall.io/normal/front/4/c/4ca76e9a-672f-49eb-86c6-00fac60d3065.jpg?1783942045"
        ruling("2024-11-08", "Whether you had a land enter the battlefield under your control this turn is checked as this spell resolves, not as you cast it.")
        ruling("2024-11-08", "The landfall ability checks for an action that has happened in the past. It doesn't matter if a land that entered the battlefield under your control previously in the turn is still on the battlefield, is still under your control, or is still a land.")
        ruling("2024-11-08", "The effect of this spell's landfall ability replaces its normal effect. If you had a land enter under your control this turn, only the landfall-based effect happens.")
    }
}

package com.wingedsheep.mtg.sets.definitions.soi.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.madness
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Welcome to the Fold (Shadows over Innistrad #96)
 * {2}{U}{U}
 * Sorcery
 *
 * Gain control of target creature if its toughness is 2 or less. If this spell's madness cost
 * was paid, instead gain control of that creature if its toughness is X or less.
 * Madness {X}{U}{U}
 *
 * Any creature is a legal target; the toughness check happens on resolution, so a creature that
 * grows out of range in response stays put while the spell still resolves. The X lives only in
 * the madness cost and is read as [DynamicAmounts.xValue] when [Conditions.MadnessCostWasPaid]
 * (CR 702.35) holds.
 */
val WelcomeToTheFold = card("Welcome to the Fold") {
    manaCost = "{2}{U}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "Gain control of target creature if its toughness is 2 or less. If this spell's " +
        "madness cost was paid, instead gain control of that creature if its toughness is X or less.\n" +
        "Madness {X}{U}{U} (If you discard this card, discard it into exile. When you do, cast it " +
        "for its madness cost or put it into your graveyard.)"

    spell {
        val creature = target(TargetFilter.Creature)
        fun stealIfToughnessAtMost(limit: DynamicAmount) = Effects.If(
            Conditions.CompareAmounts(DynamicAmounts.targetToughness(), ComparisonOperator.LTE, limit),
            then = Effects.GainControl(creature),
        )
        effect = Effects.If(
            Conditions.MadnessCostWasPaid,
            then = stealIfToughnessAtMost(DynamicAmounts.xValue()),
            otherwise = stealIfToughnessAtMost(DynamicAmounts.fixed(2)),
        )
    }

    madness("{X}{U}{U}")

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "96"
        artist = "David Palumbo"
        imageUri = "https://cards.scryfall.io/normal/front/2/d/2d07fbe1-e66a-4893-86ee-7752ad77b120.jpg?1783937783"
    }
}

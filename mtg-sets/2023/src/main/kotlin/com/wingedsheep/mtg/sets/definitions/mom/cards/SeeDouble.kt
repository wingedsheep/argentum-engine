package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * See Double
 * {2}{U}{U}
 * Instant
 *
 * This spell can't be copied.
 * Choose one. If an opponent has eight or more cards in their graveyard, you may choose both instead.
 * • Copy target spell. You may choose new targets for the copy.
 * • Create a token that's a copy of target creature.
 *
 * "Can't be copied" is the card-level `cantBeCopied` flag (CR 707.10). The conditional modal count
 * is a cast-time `dynamicChooseCount` (the Molten Collapse shape): "an opponent has eight or more"
 * is per-opponent, so the gate is the greatest graveyard size among opponents, not their sum.
 */
val SeeDouble = card("See Double") {
    manaCost = "{2}{U}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "This spell can't be copied.\n" +
        "Choose one. If an opponent has eight or more cards in their graveyard, you may choose both instead.\n" +
        "• Copy target spell. You may choose new targets for the copy. (A copy of a permanent spell becomes a token.)\n" +
        "• Create a token that's a copy of target creature."

    cantBeCopied = true

    spell {
        modal(
            chooseCount = 2,
            minChooseCount = 1,
            dynamicChooseCount = DynamicAmounts.conditional(
                condition = Conditions.CompareAmounts(
                    DynamicAmounts.greatestAmongPlayers(
                        DynamicAmounts.count(Player.You, Zone.GRAVEYARD),
                        Player.EachOpponent
                    ),
                    ComparisonOperator.GTE,
                    8
                ),
                ifTrue = 2,
                ifFalse = 1
            )
        ) {
            mode("Copy target spell. You may choose new targets for the copy") {
                val spell = target(TargetFilter.SpellOnStack)
                effect = Effects.CopyTargetSpell(target = spell)
            }
            mode("Create a token that's a copy of target creature") {
                val creature = target(TargetFilter.Creature)
                effect = Effects.CreateTokenCopyOfTarget(creature)
            }
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "77"
        artist = "Marc Simonetti"
        imageUri = "https://cards.scryfall.io/normal/front/4/1/41634103-7b58-4feb-84b7-90a07a7b474f.jpg?1783917024"
        ruling("2023-04-14", "You may choose both modes if any opponent has eight or more cards in their graveyard, not necessarily all of them.")
        ruling("2023-04-14", "Once you've cast See Double and legally chosen both modes, it doesn't matter what happens to the number of cards in opponents' graveyards.")
    }
}

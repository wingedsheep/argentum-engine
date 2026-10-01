package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.dsl.CollectionSlot
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator

/**
 * White Sun's Twilight
 * {X}{W}{W}
 * Sorcery
 * You gain X life. Create X 1/1 colorless Phyrexian Mite artifact creature tokens with toxic 1 and
 * "This token can't block." If X is 5 or more, destroy all other creatures.
 *
 * "Other creatures" are the creatures other than the Mites this spell just created: the token
 * creation publishes [CollectionSlot.CreatedTokens], and the wipe gathers every creature and
 * subtracts that collection before destroying.
 */
val WhiteSunsTwilight = card("White Sun's Twilight") {
    manaCost = "{X}{W}{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"
    oracleText = "You gain X life. Create X 1/1 colorless Phyrexian Mite artifact creature tokens with toxic 1 and " +
        "\"This token can't block.\" If X is 5 or more, destroy all other creatures. " +
        "(Players dealt combat damage by a creature with toxic 1 also get a poison counter.)"

    spell {
        effect = Effects.GainLife(DynamicAmounts.xValue()) then
            Effects.CreatePhyrexianMite(DynamicAmounts.xValue()) then
            Effects.If(
                condition = Conditions.CompareAmounts(
                    DynamicAmounts.xValue(),
                    ComparisonOperator.GTE,
                    5
                ),
                then = Effects.Pipeline {
                    val creatures = gather(GameObjectFilter.Creature)
                    val others = exclude(creatures, CollectionSlot.CreatedTokens)
                    destroy(others)
                }
            )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "38"
        artist = "Julian Kok Joon Wen"
        imageUri = "https://cards.scryfall.io/normal/front/f/f/fff13d77-8133-4328-b91e-efce229bc331.jpg?1783918071"
    }
}

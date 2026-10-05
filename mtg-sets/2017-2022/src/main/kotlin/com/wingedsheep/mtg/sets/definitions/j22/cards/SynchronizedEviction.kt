package com.wingedsheep.mtg.sets.definitions.j22.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostGating
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Synchronized Eviction
 * {4}{U}
 * Instant
 * This spell costs {2} less to cast if you control at least two creatures that share a creature type.
 * Put target nonland permanent into its owner's library second from the top.
 *
 * "At least two creatures that share a creature type" is the largest creature-type tribe among the
 * creatures you control ([DynamicAmounts.largestSharedCreatureTypeCount], projected, so changelings
 * count toward every tribe) being at least two. The discount is a [ModifySpellCost] on the spell
 * itself, gated on that comparison, so it comes off only the generic part of the cost.
 */
val SynchronizedEviction = card("Synchronized Eviction") {
    manaCost = "{4}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "This spell costs {2} less to cast if you control at least two creatures that share a creature type.\n" +
        "Put target nonland permanent into its owner's library second from the top."

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.SelfCast,
            modification = CostModification.ReduceGeneric(2),
            gating = CostGating.OnlyIf(
                Conditions.CompareAmounts(
                    DynamicAmounts.largestSharedCreatureTypeCount(),
                    ComparisonOperator.GTE,
                    2,
                )
            )
        )
    }

    spell {
        val permanent = target(TargetFilter.NonlandPermanent)
        // 0-indexed: position 1 = second from the top.
        effect = Effects.PutIntoLibraryNthFromTop(permanent, 1)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "18"
        artist = "Nicholas Gregory"
        flavorText = "\"I told you this one would be a handful.\""
        imageUri = "https://cards.scryfall.io/normal/front/6/8/68f10249-8cd0-4366-bd9a-755f8e1c3592.jpg?1783919190"
    }
}

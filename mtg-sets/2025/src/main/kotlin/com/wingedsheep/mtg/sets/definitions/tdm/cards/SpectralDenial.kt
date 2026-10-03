package com.wingedsheep.mtg.sets.definitions.tdm.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.CostReductionSource
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Spectral Denial
 * {X}{U}
 * Instant
 * This spell costs {1} less to cast for each creature you control with power 4 or greater.
 * Counter target spell unless its controller pays {X}.
 *
 * The cost reduction counts only creatures the caster controls with power 4 or greater
 * (PermanentsYouControlMatching, the "you control" source), and the counter resolves against
 * the spell's chosen X via the same XValue pump used by Mindswipe.
 */
val SpectralDenial = card("Spectral Denial") {
    manaCost = "{X}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "This spell costs {1} less to cast for each creature you control with power " +
        "4 or greater.\nCounter target spell unless its controller pays {X}."

    spell {
        val spell = target(TargetFilter.SpellOnStack)
        effect = Effects.CounterUnlessDynamicPays(DynamicAmounts.xValue())
    }

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.SelfCast,
            modification = CostModification.ReduceGenericBy(
                CostReductionSource.PermanentsYouControlMatching(
                    GameObjectFilter.Creature.powerAtLeast(4)
                )
            ),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "58"
        artist = "Xabi Gaztelua"
        imageUri = "https://cards.scryfall.io/normal/front/e/e/ee4e732a-1ffd-463d-92c2-26187659cfc3.jpg?1743204193"
    }
}

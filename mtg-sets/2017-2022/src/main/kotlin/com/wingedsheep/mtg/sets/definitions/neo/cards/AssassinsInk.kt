package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.CostReductionSource
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget

/**
 * Assassin's Ink
 * {2}{B}{B}
 * Instant
 * This spell costs {1} less to cast if you control an artifact and {1} less to cast if you control an enchantment.
 * Destroy target creature or planeswalker.
 *
 * The two reductions are independent self-cast statics, so an artifact enchantment alone satisfies both.
 */
val AssassinsInk = card("Assassin's Ink") {
    manaCost = "{2}{B}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "This spell costs {1} less to cast if you control an artifact and {1} less to cast if you control an enchantment.\nDestroy target creature or planeswalker."

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.SelfCast,
            modification = CostModification.ReduceGenericBy(
                CostReductionSource.FixedIfControlFilter(amount = 1, filter = GameObjectFilter.Artifact)
            )
        )
    }
    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.SelfCast,
            modification = CostModification.ReduceGenericBy(
                CostReductionSource.FixedIfControlFilter(amount = 1, filter = GameObjectFilter.Enchantment)
            )
        )
    }

    spell {
        val t = target(Targets.CreatureOrPlaneswalker)
        effect = Effects.Destroy(t)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "87"
        artist = "Heonhwa"
        flavorText = "A Reckoner always has another trick up their sleeve—or as their sleeve."
        imageUri = "https://cards.scryfall.io/normal/front/5/a/5a926c10-029d-4e24-8c3f-1808025e30aa.jpg"
    }
}

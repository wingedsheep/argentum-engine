package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.CostReductionSource
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Red Alert!
 * {2}{R}
 * Instant
 *
 * This spell costs {2} less to cast if a creature is attacking you.
 * Creatures you control get +2/+0 until end of turn.
 */
val RedAlert = card("Red Alert!") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "This spell costs {2} less to cast if a creature is attacking you.\n" +
        "Creatures you control get +2/+0 until end of turn."

    spell {
        effect = Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Creature.youControl()),
            Effects.ModifyStats(2, 0, EffectTarget.IterationEntity)
        )
    }

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.SelfCast,
            modification = CostModification.ReduceGenericBy(
                CostReductionSource.FixedIfCreatureAttackingYou(amount = 2),
            ),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "169"
        artist = "Carlos C Diaz"
        flavorText = "Solar flares, quantum filaments, molecular reversion fields . . . space is dangerous before you encounter a single hostile ship."
        imageUri = "https://cards.scryfall.io/normal/front/0/f/0f9b10d3-ba76-45ec-a85c-fb7b26da5561.jpg?1793274269"
    }
}

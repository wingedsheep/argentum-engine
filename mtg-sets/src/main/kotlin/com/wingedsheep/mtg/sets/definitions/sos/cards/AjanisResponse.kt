package com.wingedsheep.mtg.sets.definitions.sos.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.CostReductionSource
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Ajani's Response
 * {4}{W}
 * Instant
 * This spell costs {3} less to cast if it targets a tapped creature.
 * Destroy target creature.
 *
 * The {3} reduction is purely generic, gated on the spell targeting a tapped creature, modeled
 * with [CostReductionSource.FixedIfAnyTargetMatches] on [CostModification.ReduceGenericBy] — the
 * same shape used by Brush Off's generic half. The spell still legally targets any creature; the
 * reduction only applies once the announced target is tapped (CR 601.2f). Excess reduction that
 * cannot match a generic symbol is silently dropped.
 */
val AjanisResponse = card("Ajani's Response") {
    manaCost = "{4}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "This spell costs {3} less to cast if it targets a tapped creature.\n" +
        "Destroy target creature."

    spell {
        val t = target("target", TargetCreature(filter = TargetFilter.Creature))
        effect = Effects.Destroy(t)
    }

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.SelfCast,
            modification = CostModification.ReduceGenericBy(
                CostReductionSource.FixedIfAnyTargetMatches(
                    amount = 3,
                    filter = GameObjectFilter.Creature.tapped(),
                ),
            ),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "6"
        artist = "April Prime"
        flavorText = "Ajani no longer wanted to swing his axe in battle, but nothing came " +
            "easier to him than defending those who needed protection."
        imageUri = "https://cards.scryfall.io/normal/front/9/c/9cd1417a-badc-4abd-a8ca-5b31f85c1072.jpg?1776047920"
    }
}

package com.wingedsheep.mtg.sets.definitions.msh.cards

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
 * Depower — Marvel Super Heroes #50
 * {2}{U} · Instant
 *
 * This spell costs {2} less to cast if it targets an attacking creature.
 * Target creature gets -4/-0 until end of turn.
 * Draw a card.
 *
 * The target-gated discount is the Ride's End / Dragon's Prey shape: a [ModifySpellCost] on
 * [SpellCostTarget.SelfCast] whose [CostModification.ReduceGenericBy] reads a
 * [CostReductionSource.FixedIfAnyTargetMatches] keyed on an attacking-creature filter, so the
 * {2} comes off only while the chosen target is attacking (and only from the generic part of
 * the cost — {U} is never reduced).
 */
val Depower = card("Depower") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "This spell costs {2} less to cast if it targets an attacking creature.\n" +
        "Target creature gets -4/-0 until end of turn.\n" +
        "Draw a card."

    spell {
        val creature = target("target creature", Targets.Creature)
        effect = Effects.ModifyStats(-4, 0, creature) then Effects.DrawCards(1)
    }

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.SelfCast,
            modification = CostModification.ReduceGenericBy(
                CostReductionSource.FixedIfAnyTargetMatches(
                    amount = 2,
                    filter = GameObjectFilter.Creature.attacking(),
                ),
            ),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "50"
        artist = "Nathaniel Himawan"
        flavorText = "\"WAIT . . . I feel funny . . . something's not right.\""
        imageUri = "https://cards.scryfall.io/normal/front/3/6/36e9a6e9-1f9d-4860-97ee-f01e66f8eb4d.jpg?1783902960"
    }
}

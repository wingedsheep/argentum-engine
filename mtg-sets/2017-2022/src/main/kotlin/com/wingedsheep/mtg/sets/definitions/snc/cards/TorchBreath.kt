package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.DynamicAmounts
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
 * Torch Breath
 * {X}{R}
 * Instant
 * This spell costs {2} less to cast if it targets a blue permanent.
 * This spell can't be countered.
 * Torch Breath deals X damage to target creature or planeswalker.
 *
 * The {2} reduction applies to the total cost with X in it (CR 601.2f), so with a blue target
 * X=2 costs just {R}; it can't reduce the {R}.
 */
val TorchBreath = card("Torch Breath") {
    manaCost = "{X}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "This spell costs {2} less to cast if it targets a blue permanent.\n" +
        "This spell can't be countered.\n" +
        "Torch Breath deals X damage to target creature or planeswalker."

    cantBeCountered = true

    spell {
        val t = target(Targets.CreatureOrPlaneswalker)
        effect = Effects.DealDamage(DynamicAmounts.xValue(), t)
    }

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.SelfCast,
            modification = CostModification.ReduceGenericBy(
                CostReductionSource.FixedIfAnyTargetMatches(
                    amount = 2,
                    filter = GameObjectFilter.Permanent.withColor(Color.BLUE),
                ),
            ),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "127"
        artist = "Cristi Balanescu"
        flavorText = "\"Fried squid, comin' up!\""
        imageUri = "https://cards.scryfall.io/normal/front/d/9/d9fbd4a8-c44c-42fc-b946-bdc7e20e1668.jpg?1783923112"
    }
}

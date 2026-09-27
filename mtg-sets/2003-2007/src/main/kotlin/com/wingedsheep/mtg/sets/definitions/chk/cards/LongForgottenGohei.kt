package com.wingedsheep.mtg.sets.definitions.chk.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Long-Forgotten Gohei
 * {3}
 * Artifact
 * Arcane spells you cast cost {1} less to cast.
 * Spirit creatures you control get +1/+1.
 *
 * Daru Warchief's two statics on an artifact: a `ModifySpellCost` keyed on the Arcane spell
 * subtype (generic-only reduction, applied once per spell) and a Spirit lord `ModifyStats`.
 */
val LongForgottenGohei = card("Long-Forgotten Gohei") {
    manaCost = "{3}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "Arcane spells you cast cost {1} less to cast.\nSpirit creatures you control get +1/+1."

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.YouCast(GameObjectFilter.Any.withSubtype("Arcane")),
            modification = CostModification.ReduceGeneric(1),
        )
    }

    staticAbility {
        ability = ModifyStats(
            powerBonus = 1,
            toughnessBonus = 1,
            filter = GroupFilter(GameObjectFilter.Creature.withSubtype("Spirit").youControl())
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "261"
        artist = "Alan Pollack"
        flavorText = "Long ago, the priests would wave the gohei to call down the gods. Now it lies forgotten, but the spirits still feel its pull."
        imageUri = "https://cards.scryfall.io/normal/front/6/f/6fc4b500-ca74-4b90-9a8c-d4efa80ebe2c.jpg?1783944278"

        ruling(
            "2004-12-01",
            "The cost reduction applies to the total cost of the Arcane spell, including any " +
                "additional costs from cards spliced onto it. However, the cost reduction applies " +
                "only once to each Arcane spell."
        )
    }
}

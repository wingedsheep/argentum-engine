package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget

/**
 * Mana Matrix
 * {6}
 * Artifact
 *
 * Instant and enchantment spells you cast cost {2} less to cast.
 */
val ManaMatrix = card("Mana Matrix") {
    manaCost = "{6}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "Instant and enchantment spells you cast cost {2} less to cast."

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.YouCast(GameObjectFilter.Instant or GameObjectFilter.Enchantment),
            modification = CostModification.ReduceGeneric(2),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "285"
        artist = "Mark Tedin"
        imageUri = "https://cards.scryfall.io/normal/front/a/3/a3eedc11-0b47-430c-8391-577a2d05c2ae.jpg?1783948027"
        ruling("2004-10-04", "Multiple Matrices are cumulative. Two will reduce the cost by up to {4}, and so on.")
        ruling("2004-10-04", "Only reduces the generic mana portion of a spell's cost. If the cost does not include generic mana or includes less than {2}, you get a reduced or null effect from this card.")
    }
}

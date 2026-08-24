package com.wingedsheep.mtg.sets.definitions.mmq.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget

/**
 * Squeeze
 * {3}{U}
 * Enchantment
 * Sorcery spells cost {3} more to cast.
 *
 * Symmetrical tax — [SpellCostTarget.AnyCaster], so it hits its own controller too
 * (cf. Thorn of Amethyst).
 */
val Squeeze = card("Squeeze") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment"
    oracleText = "Sorcery spells cost {3} more to cast."

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.AnyCaster(GameObjectFilter.Sorcery),
            modification = CostModification.IncreaseGeneric(3)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "105"
        artist = "DiTerlizzi"
        flavorText = "Any pirate would prefer Rishada's swift and cruel justice to Saprazzo's patient punishments."
        imageUri = "https://cards.scryfall.io/normal/front/b/b/bbe63220-992b-459c-81ca-d4e2de273ce1.jpg"
    }
}

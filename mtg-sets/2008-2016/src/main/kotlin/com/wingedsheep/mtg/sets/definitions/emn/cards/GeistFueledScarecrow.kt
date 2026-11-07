package com.wingedsheep.mtg.sets.definitions.emn.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget

/**
 * Geist-Fueled Scarecrow (Eldritch Moon #196)
 * {4}
 * Artifact Creature — Scarecrow
 * 4/4
 *
 * Creature spells you cast cost {1} more to cast.
 */
val GeistFueledScarecrow = card("Geist-Fueled Scarecrow") {
    manaCost = "{4}"
    colorIdentity = ""
    typeLine = "Artifact Creature — Scarecrow"
    power = 4
    toughness = 4
    oracleText = "Creature spells you cast cost {1} more to cast."

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.YouCast(GameObjectFilter.Creature),
            modification = CostModification.IncreaseGeneric(1),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "196"
        artist = "Adam Paquette"
        flavorText = "\"It'll keep the crows away. Works on neighbors too.\"\n—Old Rutstein"
        imageUri = "https://cards.scryfall.io/normal/front/8/9/89c9cafb-4543-409f-a98a-df0e7a0f2749.jpg"
    }
}

package com.wingedsheep.mtg.sets.definitions.fem.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget

/**
 * Derelor
 * {3}{B}
 * Creature — Thrull
 * 4/4
 * Black spells you cast cost {B} more to cast.
 *
 * The inverse of Jet Medallion — the same `ModifySpellCost` static, taxing rather than
 * discounting, and in coloured rather than generic mana. It taxes Derelor's own controller
 * only, and it matches on the spell's *colors*, so a partly-black multicolored spell is taxed.
 */
val Derelor = card("Derelor") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Thrull"
    oracleText = "Black spells you cast cost {B} more to cast."
    power = 4
    toughness = 4

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.YouCast(GameObjectFilter.Any.withColor(Color.BLACK)),
            modification = CostModification.IncreaseColored("{B}"),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "36"
        artist = "Anson Maddocks"
        flavorText = "\"Strength it has, but at the cost of a continuous supply of energy. Such failure can bear only one result.\"\n—From the execution order for Endrek Sahr, Master Breeder"
        imageUri = "https://cards.scryfall.io/normal/front/9/e/9eb2b79f-f09a-49dc-8e0f-7d711ba78981.jpg?1783947903"
    }
}

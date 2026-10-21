package com.wingedsheep.mtg.sets.definitions.mbs.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Vivisection
 * {3}{U}
 * Sorcery
 * As an additional cost to cast this spell, sacrifice a creature.
 * Draw three cards.
 */
val Vivisection = card("Vivisection") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "As an additional cost to cast this spell, sacrifice a creature.\nDraw three cards."
    additionalCost(Costs.additional.SacrificePermanent(GameObjectFilter.Creature))
    spell {
        effect = Effects.DrawCards(3)
    }
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "38"
        artist = "Anthony Francisco"
        flavorText = "Phyrexians research with the grace of surgeons and the finesse of butchers."
        imageUri = "https://cards.scryfall.io/normal/front/6/8/684a3631-f2d6-4a64-a04a-893f452e3a60.jpg"
    }
}

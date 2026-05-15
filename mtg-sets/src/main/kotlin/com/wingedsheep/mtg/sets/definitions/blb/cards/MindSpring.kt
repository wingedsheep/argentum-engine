package com.wingedsheep.mtg.sets.definitions.blb.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Mind Spring
 * {X}{U}{U}
 * Sorcery
 *
 * Draw X cards.
 */
val MindSpring = card("Mind Spring") {
    manaCost = "{X}{U}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "Draw X cards."

    spell {
        effect = Effects.DrawCards(DynamicAmount.XValue)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "389"
        artist = "Mark Zug"
        imageUri = "https://cards.scryfall.io/normal/front/e/7/e7e7d174-eb7c-41ad-a241-cfbfdc71e3a7.jpg?1721428086"
        inBooster = false
    }
}

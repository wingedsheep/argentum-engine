package com.wingedsheep.mtg.sets.definitions.jou.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Font of Vigor
 * {1}{W}
 * Enchantment
 *
 * {2}{W}, Sacrifice this enchantment: You gain 7 life.
 */
val FontOfVigor = card("Font of Vigor") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment"
    oracleText = "{2}{W}, Sacrifice this enchantment: You gain 7 life."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}{W}"), Costs.SacrificeSelf)
        effect = Effects.GainLife(7)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "11"
        artist = "Noah Bradley"
        flavorText = "Drink deep, and be restored."
        imageUri = "https://cards.scryfall.io/normal/front/d/4/d4ef3a8e-ef8b-417a-a3a9-cb0ce88cb0c9.jpg?1783939461"
    }
}

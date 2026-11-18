package com.wingedsheep.mtg.sets.definitions.jou.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Font of Fortunes
 * {1}{U}
 * Enchantment
 *
 * {1}{U}, Sacrifice this enchantment: Draw two cards.
 */
val FontOfFortunes = card("Font of Fortunes") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment"
    oracleText = "{1}{U}, Sacrifice this enchantment: Draw two cards."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}{U}"), Costs.SacrificeSelf)
        effect = Effects.DrawCards(2)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "38"
        artist = "Jonas De Ro"
        flavorText = "Drink deep, and your thoughts will flow freely."
        imageUri = "https://cards.scryfall.io/normal/front/2/5/25de3481-9b0d-4747-a97b-171b6aa41073.jpg?1783939448"
    }
}

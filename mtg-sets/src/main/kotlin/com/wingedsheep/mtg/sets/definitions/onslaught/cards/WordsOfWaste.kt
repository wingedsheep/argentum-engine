package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ReplaceNextDrawWithDiscardEffect

/**
 * Words of Waste
 * {2}{B}
 * Enchantment
 * {1}: The next time you would draw a card this turn, each opponent discards a card instead.
 */
val WordsOfWaste = card("Words of Waste") {
    manaCost = "{2}{B}"
    typeLine = "Enchantment"
    oracleText = "{1}: The next time you would draw a card this turn, each opponent discards a card instead."

    activatedAbility {
        cost = Costs.Mana("{1}")
        effect = ReplaceNextDrawWithDiscardEffect
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "182"
        artist = "Jerry Tiritilli"
        flavorText = "Terror corrupts order and paralyzes instinct."
        imageUri = "https://cards.scryfall.io/large/front/d/2/d2dcb8ed-23e7-4cee-9f43-042232c6035a.jpg?1562937190"
    }
}

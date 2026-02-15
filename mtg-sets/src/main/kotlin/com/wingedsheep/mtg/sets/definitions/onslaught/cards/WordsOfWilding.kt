package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ReplaceNextDrawWithBearTokenEffect

/**
 * Words of Wilding
 * {2}{G}
 * Enchantment
 * {1}: The next time you would draw a card this turn, create a 2/2 green Bear
 * creature token instead.
 */
val WordsOfWilding = card("Words of Wilding") {
    manaCost = "{2}{G}"
    typeLine = "Enchantment"
    oracleText = "{1}: The next time you would draw a card this turn, create a 2/2 green Bear creature token instead."

    activatedAbility {
        cost = Costs.Mana("{1}")
        effect = ReplaceNextDrawWithBearTokenEffect
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "305"
        artist = "Ron Spears"
        imageUri = "https://cards.scryfall.io/large/front/f/d/fdb9565f-5b09-4127-b169-3146079dab84.jpg?1562955080"
    }
}

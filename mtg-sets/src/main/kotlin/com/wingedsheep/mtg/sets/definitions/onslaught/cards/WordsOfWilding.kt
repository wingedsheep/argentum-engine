package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

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
        effect = Effects.ReplaceNextDraw(
            Effects.CreateToken(power = 2, toughness = 2, colors = setOf(Color.GREEN), creatureTypes = setOf("Bear"))
        )
        promptOnDraw = true
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "305"
        artist = "Ron Spears"
        imageUri = "https://cards.scryfall.io/normal/front/f/d/fdb9565f-5b09-4127-b169-3146079dab84.jpg?1562955080"
    }
}

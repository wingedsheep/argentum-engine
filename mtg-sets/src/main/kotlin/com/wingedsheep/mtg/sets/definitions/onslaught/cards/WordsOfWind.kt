package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Words of Wind
 * {2}{U}
 * Enchantment
 * {1}: The next time you would draw a card this turn, each player returns a permanent
 * they control to its owner's hand instead.
 */
val WordsOfWind = card("Words of Wind") {
    manaCost = "{2}{U}"
    typeLine = "Enchantment"
    oracleText = "{1}: The next time you would draw a card this turn, each player returns a permanent they control to its owner's hand instead."

    activatedAbility {
        cost = Costs.Mana("{1}")
        effect = Effects.ReplaceNextDraw(Effects.EachPlayerReturnPermanentToHand())
        promptOnDraw = true
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "122"
        artist = "Eric Peterson"
        flavorText = "Be logical in all things. Do not allow instinct or passion to cloud your mind."
        imageUri = "https://cards.scryfall.io/normal/front/5/5/5595a57a-a76c-467b-afaf-5affffc24f35.jpg?1562915041"
    }
}

package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Words of Worship
 * {2}{W}
 * Enchantment
 * {1}: The next time you would draw a card this turn, you gain 5 life instead.
 */
val WordsOfWorship = card("Words of Worship") {
    manaCost = "{2}{W}"
    typeLine = "Enchantment"
    oracleText = "{1}: The next time you would draw a card this turn, you gain 5 life instead."

    activatedAbility {
        cost = Costs.Mana("{1}")
        effect = Effects.ReplaceNextDraw(Effects.GainLife(5))
        promptOnDraw = true
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "61"
        artist = "Rebecca Guay"
        flavorText = "The faithful don't succumb to terror, nor are they ruled by passion. They adhere to order, for order is life."
        imageUri = "https://cards.scryfall.io/normal/front/0/e/0ea5c6e0-8361-4214-997b-32a66b19fae9.jpg?1562898374"
    }
}

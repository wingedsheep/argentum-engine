package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Urban Evolution
 * {3}{G}{U}
 * Sorcery
 *
 * Draw three cards. You may play an additional land this turn.
 */
val UrbanEvolution = card("Urban Evolution") {
    manaCost = "{3}{G}{U}"
    colorIdentity = "UG"
    typeLine = "Sorcery"
    oracleText = "Draw three cards. You may play an additional land this turn."

    spell {
        effect = Effects.DrawCards(3) then Effects.PlayAdditionalLands(1)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "204"
        artist = "Eytan Zana"
        flavorText = "As the Simic released more of their krasis experiments, they required new habitats, always at the expense of the locals."
        imageUri = "https://cards.scryfall.io/normal/front/9/f/9fcd6fac-2cde-4a89-b484-b910be2dcecf.jpg?1783940098"
    }
}

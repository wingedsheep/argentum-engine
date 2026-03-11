package com.wingedsheep.mtg.sets.definitions.dominaria.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Divination
 * {2}{U}
 * Sorcery
 * Draw two cards.
 */
val Divination = card("Divination") {
    manaCost = "{2}{U}"
    typeLine = "Sorcery"
    oracleText = "Draw two cards."

    spell {
        effect = Effects.DrawCards(2)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "52"
        artist = "Matt Stewart"
        flavorText = "\"Half of your studies will be learning the laws of magic. The other half will be bending them.\" —Naru Meha, master wizard"
        imageUri = "https://cards.scryfall.io/normal/front/c/7/c7f9daf0-dbfd-45b2-be35-9c2de9d1a56e.jpg?1562742725"
    }
}

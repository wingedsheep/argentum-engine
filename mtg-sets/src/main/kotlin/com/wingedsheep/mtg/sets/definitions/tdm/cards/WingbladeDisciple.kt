package com.wingedsheep.mtg.sets.definitions.tdm.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Wingblade Disciple
 * {2}{U}
 * Creature — Human Monk
 * 2/2
 *
 * Flying
 * Flurry — Whenever you cast your second spell each turn, create a 1/1 white Bird
 * creature token with flying.
 */
val WingbladeDisciple = card("Wingblade Disciple") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Monk"
    power = 2
    toughness = 2
    oracleText = "Flying\nFlurry — Whenever you cast your second spell each turn, create a 1/1 white Bird creature token with flying."

    keywords(Keyword.FLYING)

    flurry {
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Bird"),
            keywords = setOf(Keyword.FLYING),
            imageUri = "https://cards.scryfall.io/normal/front/6/1/6105623a-ff2c-46bf-8881-e8b899d47d54.jpg?1742506584"
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "65"
        artist = "Julian Kok Joon Wen"
        flavorText = "\"Our greatest teachers are those which the natural world has so thoroughly refined.\""
        imageUri = "https://cards.scryfall.io/normal/front/7/1/71de7dca-0231-4407-86a0-c7fc95f5aaa0.jpg?1743204222"
    }
}

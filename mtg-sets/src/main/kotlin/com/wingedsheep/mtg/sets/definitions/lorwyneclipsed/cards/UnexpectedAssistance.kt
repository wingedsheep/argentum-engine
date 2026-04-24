package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Unexpected Assistance
 * {3}{U}{U}
 * Instant
 *
 * Convoke (Your creatures can help cast this spell. Each creature you tap while
 * casting this spell pays for {1} or one mana of that creature's color.)
 * Draw three cards, then discard a card.
 */
val UnexpectedAssistance = card("Unexpected Assistance") {
    manaCost = "{3}{U}{U}"
    typeLine = "Instant"
    oracleText = "Convoke (Your creatures can help cast this spell. Each creature you tap while casting this spell pays for {1} or one mana of that creature's color.)\n" +
        "Draw three cards, then discard a card."

    keywords(Keyword.CONVOKE)

    spell {
        effect = Effects.DrawCards(3).then(EffectPatterns.discardCards(1))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "80"
        artist = "Gustavo Pelissari"
        flavorText = "\"In one breath, Abigale's mind turned from panic to poetry.\""
        imageUri = "https://cards.scryfall.io/normal/front/7/b/7b540a5f-7f1b-421a-864f-5af469556fc6.jpg?1765451670"
    }
}

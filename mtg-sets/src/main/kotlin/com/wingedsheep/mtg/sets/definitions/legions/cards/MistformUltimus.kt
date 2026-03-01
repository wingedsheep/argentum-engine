package com.wingedsheep.mtg.sets.definitions.legions.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Mistform Ultimus
 * {3}{U}
 * Legendary Creature — Illusion
 * 3/3
 *
 * Mistform Ultimus is every creature type (even if this card isn't on the battlefield).
 *
 * This is a characteristic-defining ability, functionally equivalent to Changeling.
 * It applies in all zones, not just the battlefield (Rule 604.3).
 */
val MistformUltimus = card("Mistform Ultimus") {
    manaCost = "{3}{U}"
    typeLine = "Legendary Creature — Illusion"
    power = 3
    toughness = 3
    oracleText = "Mistform Ultimus is every creature type (even if this card isn't on the battlefield)."

    keywords(Keyword.CHANGELING)

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "47"
        artist = "Anthony S. Waters"
        flavorText = "\"It may wear your face, but its mind is its own.\""
        imageUri = "https://cards.scryfall.io/normal/front/e/3/e3be21c3-9b83-430b-be0a-792de9a680e3.jpg?1562940674"
    }
}

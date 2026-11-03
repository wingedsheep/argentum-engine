package com.wingedsheep.mtg.sets.definitions.xln.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Ritual of Rejuvenation
 * {2}{W}
 * Instant
 * You gain 4 life.
 * Draw a card.
 */
val RitualOfRejuvenation = card("Ritual of Rejuvenation") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "You gain 4 life.\nDraw a card."

    spell {
        effect = Effects.GainLife(4) then Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "32"
        artist = "Bastien L. Deharme"
        flavorText = "\"Nothing is more sacred than the gift of blood. Blessed are the vein that empties and the heart that is renewed.\""
        imageUri = "https://cards.scryfall.io/normal/front/6/9/691c0acb-1882-482f-9513-e7d010cc1d15.jpg?1783935794"
    }
}

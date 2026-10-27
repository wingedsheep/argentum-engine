package com.wingedsheep.mtg.sets.definitions.m19.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Revitalize
 * {1}{W}
 * Instant
 * You gain 3 life.
 * Draw a card.
 */
val Revitalize = card("Revitalize") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "You gain 3 life.\nDraw a card."

    spell {
        effect = Effects.GainLife(3) then Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "35"
        artist = "Matt Stewart"
        flavorText = "\"A potion is no substitute for a skilled surgeon, but it will hold your bits in.\"\n—Torricks, battlefield medic"
        imageUri = "https://cards.scryfall.io/normal/front/d/f/dfbdd90c-1ae3-45e5-b1e5-5b8615a1511f.jpg?1783934597"
    }
}

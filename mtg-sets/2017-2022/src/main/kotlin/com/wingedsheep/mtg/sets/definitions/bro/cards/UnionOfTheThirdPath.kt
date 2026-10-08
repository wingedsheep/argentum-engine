package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Union of the Third Path
 * {2}{W}
 * Instant
 * Draw a card, then you gain life equal to the number of cards in your hand.
 *
 * The hand is counted at resolution of the life gain, after the draw — so the drawn card counts.
 */
val UnionOfTheThirdPath = card("Union of the Third Path") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Draw a card, then you gain life equal to the number of cards in your hand."

    spell {
        effect = Effects.DrawCards(1) then Effects.GainLife(DynamicAmounts.cardsInYourHand())
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "31"
        artist = "Robin Olausson"
        flavorText = "Terisia City at last opened its ivory towers to all who sought respite from the endless war."
        imageUri = "https://cards.scryfall.io/normal/front/4/8/486a0745-7360-4cc9-9cc2-30c0eda6e00c.jpg"
    }
}

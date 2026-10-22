package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Sunset Revelry (Innistrad: Midnight Hunt #38)
 * {1}{W}
 * Sorcery
 * If an opponent has more life than you, you gain 4 life.
 * If an opponent controls more creatures than you, create two 1/1 white Human creature tokens.
 * If an opponent has more cards in hand than you, draw a card.
 *
 * Each clause is its own [Effects.If], checked independently and in order at resolution (the same
 * composition as Beza, the Bounding Spring).
 */
val SunsetRevelry = card("Sunset Revelry") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"
    oracleText = "If an opponent has more life than you, you gain 4 life.\n" +
        "If an opponent controls more creatures than you, create two 1/1 white Human creature tokens.\n" +
        "If an opponent has more cards in hand than you, draw a card."

    spell {
        effect = Effects.If(
            condition = Conditions.LessLifeThanOpponent,
            then = Effects.GainLife(4)
        ) then Effects.If(
            condition = Conditions.OpponentControlsMoreCreatures,
            then = Effects.CreateToken(
                power = 1,
                toughness = 1,
                colors = setOf(Color.WHITE),
                creatureTypes = setOf("Human"),
                count = 2
            )
        ) then Effects.If(
            condition = Conditions.OpponentHasMoreCardsInHand,
            then = Effects.DrawCards(1)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "38"
        artist = "Antonio José Manzanedo"
        imageUri = "https://cards.scryfall.io/normal/front/6/1/61b39ef1-29d4-4c8d-aece-a3f1ce008e2d.jpg?1783925649"
    }
}

package com.wingedsheep.mtg.sets.definitions.mrd.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.entwineModal
import com.wingedsheep.sdk.dsl.entwine
import com.wingedsheep.sdk.model.Rarity

/**
 * Promise of Power
 * {2}{B}{B}{B}
 * Sorcery
 * Choose one —
 * • You draw five cards and you lose 5 life.
 * • Create an X/X black Demon creature token with flying, where X is the number of cards in your hand.
 * Entwine {4} (Choose both if you pay the entwine cost.)
 */
val PromiseOfPower = card("Promise of Power") {
    manaCost = "{2}{B}{B}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Choose one —\n" +
        "• You draw five cards and you lose 5 life.\n" +
        "• Create an X/X black Demon creature token with flying, where X is the number of cards in your hand.\n" +
        "Entwine {4} (Choose both if you pay the entwine cost.)"

    entwine("{4}")

    spell {
        entwineModal {
            mode("Draw five cards and lose 5 life") {
                effect = Effects.DrawCards(5) then Effects.LoseLife(5)
            }
            mode("Create an X/X black Demon creature token with flying") {
                effect = Effects.CreateDynamicToken(
                    dynamicPower = DynamicAmounts.cardsInYourHand(),
                    dynamicToughness = DynamicAmounts.cardsInYourHand(),
                    colors = setOf(Color.BLACK),
                    creatureTypes = setOf("Demon"),
                    keywords = setOf(Keyword.FLYING),
                    imageUri = "https://cards.scryfall.io/normal/front/c/6/c6df5992-9c1c-407d-9602-a6c659342a15.jpg?1783927202",
                )
            }
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "74"
        artist = "Kev Walker"
        imageUri = "https://cards.scryfall.io/normal/front/d/9/d96c3947-0eec-48a1-ba69-59734b4ac9da.jpg?1783944545"
        ruling(
            "2004-12-01",
            "The power and toughness of the Demon token are set when Promise of Power resolves. " +
                "They’re unaffected if the number of cards in your hand changes later.",
        )
        ruling(
            "2004-12-01",
            "If you pay the entwine cost, you draw five cards, then lose five life, then put the " +
                "Demon token onto the battlefield. The five cards you draw count toward the " +
                "Demon’s power and toughness.",
        )
    }
}

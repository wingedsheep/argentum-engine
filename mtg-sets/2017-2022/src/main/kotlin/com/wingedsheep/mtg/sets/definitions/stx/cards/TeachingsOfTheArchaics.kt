package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.plus
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Teachings of the Archaics — Strixhaven: School of Mages #57 (canonical printing)
 * {2}{U} · Sorcery — Lesson
 *
 * If an opponent has more cards in hand than you, draw two cards. Draw three cards instead if an
 * opponent has at least four more cards in hand than you.
 *
 * Both clauses are "an opponent" — existential — so each compares the *largest* opponent hand
 * ([DynamicAmounts.greatestAmongPlayers] of "cards in your hand" read as each opponent, Windfall's
 * rebinding) against your own, evaluated as the spell resolves (ruling). The "instead" clause is
 * checked first; failing it falls through to the two-card clause, and failing both does nothing.
 */
val TeachingsOfTheArchaics = card("Teachings of the Archaics") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery — Lesson"
    oracleText = "If an opponent has more cards in hand than you, draw two cards. Draw three cards " +
        "instead if an opponent has at least four more cards in hand than you."

    spell {
        val largestOpponentHand = DynamicAmounts.greatestAmongPlayers(
            DynamicAmounts.cardsInYourHand(),
            Player.EachOpponent,
        )
        val yourHand = DynamicAmounts.cardsInYourHand()
        effect = Effects.If(
            condition = Conditions.CompareAmounts(largestOpponentHand, ComparisonOperator.GTE, yourHand + 4),
            then = Effects.DrawCards(3),
            otherwise = Effects.If(
                condition = Conditions.CompareAmounts(largestOpponentHand, ComparisonOperator.GT, yourHand),
                then = Effects.DrawCards(2),
            ),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "57"
        artist = "Lius Lasahido"
        flavorText = "Mages seek out the ancient beings known as the Archaics for their boundless " +
            "knowledge of history and magic, dispensed only in cryptic metaphors."
        imageUri = "https://cards.scryfall.io/normal/front/9/6/967148b1-2bb6-4bc0-95e6-c45fcf99afd2.jpg?1783927373"
        ruling("2021-04-16", "The number of cards in each player’s hand is determined as Teachings of the Archaics resolves.")
    }
}

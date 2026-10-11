package com.wingedsheep.mtg.sets.definitions.blc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.minus
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Mr. Foxglove
 * {2}{G}{W}{U}
 * Legendary Creature — Fox Rogue
 * 3/5
 * Lifelink
 * Whenever Mr. Foxglove attacks, draw cards equal to the number of cards in defending player's
 * hand minus the number of cards in your hand. If you didn't draw cards this way, you may put a
 * creature card from your hand onto the battlefield.
 *
 * A non-positive difference draws nothing (the card's ruling), hence `nonNegative`. "If you didn't
 * draw cards this way" is read from what actually happened, not from the difference: the growth of
 * your cards-drawn-this-turn tally across the draw (the Rise of the Deathbringer spelling), so an
 * empty library — which draws nothing — also opens the put. `putFromHand`'s `ChooseUpTo(1)` is the
 * "you may".
 */
val MrFoxglove = card("Mr. Foxglove") {
    manaCost = "{2}{G}{W}{U}"
    colorIdentity = "GWU"
    typeLine = "Legendary Creature — Fox Rogue"
    power = 3
    toughness = 5
    oracleText = "Lifelink\n" +
        "Whenever Mr. Foxglove attacks, draw cards equal to the number of cards in defending " +
        "player's hand minus the number of cards in your hand. If you didn't draw cards this way, " +
        "you may put a creature card from your hand onto the battlefield."

    keywords(Keyword.LIFELINK)

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.Pipeline {
            val drawnThisTurn = DynamicAmounts.cardsDrawnThisTurn(Player.You)
            val drawnBefore = storeNumber(drawnThisTurn)
            run(Effects.DrawCards(
                DynamicAmounts.nonNegative(
                    DynamicAmounts.count(Player.DefendingPlayer, Zone.HAND) -
                        DynamicAmounts.count(Player.You, Zone.HAND)
                )
            ))
            run(Effects.If(
                condition = Conditions.CompareAmounts(
                    drawnThisTurn - drawnBefore.amount,
                    ComparisonOperator.LTE,
                    0
                ),
                then = Patterns.Hand.putFromHand(GameObjectFilter.Creature)
            ))
        }
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "6"
        artist = "Anna Podedworna"
        imageUri = "https://cards.scryfall.io/normal/front/c/b/cba58beb-9524-46d4-ac63-119f19d9d44f.jpg?1783910737"
        ruling(
            "2024-07-26",
            "If the number of cards in defending player's hand minus the number of cards in your hand " +
                "is 0 or less, you won't draw any cards."
        )
    }
}

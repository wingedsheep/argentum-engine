package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.RestrictCardsDrawnPerTurn
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Narset, Parter of Veils
 * {1}{U}{U}
 * Legendary Planeswalker — Narset
 * Starting Loyalty: 5
 *
 * Each opponent can't draw more than one card each turn.
 * −2: Look at the top four cards of your library. You may reveal a noncreature, nonland card from
 * among them and put it into your hand. Put the rest on the bottom of your library in a random order.
 */
val NarsetParterOfVeils = card("Narset, Parter of Veils") {
    manaCost = "{1}{U}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Planeswalker — Narset"
    startingLoyalty = 5
    oracleText = "Each opponent can't draw more than one card each turn.\n" +
        "−2: Look at the top four cards of your library. You may reveal a noncreature, nonland card from among them and put it into your hand. Put the rest on the bottom of your library in a random order."

    staticAbility {
        ability = RestrictCardsDrawnPerTurn(affected = Player.EachOpponent, maxPerTurn = 1)
    }

    loyaltyAbility(-2) {
        effect = Patterns.Library.lookAtTopRevealMatchingToHand(
            count = 4,
            filter = GameObjectFilter(cardPredicates = listOf(CardPredicate.IsNoncreature, CardPredicate.IsNonland)),
            prompt = "You may reveal a noncreature, nonland card from among them and put it into your hand"
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "61"
        artist = "Magali Villeneuve"
        imageUri = "https://cards.scryfall.io/normal/front/8/c/8c39f9b4-02b9-4d44-b8d6-4fd02ebbb0c5.jpg?1783933458"

        ruling("2019-05-03", "Your opponents can each draw a maximum of one card each on each player's turn. Subsequent card draws during that turn are ignored.")
        ruling("2019-05-03", "If an opponent hasn't drawn any cards in a turn and a spell or ability instructs that player to draw multiple cards, that player will just draw one card. However, if the draws are optional, the player can't choose to draw, even if they could draw one card this way.")
        ruling("2019-05-03", "Narset will \"see\" cards drawn by opponents earlier in the turn she entered the battlefield, although Narset can't affect cards drawn before she entered the battlefield. For example, if an opponent draws two cards, then Narset enters the battlefield, that opponent can't draw more cards that turn, but the two drawn cards are unaffected.")
        ruling("2019-05-03", "Replacement effects (such as that of Underrealm Lich or the first ability of Jace, Wielder of Mysteries) can't be used to replace draws that Narset disallows. However, if an opponent's first draw is replaced (by Underrealm Lich's ability, for example), that draw didn't happen and Narset won't stop the next draw (which may also be replaced by Underrealm Lich's ability).")
    }
}

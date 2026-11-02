package com.wingedsheep.mtg.sets.definitions.m19.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate

/**
 * Liliana's Spoils — {3}{B} Sorcery
 *
 * Target opponent discards a card.
 * Look at the top five cards of your library. You may reveal a black card from among them and put it
 * into your hand. Put the rest on the bottom of your library in a random order.
 */
val LilianasSpoils = card("Liliana's Spoils") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Target opponent discards a card.\n" +
        "Look at the top five cards of your library. You may reveal a black card from among them and put it " +
        "into your hand. Put the rest on the bottom of your library in a random order."

    spell {
        val opponent = target(Targets.Opponent)
        effect = Effects.Discard(1, opponent) then
            Patterns.Library.lookAtTopRevealMatchingToHand(
                count = 5,
                filter = GameObjectFilter.Any.withCardPredicate(CardPredicate.HasColor(Color.BLACK)),
                prompt = "You may reveal a black card from among them and put it into your hand",
            )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "294"
        artist = "Tyler Jacobson"
        imageUri = "https://cards.scryfall.io/normal/front/e/9/e960fb5a-70cc-490d-99cf-99bc65485cfb.jpg?1783934485"
        ruling(
            "2018-07-13",
            "You may cast Liliana's Spoils targeting any opponent, even one who has no cards in hand. " +
                "You'll perform the actions of its second ability even if the target opponent can't discard a card."
        )
    }
}

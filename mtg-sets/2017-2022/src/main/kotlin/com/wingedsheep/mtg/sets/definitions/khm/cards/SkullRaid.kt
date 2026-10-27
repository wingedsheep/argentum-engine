package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.minus
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Skull Raid
 * {3}{B}
 * Sorcery
 * Target opponent discards two cards. If fewer than two cards were discarded this way, you draw
 * cards equal to the difference.
 * Foretell {1}{B}
 *
 * The opponent's discard is the stock [Patterns.Hand.discardCards] pipeline, which records the
 * cards actually discarded in [Patterns.Hand.discarded]. The follow-up draw is
 * `max(0, 2 - discarded)` — [DynamicAmounts.nonNegative] over a subtraction — so a
 * one-card hand draws you one, an empty hand draws you two, and a full discard draws nothing.
 * Foretell is lowered into [KeywordAbility.foretell].
 */
val SkullRaid = card("Skull Raid") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Target opponent discards two cards. If fewer than two cards were discarded " +
        "this way, you draw cards equal to the difference.\n" +
        "Foretell {1}{B} (During your turn, you may pay {2} and exile this card from your hand " +
        "face down. Cast it on a later turn for its foretell cost.)"

    spell {
        val opponent = target(Targets.Opponent)
        effect = Patterns.Hand.discardCards(2, opponent) then
            Effects.DrawCards(
                DynamicAmounts.nonNegative(DynamicAmounts.fixed(2) - Patterns.Hand.discarded.count)
            )
    }

    keywordAbility(KeywordAbility.foretell("{1}{B}"))

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "111"
        artist = "Igor Kieryluk"
        imageUri = "https://cards.scryfall.io/normal/front/4/5/4589c520-4db3-47bf-bfb2-5d7e02ede4bf.jpg?1783928240"

        ruling(
            "2021-02-05",
            "The opponent must discard two cards if they have two or more cards in their hand. " +
                "If they have only one card in hand, they'll discard that card and you'll draw a " +
                "card. If they have no cards in hand, they can't discard any, so you'll draw two cards."
        )
    }
}

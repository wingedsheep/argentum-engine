package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.CardOrder

/**
 * Arcane Infusion
 * {U}{R}
 * Instant
 * Look at the top four cards of your library. You may reveal an instant or sorcery card from among
 * them and put it into your hand. Put the rest on the bottom of your library in a random order.
 * Flashback {3}{U}{R}
 *
 * Peer Through Depths' shape over four cards, with the rest going to the bottom in a random order.
 */
val ArcaneInfusion = card("Arcane Infusion") {
    manaCost = "{U}{R}"
    colorIdentity = "RU"
    typeLine = "Instant"
    oracleText = "Look at the top four cards of your library. You may reveal an instant or sorcery card from " +
        "among them and put it into your hand. Put the rest on the bottom of your library in a random order.\n" +
        "Flashback {3}{U}{R} (You may cast this card from your graveyard for its flashback cost. Then exile it.)"

    spell {
        effect = Patterns.Library.lookAtTopRevealMatchingToHand(
            count = 4,
            filter = GameObjectFilter.InstantOrSorcery,
            prompt = "You may reveal an instant or sorcery card from among them and put it into your hand",
            restOrder = CardOrder.Random,
        )
    }

    keywordAbility(KeywordAbility.flashback("{3}{U}{R}"))

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "210"
        artist = "Jason Felix"
        imageUri = "https://cards.scryfall.io/normal/front/9/a/9ab520b7-ad87-4c8e-a454-5c022e378564.jpg"
    }
}

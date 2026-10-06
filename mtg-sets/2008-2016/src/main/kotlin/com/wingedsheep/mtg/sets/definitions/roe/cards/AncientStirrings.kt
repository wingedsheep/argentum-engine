package com.wingedsheep.mtg.sets.definitions.roe.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.predicates.CardPredicate

/**
 * Ancient Stirrings
 * {G}
 * Sorcery
 * Look at the top five cards of your library. You may reveal a colorless card from among them and put it into
 * your hand. Then put the rest on the bottom of your library in any order.
 *
 * "A colorless card" is any card with [CardPredicate.IsColorless] — lands and artifacts with no coloured
 * pips included. "…in any order" is [CardOrder.ControllerChooses].
 */
val AncientStirrings = card("Ancient Stirrings") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Look at the top five cards of your library. You may reveal a colorless card from among them " +
        "and put it into your hand. Then put the rest on the bottom of your library in any order."

    spell {
        effect = Patterns.Library.lookAtTopRevealMatchingToHand(
            count = 5,
            filter = GameObjectFilter.Any.withCardPredicate(CardPredicate.IsColorless),
            prompt = "You may reveal a colorless card from among them and put it into your hand",
            restDestination = CardDestination.ToZone(Zone.LIBRARY, placement = ZonePlacement.Bottom),
            restOrder = CardOrder.ControllerChooses,
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "174"
        artist = "Vincent Proce"
        imageUri = "https://cards.scryfall.io/normal/front/9/7/97ac2766-a597-4949-882d-c5e61e6dd268.jpg?1783941968"
        ruling("2021-03-19", "Objects with no mana cost, including lands that could produce colored mana, are colorless by default.")
    }
}

package com.wingedsheep.mtg.sets.definitions.xln.cards

import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardOrder

val CommuneWithDinosaurs = card("Commune with Dinosaurs") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Look at the top five cards of your library. You may reveal a Dinosaur or land card from among them and put it into your hand. Put the rest on the bottom of your library in any order."

    spell {
        effect = Patterns.Library.lookAtTopRevealMatchingToHand(
            count = 5,
            filter = GameObjectFilter.Any.withSubtype("Dinosaur") or GameObjectFilter.Land,
            prompt = "You may reveal a Dinosaur or land card and put it into your hand",
            restOrder = CardOrder.ControllerChooses
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "181"
        artist = "Jonathan Kuo"
        imageUri = "https://cards.scryfall.io/normal/front/d/e/de4b9a07-6631-477a-8c77-3b04f211439d.jpg?1783935729"
        ruling("2018-01-19", "If an effect refers to a \"[subtype] spell\" or \"[subtype] card,\" it refers only to a spell or card that has that subtype. For example, March of the Drowned is a card that benefits Pirates and features Pirates in its illustration, but it isn't a Pirate card.")
    }
}

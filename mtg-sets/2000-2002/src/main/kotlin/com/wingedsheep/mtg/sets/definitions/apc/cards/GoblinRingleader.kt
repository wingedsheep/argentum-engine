package com.wingedsheep.mtg.sets.definitions.apc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardOrder

/**
 * Goblin Ringleader
 * {3}{R}
 * Creature — Goblin
 * 2/2
 *
 * Haste
 * When this creature enters, reveal the top four cards of your library. Put all Goblin cards
 * revealed this way into your hand and the rest on the bottom of your library in any order.
 *
 * Sylvan Messenger's shape: [Patterns.Library.revealTopPutAllMatchingToHand] with the printed
 * "in any order" as [CardOrder.ControllerChooses].
 */
val GoblinRingleader = card("Goblin Ringleader") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Goblin"
    oracleText = "Haste (This creature can attack and {T} as soon as it comes under your control.)\n" +
        "When this creature enters, reveal the top four cards of your library. Put all Goblin cards " +
        "revealed this way into your hand and the rest on the bottom of your library in any order."
    power = 2
    toughness = 2

    keywords(Keyword.HASTE)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Patterns.Library.revealTopPutAllMatchingToHand(
            count = 4,
            filter = GameObjectFilter.Any.withSubtype(Subtype.GOBLIN),
            restOrder = CardOrder.ControllerChooses
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "62"
        artist = "Mark Romanoski"
        imageUri = "https://cards.scryfall.io/normal/front/b/6/b6b2cd77-9552-48b1-80cb-26966323c1ea.jpg?1783945344"
        ruling(
            "2019-07-12",
            "If an effect refers to a “[subtype] spell” or “[subtype] card,” it refers only to a spell or " +
                "card that has that subtype. For example, Goblin Gathering is a card that creates Goblins " +
                "and features Goblins in its illustration, but it isn’t a Goblin card."
        )
    }
}

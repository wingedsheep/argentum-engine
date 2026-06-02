package com.wingedsheep.mtg.sets.definitions.eoe.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.dsl.LibraryPatterns

/**
 * Consult the Star Charts
 * {1}{U}
 * Instant
 *
 * Kicker {1}{U}
 * Look at the top X cards of your library, where X is the number of lands you control.
 * Put one of those cards into your hand. If this spell was kicked, put two of those cards
 * into your hand instead. Put the rest on the bottom of your library in a random order.
 */
val ConsultTheStarCharts = card("Consult the Star Charts") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Kicker {1}{U} (You may pay an additional {1}{U} as you cast this spell.)\n" +
        "Look at the top X cards of your library, where X is the number of lands you control. " +
        "Put one of those cards into your hand. If this spell was kicked, put two of those cards " +
        "into your hand instead. Put the rest on the bottom of your library in a random order."

    keywordAbility(KeywordAbility.kicker("{1}{U}"))

    spell {
        // Unkicked: look at top X (X = lands you control), keep one, rest on bottom in random order.
        effect = LibraryPatterns.lookAtTopAndKeep(
            count = DynamicAmount.AggregateBattlefield(Player.You, GameObjectFilter.Land),
            keepCount = DynamicAmount.Fixed(1),
            keepDestination = CardDestination.ToZone(Zone.HAND),
            restDestination = CardDestination.ToZone(Zone.LIBRARY, placement = ZonePlacement.Bottom),
            restOrder = CardOrder.Random
        )

        // Kicked: keep two instead.
        kickerEffect = LibraryPatterns.lookAtTopAndKeep(
            count = DynamicAmount.AggregateBattlefield(Player.You, GameObjectFilter.Land),
            keepCount = DynamicAmount.Fixed(2),
            keepDestination = CardDestination.ToZone(Zone.HAND),
            restDestination = CardDestination.ToZone(Zone.LIBRARY, placement = ZonePlacement.Bottom),
            restOrder = CardOrder.Random
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "51"
        artist = "Antonio José Manzanedo"
        imageUri = "https://cards.scryfall.io/normal/front/a/1/a16a6555-2e3a-4587-aacd-0307d696b26c.jpg?1752946753"
    }
}

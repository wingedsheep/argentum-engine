package com.wingedsheep.mtg.sets.definitions.tla.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Accumulate Wisdom
 * {1}{U}
 * Instant — Lesson
 *
 * Look at the top three cards of your library. Put one of those cards into your hand and the
 * rest on the bottom of your library in any order. Put each of those cards into your hand
 * instead if there are three or more Lesson cards in your graveyard.
 *
 * The graveyard count is evaluated on resolution. When three or more Lesson cards are present,
 * all three looked-at cards go to hand; otherwise the default keep-one / rest-to-bottom flow
 * applies, with the controller choosing the bottom order.
 */
val AccumulateWisdom = card("Accumulate Wisdom") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Instant — Lesson"
    oracleText = "Look at the top three cards of your library. Put one of those cards into your hand " +
        "and the rest on the bottom of your library in any order. Put each of those cards into your " +
        "hand instead if there are three or more Lesson cards in your graveyard."

    spell {
        effect = ConditionalEffect(
            condition = Conditions.CompareAmounts(
                DynamicAmount.Count(
                    Player.You,
                    Zone.GRAVEYARD,
                    GameObjectFilter.Any.withSubtype(Subtype.LESSON)
                ),
                ComparisonOperator.GTE,
                DynamicAmount.Fixed(3)
            ),
            // Three or more Lesson cards in graveyard: put each of the looked-at cards into hand.
            effect = Patterns.Library.lookAtTopAndKeep(
                count = 3,
                keepCount = 3,
                keepDestination = CardDestination.ToZone(Zone.HAND),
            ),
            // Default: keep one, rest to the bottom of the library in any order.
            elseEffect = Patterns.Library.lookAtTopAndKeep(
                count = DynamicAmount.Fixed(3),
                keepCount = DynamicAmount.Fixed(1),
                keepDestination = CardDestination.ToZone(Zone.HAND),
                restDestination = CardDestination.ToZone(Zone.LIBRARY, placement = ZonePlacement.Bottom),
                restOrder = CardOrder.ControllerChooses,
            ),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "44"
        artist = "Gemi"
        flavorText = "\"To prove your worth as scholars, you have to contribute some worthwhile knowledge.\"\n" +
            "—Wan Shi Tong"
        imageUri = "https://cards.scryfall.io/normal/front/a/6/a6335319-6c92-40d4-ab2d-c06c79049c30.jpg?1764120188"
    }
}

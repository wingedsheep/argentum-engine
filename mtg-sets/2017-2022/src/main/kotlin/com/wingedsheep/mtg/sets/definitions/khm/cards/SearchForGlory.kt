package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.predicates.CardPredicate

/**
 * Search for Glory
 * {2}{W}
 * Snow Sorcery
 * Search your library for a snow permanent card, a legendary card, or a Saga card, reveal it, put
 * it into your hand, then shuffle. You gain 1 life for each {S} spent to cast this spell.
 *
 * "{S} spent" is any mana from a snow source spent on the cost (CR 107.4h), read off the spell's
 * recorded payment by `SnowManaSpent` at resolution. A copy was not cast, so it spent no mana and
 * gains no life, but still searches.
 */
val SearchForGlory = card("Search for Glory") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Snow Sorcery"
    oracleText = "Search your library for a snow permanent card, a legendary card, or a Saga card, " +
        "reveal it, put it into your hand, then shuffle. You gain 1 life for each {S} spent to cast " +
        "this spell. ({S} is mana from a snow source.)"

    spell {
        effect = Patterns.Library.searchLibrary(
            filter = GameObjectFilter(
                cardPredicates = listOf(
                    CardPredicate.Or(
                        listOf(
                            CardPredicate.And(listOf(CardPredicate.IsSnow, CardPredicate.IsPermanent)),
                            CardPredicate.IsLegendary,
                            CardPredicate.HasSubtype(Subtype.SAGA),
                        )
                    )
                )
            ),
            count = 1,
            destination = SearchDestination.HAND,
            reveal = true,
            shuffleAfter = true,
        ) then Effects.GainLife(DynamicAmounts.snowManaSpent())
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "27"
        artist = "Kieran Yanner"
        imageUri = "https://cards.scryfall.io/normal/front/b/6/b65c215d-562d-4c7c-bc9f-b1d741050158.jpg?1783928277"
        ruling("2021-02-05", "If you cast Search for Glory without spending snow mana, you won't gain any life. You'll still perform the search.")
        ruling("2021-02-05", "If Search for Glory is copied, no snow mana was spent to cast the copy, so you won't gain any life. You'll still search . . . for glory.")
    }
}

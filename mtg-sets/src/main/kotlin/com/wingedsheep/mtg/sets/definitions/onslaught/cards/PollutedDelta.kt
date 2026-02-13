package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CardPredicate
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.SearchDestination
import com.wingedsheep.sdk.scripting.SearchLibraryEffect

/**
 * Polluted Delta
 * Land
 * {T}, Pay 1 life, Sacrifice Polluted Delta: Search your library for an Island or Swamp card,
 * put it onto the battlefield, then shuffle.
 */
val PollutedDelta = card("Polluted Delta") {
    typeLine = "Land"
    oracleText = "{T}, Pay 1 life, Sacrifice Polluted Delta: Search your library for an Island or Swamp card, put it onto the battlefield, then shuffle."

    activatedAbility {
        cost = Costs.Composite(Costs.Tap, Costs.PayLife(1), Costs.SacrificeSelf)
        effect = SearchLibraryEffect(
            filter = GameObjectFilter(
                cardPredicates = listOf(
                    CardPredicate.IsLand,
                    CardPredicate.Or(
                        listOf(
                            CardPredicate.HasSubtype(Subtype("Island")),
                            CardPredicate.HasSubtype(Subtype("Swamp"))
                        )
                    )
                )
            ),
            destination = SearchDestination.BATTLEFIELD,
            entersTapped = false,
            shuffleAfter = true
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "321"
        artist = "Rob Alexander"
        imageUri = "https://cards.scryfall.io/large/front/0/f/0f7585c8-9e21-4eef-afc1-2852de23db2f.jpg?1562898596"
    }
}

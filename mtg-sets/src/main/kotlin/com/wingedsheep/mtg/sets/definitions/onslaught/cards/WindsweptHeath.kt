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
 * Windswept Heath
 * Land
 * {T}, Pay 1 life, Sacrifice Windswept Heath: Search your library for a Forest or Plains card,
 * put it onto the battlefield, then shuffle.
 */
val WindsweptHeath = card("Windswept Heath") {
    typeLine = "Land"
    oracleText = "{T}, Pay 1 life, Sacrifice Windswept Heath: Search your library for a Forest or Plains card, put it onto the battlefield, then shuffle."

    activatedAbility {
        cost = Costs.Composite(Costs.Tap, Costs.PayLife(1), Costs.SacrificeSelf)
        effect = SearchLibraryEffect(
            filter = GameObjectFilter(
                cardPredicates = listOf(
                    CardPredicate.IsLand,
                    CardPredicate.Or(
                        listOf(
                            CardPredicate.HasSubtype(Subtype("Forest")),
                            CardPredicate.HasSubtype(Subtype("Plains"))
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
        collectorNumber = "328"
        artist = "Anthony S. Waters"
        imageUri = "https://cards.scryfall.io/large/front/e/0/e0b28950-1515-4f9e-a155-22c2e9248c87.jpg?1562948672"
    }
}

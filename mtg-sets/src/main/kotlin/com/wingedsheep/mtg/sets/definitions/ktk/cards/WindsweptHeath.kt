package com.wingedsheep.mtg.sets.definitions.ktk.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.dsl.LibraryPatterns
/**
 * Windswept Heath
 * Land
 * {T}, Pay 1 life, Sacrifice Windswept Heath: Search your library for a Forest or Plains card,
 * put it onto the battlefield, then shuffle.
 */
val WindsweptHeath = card("Windswept Heath") {
    typeLine = "Land"
    colorIdentity = ""
    oracleText = "{T}, Pay 1 life, Sacrifice Windswept Heath: Search your library for a Forest or Plains card, put it onto the battlefield, then shuffle."

    activatedAbility {
        cost = Costs.Composite(Costs.Tap, Costs.PayLife(1), Costs.SacrificeSelf)
        effect = LibraryPatterns.searchLibrary(
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
        collectorNumber = "248"
        artist = "Yeong-Hao Han"
        flavorText = "Where dragons once roared, their bones now keen."
        imageUri = "https://cards.scryfall.io/normal/front/e/7/e7b28650-cddc-4878-b1d1-b5a764f4df49.jpg?1707235002"
    }
}

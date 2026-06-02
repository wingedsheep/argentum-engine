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
 * Wooded Foothills
 * Land
 * {T}, Pay 1 life, Sacrifice Wooded Foothills: Search your library for a Mountain or Forest card,
 * put it onto the battlefield, then shuffle.
 */
val WoodedFoothills = card("Wooded Foothills") {
    typeLine = "Land"
    colorIdentity = ""
    oracleText = "{T}, Pay 1 life, Sacrifice Wooded Foothills: Search your library for a Mountain or Forest card, put it onto the battlefield, then shuffle."

    activatedAbility {
        cost = Costs.Composite(Costs.Tap, Costs.PayLife(1), Costs.SacrificeSelf)
        effect = LibraryPatterns.searchLibrary(
            filter = GameObjectFilter(
                cardPredicates = listOf(
                    CardPredicate.IsLand,
                    CardPredicate.Or(
                        listOf(
                            CardPredicate.HasSubtype(Subtype("Mountain")),
                            CardPredicate.HasSubtype(Subtype("Forest"))
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
        collectorNumber = "249"
        artist = "Jonas De Ro"
        flavorText = "Where dragons' breath once burned, their bones now freeze."
        imageUri = "https://cards.scryfall.io/normal/front/a/8/a8503cca-7e7d-44c4-8587-81376b396398.jpg?1707234996"
    }
}

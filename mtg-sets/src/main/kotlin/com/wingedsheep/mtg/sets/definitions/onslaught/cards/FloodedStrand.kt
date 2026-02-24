package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.dsl.Effects

/**
 * Flooded Strand
 * Land
 * {T}, Pay 1 life, Sacrifice Flooded Strand: Search your library for a Plains or Island card,
 * put it onto the battlefield, then shuffle.
 */
val FloodedStrand = card("Flooded Strand") {
    typeLine = "Land"
    oracleText = "{T}, Pay 1 life, Sacrifice Flooded Strand: Search your library for a Plains or Island card, put it onto the battlefield, then shuffle."

    activatedAbility {
        cost = Costs.Composite(Costs.Tap, Costs.PayLife(1), Costs.SacrificeSelf)
        effect = Effects.SearchLibrary(
            filter = GameObjectFilter(
                cardPredicates = listOf(
                    CardPredicate.IsLand,
                    CardPredicate.Or(
                        listOf(
                            CardPredicate.HasSubtype(Subtype("Plains")),
                            CardPredicate.HasSubtype(Subtype("Island"))
                        )
                    )
                )
            ),
            destination = SearchDestination.BATTLEFIELD,
            entersTapped = false,
            shuffle = true
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "316"
        artist = "Rob Alexander"
        imageUri = "https://cards.scryfall.io/normal/front/b/4/b4e3d844-d3b4-41d8-921d-c1cb3af343f8.jpg?1562929608"
    }
}

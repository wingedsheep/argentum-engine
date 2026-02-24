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
        effect = Effects.SearchLibrary(
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
            shuffle = true
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "328"
        artist = "Anthony S. Waters"
        imageUri = "https://cards.scryfall.io/normal/front/7/a/7a7c5941-9c8a-4a40-9efb-a84f05c58e53.jpg?1562923899"
    }
}

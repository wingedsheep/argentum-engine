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
 * Wooded Foothills
 * Land
 * {T}, Pay 1 life, Sacrifice Wooded Foothills: Search your library for a Mountain or Forest card,
 * put it onto the battlefield, then shuffle.
 */
val WoodedFoothills = card("Wooded Foothills") {
    typeLine = "Land"
    oracleText = "{T}, Pay 1 life, Sacrifice Wooded Foothills: Search your library for a Mountain or Forest card, put it onto the battlefield, then shuffle."

    activatedAbility {
        cost = Costs.Composite(Costs.Tap, Costs.PayLife(1), Costs.SacrificeSelf)
        effect = Effects.SearchLibrary(
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
            shuffle = true
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "330"
        artist = "Rob Alexander"
        imageUri = "https://cards.scryfall.io/normal/front/c/d/cdad38f7-9dfa-4f1b-9fac-41ab2b253f53.jpg?1562948672"
    }
}

package com.wingedsheep.mtg.sets.definitions.khans.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.dsl.EffectPatterns

/**
 * Bloodstained Mire
 * Land
 * {T}, Pay 1 life, Sacrifice Bloodstained Mire: Search your library for a Swamp or Mountain card,
 * put it onto the battlefield, then shuffle.
 */
val BloodstainedMire = card("Bloodstained Mire") {
    typeLine = "Land"
    oracleText = "{T}, Pay 1 life, Sacrifice Bloodstained Mire: Search your library for a Swamp or Mountain card, put it onto the battlefield, then shuffle."

    activatedAbility {
        cost = Costs.Composite(Costs.Tap, Costs.PayLife(1), Costs.SacrificeSelf)
        effect = EffectPatterns.searchLibrary(
            filter = GameObjectFilter(
                cardPredicates = listOf(
                    CardPredicate.IsLand,
                    CardPredicate.Or(
                        listOf(
                            CardPredicate.HasSubtype(Subtype("Swamp")),
                            CardPredicate.HasSubtype(Subtype("Mountain"))
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
        collectorNumber = "230"
        artist = "Daarken"
        flavorText = "Where dragons once triumphed, their bones now molder."
        imageUri = "https://cards.scryfall.io/normal/front/7/f/7f430794-0d86-4f6a-97e0-4bbb6716d613.jpg?1707235037"
    }
}

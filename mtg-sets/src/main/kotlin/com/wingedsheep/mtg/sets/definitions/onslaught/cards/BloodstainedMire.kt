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
        effect = SearchLibraryEffect(
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
        collectorNumber = "313"
        artist = "Rob Alexander"
        imageUri = "https://cards.scryfall.io/large/front/6/8/68c72226-6f52-4322-8b14-18737293dfa0.jpg?1562919681"
    }
}

package com.wingedsheep.mtg.sets.definitions.aer.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * Renegade Map
 * {1}
 * Artifact
 * This artifact enters tapped.
 * {T}, Sacrifice this artifact: Search your library for a basic land card, reveal it, put it into your hand, then shuffle.
 */
val RenegadeMap = card("Renegade Map") {
    manaCost = "{1}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "This artifact enters tapped.\n" +
        "{T}, Sacrifice this artifact: Search your library for a basic land card, reveal it, put it into your hand, then shuffle."

    replacementEffect(EntersTapped())

    activatedAbility {
        cost = Costs.Composite(Costs.Tap, Costs.SacrificeSelf)
        effect = Patterns.Library.searchLibrary(
            filter = GameObjectFilter.BasicLand,
            destination = SearchDestination.HAND,
            reveal = true
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "173"
        artist = "Lake Hurwitz"
        flavorText = "\"We have safe houses in every zone, but you'll never find one without this.\""
        imageUri = "https://cards.scryfall.io/normal/front/a/c/ac063445-d0e2-4015-96dc-97098433f30a.jpg"
    }
}

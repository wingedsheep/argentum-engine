package com.wingedsheep.mtg.sets.definitions.rix.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * Thunderherd Migration
 * {1}{G}
 * Sorcery
 * As an additional cost to cast this spell, reveal a Dinosaur card from your hand or pay {1}.
 * Search your library for a basic land card, put it onto the battlefield tapped, then shuffle.
 */
val ThunderherdMigration = card("Thunderherd Migration") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "As an additional cost to cast this spell, reveal a Dinosaur card from your hand or pay {1}.\n" +
        "Search your library for a basic land card, put it onto the battlefield tapped, then shuffle."

    additionalCost(
        Costs.additional.RevealFromHandOrPay(
            filter = GameObjectFilter.Any.withSubtype(Subtype.DINOSAUR),
            alternativeManaCost = "{1}"
        )
    )

    spell {
        effect = Patterns.Library.searchLibrary(
            filter = Filters.BasicLand,
            destination = SearchDestination.BATTLEFIELD,
            entersTapped = true
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "149"
        artist = "Lars Grant-West"
        imageUri = "https://cards.scryfall.io/normal/front/c/5/c56de4a3-f5ab-469e-ab66-b8187c8c04a0.jpg?1783935278"
    }
}

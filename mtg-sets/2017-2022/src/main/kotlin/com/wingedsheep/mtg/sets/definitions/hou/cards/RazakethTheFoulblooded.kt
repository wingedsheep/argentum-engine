package com.wingedsheep.mtg.sets.definitions.hou.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * Razaketh, the Foulblooded — Hour of Devastation #73 (canonical printing)
 * {5}{B}{B}{B} · Legendary Creature — Demon 8/8
 *
 * Flying, trample
 * Pay 2 life, Sacrifice another creature: Search your library for a card, put that card into your
 * hand, then shuffle.
 */
val RazakethTheFoulblooded = card("Razaketh, the Foulblooded") {
    manaCost = "{5}{B}{B}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Creature — Demon"
    power = 8
    toughness = 8
    oracleText = "Flying, trample\n" +
        "Pay 2 life, Sacrifice another creature: Search your library for a card, put that card into your " +
        "hand, then shuffle."

    keywords(Keyword.FLYING, Keyword.TRAMPLE)

    activatedAbility {
        cost = Costs.Composite(Costs.PayLife(2), Costs.SacrificeAnother(GameObjectFilter.Creature))
        effect = Patterns.Library.searchLibrary(filter = GameObjectFilter.Any, destination = SearchDestination.HAND)
        description = "Pay 2 life, Sacrifice another creature: Search your library for a card, put that card " +
            "into your hand, then shuffle."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "73"
        artist = "Chris Rallis"
        flavorText = "Liliana insisted on coming to Amonkhet to confront one of the two remaining demons to " +
            "whom she is bound."
        imageUri = "https://cards.scryfall.io/normal/front/e/1/e14adff9-33cc-467e-b782-068854c5e7b7.jpg?1783936036"
    }
}

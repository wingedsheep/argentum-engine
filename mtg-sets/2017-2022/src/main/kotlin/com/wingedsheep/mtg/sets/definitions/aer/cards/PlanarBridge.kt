package com.wingedsheep.mtg.sets.definitions.aer.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * Planar Bridge
 * {6}
 * Legendary Artifact
 * {8}, {T}: Search your library for a permanent card, put it onto the battlefield, then shuffle.
 */
val PlanarBridge = card("Planar Bridge") {
    manaCost = "{6}"
    colorIdentity = ""
    typeLine = "Legendary Artifact"
    oracleText = "{8}, {T}: Search your library for a permanent card, put it onto the battlefield, then shuffle."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{8}"), Costs.Tap)
        effect = Patterns.Library.searchLibrary(
            filter = GameObjectFilter.Permanent,
            destination = SearchDestination.BATTLEFIELD
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "171"
        artist = "Chase Stone"
        flavorText = "\"It is finished. Now the real work can begin.\"\n—Tezzeret"
        imageUri = "https://cards.scryfall.io/normal/front/9/9/991397cf-5c7d-4e8a-8f46-8e2e0ed29eff.jpg"
        ruling("2017-02-09", "A permanent card is an artifact, creature, enchantment, land, or planeswalker card.")
    }
}

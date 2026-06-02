package com.wingedsheep.mtg.sets.definitions.blc.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.dsl.LibraryPatterns

/**
 * Burnished Hart
 * {3}
 * Artifact Creature — Elk
 * 2/2
 * {3}, Sacrifice this creature: Search your library for up to two basic land cards,
 * put them onto the battlefield tapped, then shuffle.
 */
val BurnishedHart = card("Burnished Hart") {
    manaCost = "{3}"
    colorIdentity = ""
    typeLine = "Artifact Creature — Elk"
    power = 2
    toughness = 2
    oracleText = "{3}, Sacrifice this creature: Search your library for up to two basic land cards, put them onto the battlefield tapped, then shuffle."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{3}"), Costs.SacrificeSelf)
        effect = LibraryPatterns.searchLibrary(
            filter = GameObjectFilter.BasicLand,
            count = 2,
            destination = SearchDestination.BATTLEFIELD,
            entersTapped = true,
            shuffleAfter = true
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "266"
        artist = "Jakub Kasper"
        flavorText = "Not born of nature, but deeply connected to it nonetheless."
        imageUri = "https://cards.scryfall.io/normal/front/2/a/2a7f80b8-e634-4770-b6d1-bdc05e4cbb69.jpg?1721429541"
    }
}

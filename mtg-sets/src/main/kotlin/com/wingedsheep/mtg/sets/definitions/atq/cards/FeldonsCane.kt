package com.wingedsheep.mtg.sets.definitions.atq.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Feldon's Cane
 * {1}
 * Artifact
 * {T}, Exile this artifact: Shuffle your graveyard into your library.
 *
 * Tap + exile-self activated cost; the effect shuffles the controller's graveyard
 * back into their library via the standard library pattern.
 */
val FeldonsCane = card("Feldon's Cane") {
    manaCost = "{1}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "{T}, Exile this artifact: Shuffle your graveyard into your library."

    activatedAbility {
        cost = Costs.Composite(Costs.Tap, Costs.ExileSelf)
        effect = Patterns.Library.shuffleGraveyardIntoLibrary(EffectTarget.Controller)
        description = "{T}, Exile this artifact: Shuffle your graveyard into your library."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "50"
        artist = "Mark Tedin"
        flavorText = "Feldon found the first of these canes frozen in the Ronom Glacier."
        imageUri = "https://cards.scryfall.io/normal/front/b/b/bb6af436-bcfd-4d47-a1aa-e84b587a725a.jpg?1562934632"
    }
}

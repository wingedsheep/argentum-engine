package com.wingedsheep.mtg.sets.definitions.atq.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Ashnod's Altar
 * {3}
 * Artifact
 * Sacrifice a creature: Add {C}{C}.
 */
val AshnodsAltar = card("Ashnod's Altar") {
    manaCost = "{3}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "Sacrifice a creature: Add {C}{C}."

    activatedAbility {
        manaAbility = true
        cost = Costs.Sacrifice(GameObjectFilter.Creature)
        effect = Effects.AddColorlessMana(2)
        description = "Sacrifice a creature: Add {C}{C}."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "38"
        artist = "Anson Maddocks"
        imageUri = "https://cards.scryfall.io/normal/front/c/d/cdcccb0f-ce96-453b-9e82-41d87f52e58b.jpg?1562938535"
    }
}

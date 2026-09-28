package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Dreg Recycler {1}{B}
 * Creature — Phyrexian Beast
 * 2/2
 * {T}, Sacrifice an artifact or creature: Each opponent loses 1 life and you gain 1 life.
 */
val DregRecycler = card("Dreg Recycler") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Phyrexian Beast"
    power = 2
    toughness = 2
    oracleText = "{T}, Sacrifice an artifact or creature: Each opponent loses 1 life and you gain 1 life."

    activatedAbility {
        cost = Costs.Composite(
            Costs.Tap,
            Costs.Sacrifice(GameObjectFilter.Artifact or GameObjectFilter.Creature)
        )
        effect = Effects.DrainLife(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "100"
        artist = "Campbell White"
        flavorText = "Bodies too damaged to survive phyresis are collected and broken down into useful raw materials."
        imageUri = "https://cards.scryfall.io/normal/front/4/e/4ef82b33-82ba-4521-846b-e651764ef46d.jpg?1783917016"
    }
}

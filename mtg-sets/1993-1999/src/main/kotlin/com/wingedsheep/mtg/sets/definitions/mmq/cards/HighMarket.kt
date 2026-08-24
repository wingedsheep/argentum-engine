package com.wingedsheep.mtg.sets.definitions.mmq.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * High Market
 *
 * Land
 *
 * {T}: Add {C}.
 * {T}, Sacrifice a creature: You gain 1 life.
 *
 * The sacrifice outlet is the whole point of the card, so the second ability's cost is
 * `{T}` plus [Costs.Sacrifice] over [GameObjectFilter.Creature]; the life gain is the
 * controller's by default.
 */
val HighMarket = card("High Market") {
    colorIdentity = ""
    typeLine = "Land"
    oracleText = "{T}: Add {C}.\n" +
        "{T}, Sacrifice a creature: You gain 1 life."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
    }

    activatedAbility {
        cost = Costs.Composite(
            Costs.Tap,
            Costs.Sacrifice(GameObjectFilter.Creature)
        )
        effect = Effects.GainLife(1)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "320"
        artist = "Carl Critchlow"
        flavorText = "If it can't be had here, it can't be had on any world."
        imageUri = "https://cards.scryfall.io/normal/front/f/4/f4c58683-65a6-4df9-8952-458e397b1374.jpg"
    }
}

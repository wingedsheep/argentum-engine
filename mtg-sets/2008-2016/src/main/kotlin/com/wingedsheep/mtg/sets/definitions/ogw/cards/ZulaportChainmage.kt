package com.wingedsheep.mtg.sets.definitions.ogw.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Zulaport Chainmage
 * {3}{B}
 * Creature — Human Shaman Ally
 * 4/2
 * Cohort — {T}, Tap an untapped Ally you control: Target opponent loses 2 life.
 */
val ZulaportChainmage = card("Zulaport Chainmage") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Shaman Ally"
    power = 4
    toughness = 2
    oracleText = "Cohort — {T}, Tap an untapped Ally you control: Target opponent loses 2 life."

    activatedAbility {
        cost = Costs.Composite(
            Costs.Tap,
            Costs.TapPermanents(1, GameObjectFilter.Permanent.withSubtype(Subtype.ALLY)),
        )
        val opponent = target(Targets.Opponent)
        effect = Effects.LoseLife(2, opponent)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "93"
        artist = "Chris Rallis"
        flavorText = "The chains obey her. Everything else would do best to get out of her way."
        imageUri = "https://cards.scryfall.io/normal/front/9/2/92461f3c-21dc-48a8-b532-9c4e2e1e80b5.jpg"
    }
}

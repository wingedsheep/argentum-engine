package com.wingedsheep.mtg.sets.definitions.xln.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Blight Keeper
 * {B}
 * Creature — Bat Imp
 * 1/1
 *
 * Flying
 * {7}{B}, {T}, Sacrifice this creature: Target opponent loses 4 life and you gain 4 life.
 */
val BlightKeeper = card("Blight Keeper") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Creature — Bat Imp"
    oracleText = "Flying\n{7}{B}, {T}, Sacrifice this creature: Target opponent loses 4 life and you gain 4 life."
    power = 1
    toughness = 1

    keywords(Keyword.FLYING)

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{7}{B}"), Costs.Tap, Costs.SacrificeSelf)
        val opponent = target(Targets.Opponent)
        effect = Effects.LoseLife(4, opponent) then Effects.GainLife(4)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "92"
        artist = "Ben Wootten"
        flavorText = "It withers fruit and flesh alike."
        imageUri = "https://cards.scryfall.io/normal/front/3/b/3bcabe2d-82d2-4c1b-8f28-21dc29c9dbf2.jpg?1783935767"
    }
}

package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Copper Longlegs
 * {1}{G}
 * Creature — Phyrexian Spider
 * 1/3
 *
 * Reach
 * {1}{G}, Sacrifice this creature: Proliferate.
 */
val CopperLonglegs = card("Copper Longlegs") {
    manaCost = "{1}{G}"
    typeLine = "Creature — Phyrexian Spider"
    power = 1
    toughness = 3
    oracleText = "Reach\n{1}{G}, Sacrifice this creature: Proliferate. " +
        "(Choose any number of permanents and/or players, then give each another counter of each kind already there.)"

    keywords(Keyword.REACH)

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}{G}"), Costs.SacrificeSelf)
        effect = Effects.Proliferate()
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "165"
        artist = "Nicholas Gregory"
        flavorText = "Its webs are not true silk, but woven from thousands of delicate mycosynth strands."
        imageUri = "https://cards.scryfall.io/normal/front/f/8/f8855fbf-4f1e-4c44-9653-bbbfc3f2fafd.jpg?1783918018"
    }
}

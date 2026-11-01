package com.wingedsheep.mtg.sets.definitions.rna.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Gyre Engineer — Ravnica Allegiance #180
 * {1}{G}{U} · Creature — Vedalken Wizard · 1 / 1
 */
val GyreEngineer = card("Gyre Engineer") {
    manaCost = "{1}{G}{U}"
    colorIdentity = "GU"
    typeLine = "Creature — Vedalken Wizard"
    power = 1
    toughness = 1
    oracleText = "{T}: Add {G}{U}."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.GREEN) then Effects.AddMana(Color.BLUE)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "180"
        artist = "Anthony Palumbo"
        flavorText = "\"As I contemplate what is, I dive ever deeper into the depths of possibility. " +
            "Then I set an experiment in motion and watch the truth rise to the surface.\""
        imageUri = "https://cards.scryfall.io/normal/front/2/6/26dd6a1d-4dcb-4392-9856-c0e4140efbd7.jpg?1783933647"
    }
}

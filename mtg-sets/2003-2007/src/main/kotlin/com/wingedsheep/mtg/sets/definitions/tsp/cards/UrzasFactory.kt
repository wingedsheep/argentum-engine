package com.wingedsheep.mtg.sets.definitions.tsp.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Urza's Factory
 * Land — Urza's
 * {T}: Add {C}.
 * {7}, {T}: Create a 2/2 colorless Assembly-Worker artifact creature token.
 */
val UrzasFactory = card("Urza's Factory") {
    manaCost = ""
    colorIdentity = ""
    typeLine = "Land — Urza's"
    oracleText = "{T}: Add {C}.\n{7}, {T}: Create a 2/2 colorless Assembly-Worker artifact creature token."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{7}"),
            Costs.Tap
        )
        effect = Effects.CreateToken(
            power = 2,
            toughness = 2,
            colors = setOf(),
            creatureTypes = setOf("Assembly-Worker"),
            artifactToken = true
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "280"
        artist = "Mark Tedin"
        flavorText = "\"Though their ideals are leagues apart, Urza's and Mishra's creations have a surprising harmony with one another.\"\n—Tocasia, journal entry"
        imageUri = "https://cards.scryfall.io/normal/front/b/2/b22d97f3-08cf-4c19-bbe2-5a36096187cd.jpg"
    }
}

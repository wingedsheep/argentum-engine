package com.wingedsheep.mtg.sets.definitions.blc.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Thran Dynamo
 * {4}
 * Artifact
 *
 * {T}: Add {C}{C}{C}.
 */
val ThranDynamo = card("Thran Dynamo") {
    manaCost = "{4}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "{T}: Add {C}{C}{C}."

    activatedAbility {
        cost = AbilityCost.Tap
        effect = Effects.AddColorlessMana(3)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "290"
        artist = "Ron Spears"
        flavorText = "Urza's metathran children were conceived, birthed, and nurtured by an integrated system of machines."
        imageUri = "https://cards.scryfall.io/normal/front/6/4/6455b185-aa5e-45e1-951e-f6eef517daf2.jpg?1721429660"
    }
}

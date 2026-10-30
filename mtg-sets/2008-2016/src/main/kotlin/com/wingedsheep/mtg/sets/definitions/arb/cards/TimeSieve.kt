package com.wingedsheep.mtg.sets.definitions.arb.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Time Sieve
 * {U}{B}
 * Artifact
 *
 * {T}, Sacrifice five artifacts: Take an extra turn after this one.
 *
 * Time Sieve itself may be one of the five artifacts (2020-08-07 ruling), so the sacrifice
 * filter does not exclude the source.
 */
val TimeSieve = card("Time Sieve") {
    manaCost = "{U}{B}"
    colorIdentity = "UB"
    typeLine = "Artifact"
    oracleText = "{T}, Sacrifice five artifacts: Take an extra turn after this one."

    activatedAbility {
        cost = Costs.Composite(Costs.Tap, Costs.SacrificeMultiple(5, GameObjectFilter.Artifact))
        effect = Effects.TakeExtraTurn()
        description = "{T}, Sacrifice five artifacts: Take an extra turn after this one."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "31"
        artist = "Franz Vohwinkel"
        flavorText = "\"I pray that I am never considered useless or old.\"\n—Sharuum the Hegemon"
        imageUri = "https://cards.scryfall.io/normal/front/c/6/c620e565-f5e1-467c-a1d1-8d05228b2f37.jpg?1783942436"

        ruling("2020-08-07", "You may sacrifice Time Sieve as one of the artifacts to pay the cost of its own ability.")
    }
}

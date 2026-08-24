package com.wingedsheep.mtg.sets.definitions.fem.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersTapped

/**
 * Ebon Stronghold
 * Land
 * This land enters tapped.
 * {T}: Add {B}.
 * {T}, Sacrifice this land: Add {B}{B}.
 *
 * The black member of the sacrifice-land cycle — see [DwarvenRuins].
 */
val EbonStronghold = card("Ebon Stronghold") {
    colorIdentity = "B"
    typeLine = "Land"
    oracleText = "This land enters tapped.\n{T}: Add {B}.\n{T}, Sacrifice this land: Add {B}{B}."

    replacementEffect(EntersTapped())

    activatedAbility {
        cost = Costs.Tap
        manaAbility = true
        effect = Effects.AddMana(Color.BLACK)
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Tap, Costs.SacrificeSelf)
        manaAbility = true
        effect = Effects.AddMana(Color.BLACK, 2)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "95"
        artist = "Mark Poole"
        imageUri = "https://cards.scryfall.io/normal/front/3/f/3fb2a11f-a8e4-4acf-871a-11171e3304ef.jpg?1783947879"
    }
}

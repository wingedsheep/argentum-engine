package com.wingedsheep.mtg.sets.definitions.fem.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersTapped

/**
 * Svyelunite Temple
 * Land
 * This land enters tapped.
 * {T}: Add {U}.
 * {T}, Sacrifice this land: Add {U}{U}.
 *
 * The blue member of the sacrifice-land cycle — see [DwarvenRuins].
 */
val SvyeluniteTemple = card("Svyelunite Temple") {
    colorIdentity = "U"
    typeLine = "Land"
    oracleText = "This land enters tapped.\n{T}: Add {U}.\n{T}, Sacrifice this land: Add {U}{U}."

    replacementEffect(EntersTapped())

    activatedAbility {
        cost = Costs.Tap
        manaAbility = true
        effect = Effects.AddMana(Color.BLUE)
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Tap, Costs.SacrificeSelf)
        manaAbility = true
        effect = Effects.AddMana(Color.BLUE, 2)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "102"
        artist = "Mark Poole"
        imageUri = "https://cards.scryfall.io/normal/front/8/b/8b3fde62-ab21-459b-9c5d-01aa6fe1d08e.jpg?1783947878"
    }
}

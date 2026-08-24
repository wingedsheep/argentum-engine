package com.wingedsheep.mtg.sets.definitions.fem.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersTapped

/**
 * Ruins of Trokair
 * Land
 * This land enters tapped.
 * {T}: Add {W}.
 * {T}, Sacrifice this land: Add {W}{W}.
 *
 * The white member of the sacrifice-land cycle — see [DwarvenRuins].
 */
val RuinsOfTrokair = card("Ruins of Trokair") {
    colorIdentity = "W"
    typeLine = "Land"
    oracleText = "This land enters tapped.\n{T}: Add {W}.\n{T}, Sacrifice this land: Add {W}{W}."

    replacementEffect(EntersTapped())

    activatedAbility {
        cost = Costs.Tap
        manaAbility = true
        effect = Effects.AddMana(Color.WHITE)
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Tap, Costs.SacrificeSelf)
        manaAbility = true
        effect = Effects.AddMana(Color.WHITE, 2)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "100"
        artist = "Mark Poole"
        imageUri = "https://cards.scryfall.io/normal/front/4/c/4ce2e734-8cff-4bfe-85f8-17b3e1903f18.jpg?1783947877"
    }
}

package com.wingedsheep.mtg.sets.definitions.fem.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Basal Thrull
 * {B}{B}
 * Creature — Thrull
 * 1/2
 * {T}, Sacrifice this creature: Add {B}{B}.
 */
val BasalThrull = card("Basal Thrull") {
    manaCost = "{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Thrull"
    oracleText = "{T}, Sacrifice this creature: Add {B}{B}."
    power = 1
    toughness = 2

    activatedAbility {
        cost = Costs.Composite(Costs.Tap, Costs.SacrificeSelf)
        manaAbility = true
        effect = Effects.AddMana(Color.BLACK, 2)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "34a"
        artist = "Kaja Foglio"
        flavorText = "Initially bred for sacrifice, the Thrulls eventually turned on their masters, the Order of the Ebon Hand, with gruesome results.\n—*Sarpadian Empires, vol. II*"
        imageUri = "https://cards.scryfall.io/normal/front/0/c/0c1d5d13-0160-48cb-8fac-dd86102569b4.jpg?1783947904"
    }
}

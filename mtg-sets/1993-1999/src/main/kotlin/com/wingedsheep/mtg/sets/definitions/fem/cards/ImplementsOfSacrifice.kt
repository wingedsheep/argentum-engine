package com.wingedsheep.mtg.sets.definitions.fem.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Implements of Sacrifice
 * {2}
 * Artifact
 * {1}, {T}, Sacrifice this artifact: Add two mana of any one color.
 *
 * "Two mana of any one color" — one colour choice, two mana of it, not two independent choices.
 */
val ImplementsOfSacrifice = card("Implements of Sacrifice") {
    manaCost = "{2}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "{1}, {T}, Sacrifice this artifact: Add two mana of any one color."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}"), Costs.Tap, Costs.SacrificeSelf)
        manaAbility = true
        effect = Effects.AddAnyColorMana(2)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "88"
        artist = "Margaret Organ-Kean"
        flavorText = "Relics of the Order of the Ebon Hand, the bowl and dagger bespeak the hideous cruelty of its rituals."
        imageUri = "https://cards.scryfall.io/normal/front/a/a/aa5deb95-79a6-4398-b82a-c1df169550d9.jpg?1783947881"
    }
}

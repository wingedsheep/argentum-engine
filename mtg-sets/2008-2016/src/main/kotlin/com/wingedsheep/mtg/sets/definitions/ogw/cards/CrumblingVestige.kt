package com.wingedsheep.mtg.sets.definitions.ogw.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersTapped

/**
 * Crumbling Vestige
 * Land
 * This land enters tapped.
 * When this land enters, add one mana of any color.
 * {T}: Add {C}. ({C} represents colorless mana.)
 */
val CrumblingVestige = card("Crumbling Vestige") {
    colorIdentity = ""
    typeLine = "Land"
    oracleText = "This land enters tapped.\n" +
        "When this land enters, add one mana of any color.\n" +
        "{T}: Add {C}. ({C} represents colorless mana.)"

    replacementEffect(EntersTapped())

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.AddAnyColorMana(1)
    }

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "170"
        artist = "Zack Stella"
        imageUri = "https://cards.scryfall.io/normal/front/d/4/d491c13c-43e3-4ca3-b888-4edd34dfe14a.jpg"
    }
}

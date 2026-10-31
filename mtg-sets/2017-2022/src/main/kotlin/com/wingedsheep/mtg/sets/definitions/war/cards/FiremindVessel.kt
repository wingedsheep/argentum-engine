package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Firemind Vessel
 * {4}
 * Artifact
 *
 * This artifact enters tapped.
 * {T}: Add two mana of different colors.
 */
val FiremindVessel = card("Firemind Vessel") {
    manaCost = "{4}"
    typeLine = "Artifact"
    oracleText = "This artifact enters tapped.\n{T}: Add two mana of different colors."

    replacementEffect(EntersTapped())

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddManaOfDifferentColors(2)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "237"
        artist = "Ravenna Tran"
        flavorText = "The city mourned Niv-Mizzet's death, but he left behind the components for his rebirth."
        imageUri = "https://cards.scryfall.io/normal/front/e/c/ec0ebb6b-d35a-47db-8071-7fef2a46c17b.jpg?1783933373"
    }
}

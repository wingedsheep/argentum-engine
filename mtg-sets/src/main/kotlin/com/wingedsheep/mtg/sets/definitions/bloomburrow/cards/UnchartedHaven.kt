package com.wingedsheep.mtg.sets.definitions.bloomburrow.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithColorChoice
import com.wingedsheep.sdk.scripting.EntersTapped

/**
 * Uncharted Haven
 * Land
 *
 * This land enters tapped. As it enters, choose a color.
 * {T}: Add one mana of the chosen color.
 */
val UnchartedHaven = card("Uncharted Haven") {
    typeLine = "Land"
    oracleText = "This land enters tapped.\nAs it enters, choose a color.\n{T}: Add one mana of the chosen color."

    replacementEffect(EntersTapped())
    replacementEffect(EntersWithColorChoice())

    // {T}: Add one mana of the chosen color
    // Modeled as AddAnyColorMana — the player picks the color when tapping
    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddAnyColorMana(1)
        manaAbility = true
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "261"
        artist = "Adam Paquette"
        flavorText = "\"There are lands beyond Valley. Wild, calamity-scarred, and beautiful.\"\n—Farfeather, birdfolk scout"
        imageUri = "https://cards.scryfall.io/normal/front/6/8/68b90f54-d629-4126-82cc-13b51d6c1c3e.jpg?1722073883"
    }
}

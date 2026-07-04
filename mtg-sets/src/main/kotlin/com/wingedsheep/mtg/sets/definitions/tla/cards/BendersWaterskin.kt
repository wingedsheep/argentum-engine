package com.wingedsheep.mtg.sets.definitions.tla.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.UntapSelfDuringOtherUntapSteps

/**
 * Bender's Waterskin
 * {3}
 * Artifact
 * Untap this artifact during each other player's untap step.
 * {T}: Add one mana of any color.
 */
val BendersWaterskin = card("Bender's Waterskin") {
    manaCost = "{3}"
    typeLine = "Artifact"
    oracleText = "Untap this artifact during each other player's untap step.\n{T}: Add one mana of any color."

    staticAbility {
        ability = UntapSelfDuringOtherUntapSteps
    }

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddAnyColorMana(1)
        manaAbility = true
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "255"
        artist = "Dee Nguyen"
        flavorText = "Katara's waterskin ensured that she would always have water for bending, healing, or drinking."
        imageUri = "https://cards.scryfall.io/normal/front/6/d/6d4a1712-d19e-4475-9614-a0b1af4da610.jpg?1764121885"
    }
}

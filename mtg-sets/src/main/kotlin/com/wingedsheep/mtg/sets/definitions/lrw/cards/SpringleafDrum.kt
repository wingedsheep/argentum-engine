package com.wingedsheep.mtg.sets.definitions.lrw.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Springleaf Drum
 * {1}
 * Artifact
 * {T}, Tap an untapped creature you control: Add one mana of any color.
 */
val SpringleafDrum = card("Springleaf Drum") {
    manaCost = "{1}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "{T}, Tap an untapped creature you control: Add one mana of any color."

    activatedAbility {
        cost = Costs.Composite(Costs.Tap, Costs.TapPermanents(1, GameObjectFilter.Creature))
        effect = Effects.AddAnyColorMana(1)
        manaAbility = true
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "261"
        artist = "Cyril Van Der Haegen"
        flavorText = "After trying mudskippers for an afternoon, Scratch decided that crickcarp made the best noise."
        imageUri = "https://cards.scryfall.io/normal/front/f/a/fa8b09d0-fbd2-4441-9d87-02450412e0db.jpg?1562375414"
    }
}

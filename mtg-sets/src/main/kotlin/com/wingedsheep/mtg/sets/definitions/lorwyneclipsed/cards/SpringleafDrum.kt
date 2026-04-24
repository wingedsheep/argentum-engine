package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

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
    typeLine = "Artifact"
    oracleText = "{T}, Tap an untapped creature you control: Add one mana of any color."

    activatedAbility {
        cost = Costs.Composite(Costs.Tap, Costs.TapPermanents(1, GameObjectFilter.Creature))
        effect = Effects.AddAnyColorMana(1)
        manaAbility = true
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "260"
        artist = "Cory Godbey"
        flavorText = "To attain the preferred timbre for merrow shanties, the kelp leaf must be moistened with fresh water and struck with a fluid wrist."
        imageUri = "https://cards.scryfall.io/normal/front/e/1/e15ab0aa-4059-4923-9816-6f7a9e5b5a18.jpg?1767732925"
    }
}

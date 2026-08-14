package com.wingedsheep.mtg.sets.definitions.spm.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Supportive Parents
 * {2}{G}
 * Creature — Human Citizen
 * 3/3
 * Tap two untapped creatures you control: Add one mana of any color.
 */
val SupportiveParents = card("Supportive Parents") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Human Citizen"
    power = 3
    toughness = 3
    oracleText = "Tap two untapped creatures you control: Add one mana of any color."

    activatedAbility {
        cost = Costs.TapPermanents(2, GameObjectFilter.Creature)
        effect = Effects.AddAnyColorMana(1)
        manaAbility = true
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "119"
        artist = "Kim Sokol"
        flavorText = "\"Te amamos, mijo.\"\n—Rio Morales"
        imageUri = "https://cards.scryfall.io/normal/front/d/3/d3fe8a5b-4166-46cc-b910-71cd1a19ae1b.jpg?1783905321"
    }
}

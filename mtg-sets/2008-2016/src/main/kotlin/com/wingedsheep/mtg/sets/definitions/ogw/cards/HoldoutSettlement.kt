package com.wingedsheep.mtg.sets.definitions.ogw.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Holdout Settlement
 * Land
 * {T}: Add {C}. ({C} represents colorless mana.)
 * {T}, Tap an untapped creature you control: Add one mana of any color.
 */
val HoldoutSettlement = card("Holdout Settlement") {
    colorIdentity = ""
    typeLine = "Land"
    oracleText = "{T}: Add {C}. ({C} represents colorless mana.)\n" +
        "{T}, Tap an untapped creature you control: Add one mana of any color."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Tap, Costs.TapPermanents(1, GameObjectFilter.Creature))
        effect = Effects.AddManaOfChoice()
        manaAbility = true
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "172"
        artist = "Kieran Yanner"
        flavorText = "Even in areas of profound devastation, the hedrons remained intact."
        imageUri = "https://cards.scryfall.io/normal/front/c/f/cf08c317-6f2d-47e3-ab5b-8af73fd3e404.jpg"
    }
}

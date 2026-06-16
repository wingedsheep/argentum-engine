package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ChoiceType
import com.wingedsheep.sdk.scripting.EntersWithChoice
import com.wingedsheep.sdk.scripting.EntersTapped

/**
 * Mirage Mesa
 * Land — Desert
 *
 * This land enters tapped. As it enters, choose a color.
 * {T}: Add one mana of the chosen color.
 */
val MirageMesa = card("Mirage Mesa") {
    typeLine = "Land — Desert"
    colorIdentity = ""
    oracleText = "This land enters tapped. As it enters, choose a color.\n{T}: Add one mana of the chosen color."

    replacementEffect(EntersTapped())
    replacementEffect(EntersWithChoice(ChoiceType.COLOR))

    // {T}: Add one mana of the chosen color
    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddManaOfChosenColor()
        manaAbility = true
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "262"
        artist = "Andrew Mar"
        flavorText = "\"Out there, death and salvation may lie on either side of the same ridge of sand.\"\n—Ertha Jo, frontier mentor"
        imageUri = "https://cards.scryfall.io/normal/front/c/3/c3d2e816-c06d-4c5d-98fe-c350d8cfab27.jpg?1712356348"
    }
}

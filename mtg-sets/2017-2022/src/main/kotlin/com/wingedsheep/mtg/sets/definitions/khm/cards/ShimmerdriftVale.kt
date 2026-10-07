package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ChoiceType
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.EntersWithChoice
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Shimmerdrift Vale
 * Snow Land
 *
 * This land enters tapped.
 * As this land enters, choose a color.
 * {T}: Add one mana of the chosen color.
 */
val ShimmerdriftVale = card("Shimmerdrift Vale") {
    colorIdentity = ""
    typeLine = "Snow Land"
    oracleText = "This land enters tapped.\nAs this land enters, choose a color.\n{T}: Add one mana of the chosen color."

    replacementEffect(EntersTapped())
    replacementEffect(EntersWithChoice(ChoiceType.COLOR))

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddManaOfChosenColor()
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "267"
        artist = "Titus Lunter"
        flavorText = "Reflections from the wind-sculpted snow gleam with every color of the rainbow."
        imageUri = "https://cards.scryfall.io/normal/front/f/0/f09d98db-0176-41a7-b99b-ead29876cdab.jpg?1783928172"
    }
}

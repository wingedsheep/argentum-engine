package com.wingedsheep.mtg.sets.definitions.lci.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.ChoiceType
import com.wingedsheep.sdk.scripting.EntersWithChoice
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Cavern of Souls
 * Land
 * As this land enters, choose a creature type.
 * {T}: Add {C}.
 * {T}: Add one mana of any color. Spend this mana only to cast a creature spell of the chosen
 * type, and that spell can't be countered.
 */
val CavernOfSouls = card("Cavern of Souls") {
    typeLine = "Land"
    colorIdentity = ""
    oracleText =
        "As this land enters, choose a creature type.\n" +
        "{T}: Add {C}.\n" +
        "{T}: Add one mana of any color. Spend this mana only to cast a creature spell of the chosen type, and that spell can't be countered."

    replacementEffect(EntersWithChoice(ChoiceType.CREATURE_TYPE))

    activatedAbility {
        cost = AbilityCost.Tap
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = AbilityCost.Tap
        effect = Effects.AddAnyColorManaSpendOnChosenTypeUncounterable()
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "269"
        artist = "Alayna Danner"
        imageUri = "https://cards.scryfall.io/normal/front/3/a/3aad15a2-8a1b-4460-9b06-e85863081878.jpg?1706884128"
    }
}

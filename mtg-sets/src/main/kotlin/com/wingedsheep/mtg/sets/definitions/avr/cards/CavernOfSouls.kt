package com.wingedsheep.mtg.sets.definitions.avr.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.ChoiceType
import com.wingedsheep.sdk.scripting.EntersWithChoice
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.ManaSpellRider

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
        effect = Effects.AddAnyColorManaSpendOnChosenType(
            creatureOnly = true,
            riders = setOf(ManaSpellRider.MakesSpellUncounterable)
        )
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "226"
        artist = "Cliff Childs"
        imageUri = "https://cards.scryfall.io/normal/front/1/3/1381c8f1-a292-4bdf-b20c-a5c2a169ee84.jpg?1592709638"
        ruling("2023-11-10", "You must choose an existing creature type, such as Human or Warrior. Card types such as artifact can't be chosen.")
        ruling("2023-11-10", "The spell can't be countered if the mana produced by Cavern of Souls is spent to cover any cost of the spell, even an additional cost such as a kicker cost. This is true even if you use the mana to pay an additional cost while casting a spell \"without paying its mana cost.\"")
    }
}

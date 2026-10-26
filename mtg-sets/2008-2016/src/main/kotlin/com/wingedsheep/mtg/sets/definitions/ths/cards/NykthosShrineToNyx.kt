package com.wingedsheep.mtg.sets.definitions.ths.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Nykthos, Shrine to Nyx
 * Legendary Land
 * {T}: Add {C}.
 * {2}, {T}: Choose a color. Add an amount of mana of that color equal to your devotion to that color.
 */
val NykthosShrineToNyx = card("Nykthos, Shrine to Nyx") {
    manaCost = ""
    colorIdentity = ""
    typeLine = "Legendary Land"
    oracleText = "{T}: Add {C}.\n{2}, {T}: Choose a color. Add an amount of mana of that color equal to your devotion to that color. " +
        "(Your devotion to a color is the number of mana symbols of that color in the mana costs of permanents you control.)"
    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }
    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}"), Costs.Tap)
        effect = Effects.AddAnyColorMana(DynamicAmounts.devotionToChosenColor())
        manaAbility = true
        timing = TimingRule.ManaAbility
        description = "{2}, {T}: Choose a color. Add an amount of mana of that color equal to your devotion to that color."
    }
    metadata {
        rarity = Rarity.RARE
        collectorNumber = "223"
        artist = "Jung Park"
        imageUri = "https://cards.scryfall.io/normal/front/8/3/834b27a0-dfd7-4f96-8cde-cacac4b24acc.jpg?1783939713"
    }
}

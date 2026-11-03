package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Starfleet Academy
 * Legendary Land
 *
 * {T}: Add {C}.
 * Federation — {5}, {T}: Choose a color. Add an amount of mana of that color equal to the number of
 * creature types among non-Borg creatures you control.
 *
 * The Federation ability is a mana ability (no target); the color is chosen and the count taken
 * as it resolves, Nykthos-style.
 */
val StarfleetAcademy = card("Starfleet Academy") {
    manaCost = ""
    colorIdentity = ""
    typeLine = "Legendary Land"
    oracleText = "{T}: Add {C}.\nFederation — {5}, {T}: Choose a color. Add an amount of mana of that color " +
        "equal to the number of creature types among non-Borg creatures you control."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{5}"), Costs.Tap)
        effect = Effects.AddAnyColorMana(DynamicAmounts.federation())
        manaAbility = true
        timing = TimingRule.ManaAbility
        description = "Federation — {5}, {T}: Choose a color. Add mana of that color equal to the number of " +
            "creature types among non-Borg creatures you control."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "297"
        artist = "Robin Olausson"
        flavorText = "The Academy trains recruits from dozens of species, from Aamaarazans to Zaranites."
        imageUri = "https://cards.scryfall.io/normal/front/2/d/2da8d3bf-4397-4844-90d6-dd888becc804.jpg?1792508542"
    }
}

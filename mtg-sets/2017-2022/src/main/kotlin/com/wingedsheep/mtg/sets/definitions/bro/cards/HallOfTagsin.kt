package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Hall of Tagsin
 * Land
 * {T}: Add {C}.
 * {1}, {T}: Add one mana of any color.
 * {4}, {T}: Create a tapped Powerstone token.
 */
val HallOfTagsin = card("Hall of Tagsin") {
    colorIdentity = ""
    typeLine = "Land"
    oracleText = "{T}: Add {C}.\n{1}, {T}: Add one mana of any color.\n{4}, {T}: Create a tapped Powerstone token. (It's an artifact with \"{T}: Add {C}. This mana can't be spent to cast a nonartifact spell.\")"

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}"), Costs.Tap)
        effect = Effects.AddAnyColorMana()
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{4}"), Costs.Tap)
        effect = Effects.CreatePowerstone(tapped = true)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "263"
        artist = "Christian Dimitrov"
        flavorText = "The Thran's machines were not dead after all, merely dormant . . . and waiting."
        imageUri = "https://cards.scryfall.io/normal/front/a/8/a8007012-39c5-4247-ba77-1cfcaade37fa.jpg?1783920006"
    }
}

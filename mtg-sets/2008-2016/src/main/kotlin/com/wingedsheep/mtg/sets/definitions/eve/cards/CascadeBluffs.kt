package com.wingedsheep.mtg.sets.definitions.eve.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Cascade Bluffs
 * Land
 * {T}: Add {C}.
 * {U/R}, {T}: Add {U}{U}, {U}{R}, or {R}{R}.
 */
val CascadeBluffs = card("Cascade Bluffs") {
    manaCost = ""
    colorIdentity = "UR"
    typeLine = "Land"
    oracleText = "{T}: Add {C}.\n{U/R}, {T}: Add {U}{U}, {U}{R}, or {R}{R}."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }
    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{U/R}"), Costs.Tap)
        effect = Effects.AddMana(Color.BLUE, 2)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }
    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{U/R}"), Costs.Tap)
        effect = Effects.AddMana(Color.BLUE) then Effects.AddMana(Color.RED)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }
    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{U/R}"), Costs.Tap)
        effect = Effects.AddMana(Color.RED, 2)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "175"
        artist = "Brandon Kitkouski"
        flavorText = "Travelers bathe in the falls to wash away curses from the pucas who lurk nearby."
        imageUri = "https://cards.scryfall.io/normal/front/c/3/c3eede44-270a-481d-850b-b4862b9685ea.jpg"
    }
}

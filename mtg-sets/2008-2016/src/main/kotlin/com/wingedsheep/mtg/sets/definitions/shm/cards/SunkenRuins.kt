package com.wingedsheep.mtg.sets.definitions.shm.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Sunken Ruins
 * Land
 * {T}: Add {C}.
 * {U/B}, {T}: Add {U}{U}, {U}{B}, or {B}{B}.
 */
val SunkenRuins = card("Sunken Ruins") {
    manaCost = ""
    colorIdentity = "UB"
    typeLine = "Land"
    oracleText = "{T}: Add {C}.\n{U/B}, {T}: Add {U}{U}, {U}{B}, or {B}{B}."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }
    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{U/B}"), Costs.Tap)
        effect = Effects.AddMana(Color.BLUE, 2)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }
    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{U/B}"), Costs.Tap)
        effect = Effects.AddMana(Color.BLUE) then Effects.AddMana(Color.BLACK)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }
    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{U/B}"), Costs.Tap)
        effect = Effects.AddMana(Color.BLACK, 2)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "280"
        artist = "Warren Mahy"
        flavorText = "Without the care of the tideshapers, the Lanes lie neglected. Some have flooded entire towns; others dried to cracked mud."
        imageUri = "https://cards.scryfall.io/normal/front/9/d/9d91a31c-b70a-45bd-a8dd-48d49b277f24.jpg?1783942705"
    }
}

package com.wingedsheep.mtg.sets.definitions.eve.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Flooded Grove
 * Land
 * {T}: Add {C}.
 * {G/U}, {T}: Add {G}{G}, {G}{U}, or {U}{U}.
 */
val FloodedGrove = card("Flooded Grove") {
    manaCost = ""
    colorIdentity = "UG"
    typeLine = "Land"
    oracleText = "{T}: Add {C}.\n{G/U}, {T}: Add {G}{G}, {G}{U}, or {U}{U}."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }
    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{G/U}"), Costs.Tap)
        effect = Effects.AddMana(Color.GREEN, 2)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }
    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{G/U}"), Costs.Tap)
        effect = Effects.AddMana(Color.GREEN) then Effects.AddMana(Color.BLUE)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }
    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{G/U}"), Costs.Tap)
        effect = Effects.AddMana(Color.BLUE, 2)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "177"
        artist = "Dave Kendall"
        flavorText = "At full moon, the river seeps over its banks and crawls through the woods like a blanket of serpents."
        imageUri = "https://cards.scryfall.io/normal/front/4/e/4e45d16b-8320-4a9b-ad15-9687dfb52bed.jpg"
    }
}

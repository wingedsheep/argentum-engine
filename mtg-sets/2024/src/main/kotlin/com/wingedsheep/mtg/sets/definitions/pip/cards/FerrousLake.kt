package com.wingedsheep.mtg.sets.definitions.pip.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Ferrous Lake
 * Land
 * {1}, {T}: Add {U}{R}.
 */
val FerrousLake = card("Ferrous Lake") {
    manaCost = ""
    colorIdentity = "UR"
    typeLine = "Land"
    oracleText = "{1}, {T}: Add {U}{R}."
    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}"), Costs.Tap)
        effect = Effects.AddMana(Color.BLUE, 1) then Effects.AddMana(Color.RED, 1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }
    metadata {
        rarity = Rarity.RARE
        collectorNumber = "148"
        artist = "Hristo D. Chukov"
        flavorText = "Even before it was a super mutant camp, the water treatment plant was used for all sorts of questionable experiments."
        imageUri = "https://cards.scryfall.io/normal/front/0/4/0406ff96-419b-44bc-b836-a39eaa55ae78.jpg?1783912375"
    }
}

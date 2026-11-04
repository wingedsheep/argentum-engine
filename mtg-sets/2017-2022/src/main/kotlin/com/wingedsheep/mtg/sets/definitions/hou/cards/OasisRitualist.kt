package com.wingedsheep.mtg.sets.definitions.hou.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule

val OasisRitualist = card("Oasis Ritualist") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Snake Druid"
    oracleText = "{T}: Add one mana of any color.\n" +
        "{T}, Exert this creature: Add two mana of any one color. " +
        "(An exerted creature won't untap during your next untap step.)"
    power = 2
    toughness = 4

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddAnyColorMana()
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Tap, Costs.Exert)
        effect = Effects.AddAnyColorMana(2)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "124"
        artist = "Josu Hernaiz"
        flavorText = "\"It's only the end if we allow it.\""
        imageUri = "https://cards.scryfall.io/normal/front/a/3/a30bb49b-956e-44bc-8a32-46d4c9c13245.jpg?1783936017"
    }
}

package com.wingedsheep.mtg.sets.definitions.inv.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Archaeological Dig (INV 320)
 * Land
 * {T}: Add {C}.
 * {T}, Sacrifice this land: Add one mana of any color.
 */
val ArchaeologicalDig = card("Archaeological Dig") {
    typeLine = "Land"
    oracleText = "{T}: Add {C}.\n{T}, Sacrifice this land: Add one mana of any color."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Tap, Costs.SacrificeSelf)
        effect = Effects.AddAnyColorMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "320"
        artist = "Don Hazeltine"
        imageUri = "https://cards.scryfall.io/normal/front/3/5/35f55af0-5a46-4900-b3d0-ca796b710e07.jpg?1562905889"
    }
}

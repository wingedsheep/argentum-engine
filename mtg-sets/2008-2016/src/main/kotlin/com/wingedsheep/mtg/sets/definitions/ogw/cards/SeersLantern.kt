package com.wingedsheep.mtg.sets.definitions.ogw.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Seer's Lantern
 * {3}
 * Artifact
 * {T}: Add {C}.
 * {2}, {T}: Scry 1. (Look at the top card of your library. You may put that card on the bottom.)
 */
val SeersLantern = card("Seer's Lantern") {
    manaCost = "{3}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "{T}: Add {C}.\n" +
        "{2}, {T}: Scry 1. (Look at the top card of your library. You may put that card on the bottom.)"

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}"), Costs.Tap)
        effect = Effects.Scry(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "165"
        artist = "Jung Park"
        flavorText = "A light to bring hope in dark times."
        imageUri = "https://cards.scryfall.io/normal/front/6/6/6618a854-7d9c-4e57-b959-4c0259cb4d97.jpg"
    }
}

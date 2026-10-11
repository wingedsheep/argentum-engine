package com.wingedsheep.mtg.sets.definitions.eve.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Rugged Prairie
 * Land
 * {T}: Add {C}.
 * {R/W}, {T}: Add {R}{R}, {R}{W}, or {W}{W}.
 */
val RuggedPrairie = card("Rugged Prairie") {
    manaCost = ""
    colorIdentity = "WR"
    typeLine = "Land"
    oracleText = "{T}: Add {C}.\n{R/W}, {T}: Add {R}{R}, {R}{W}, or {W}{W}."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }
    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{R/W}"), Costs.Tap)
        effect = Effects.AddMana(Color.RED, 2)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }
    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{R/W}"), Costs.Tap)
        effect = Effects.AddMana(Color.RED) then Effects.AddMana(Color.WHITE)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }
    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{R/W}"), Costs.Tap)
        effect = Effects.AddMana(Color.WHITE, 2)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "178"
        artist = "Fred Fields"
        flavorText = "Hobs bury their kin far from home. They believe the dry, open ground keeps hags from stealing the bones and gwyllions from stealing the spirits."
        imageUri = "https://cards.scryfall.io/normal/front/e/3/e31f8b2a-acf4-423c-bc99-8cf44f3c018a.jpg"
    }
}

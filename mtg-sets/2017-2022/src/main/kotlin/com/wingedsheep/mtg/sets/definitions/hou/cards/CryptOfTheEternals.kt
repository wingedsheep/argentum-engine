package com.wingedsheep.mtg.sets.definitions.hou.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule

val CryptOfTheEternals = card("Crypt of the Eternals") {
    manaCost = ""
    colorIdentity = "UBR"
    typeLine = "Land"
    oracleText = "When this land enters, you gain 1 life.\n{T}: Add {C}.\n{1}, {T}: Add {U}, {B}, or {R}."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.GainLife(1)
    }

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    for (color in listOf(Color.BLUE, Color.BLACK, Color.RED)) {
        activatedAbility {
            cost = Costs.Composite(Costs.Mana("{1}"), Costs.Tap)
            effect = Effects.AddMana(color)
            manaAbility = true
            timing = TimingRule.ManaAbility
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "169"
        artist = "Titus Lunter"
        flavorText = "The dead entered as worthy. They exited as Eternals."
        imageUri = "https://cards.scryfall.io/normal/front/c/a/cab5b199-e79d-4ca9-970c-cfd9df8fd1e4.jpg?1783935999"
    }
}

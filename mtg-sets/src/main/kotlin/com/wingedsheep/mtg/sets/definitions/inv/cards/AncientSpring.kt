package com.wingedsheep.mtg.sets.definitions.inv.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Ancient Spring
 * Land
 * This land enters tapped.
 * {T}: Add {U}.
 * {T}, Sacrifice this land: Add {W}{B}.
 */
val AncientSpring = card("Ancient Spring") {
    typeLine = "Land"
    colorIdentity = "WUB"
    oracleText = "This land enters tapped.\n{T}: Add {U}.\n{T}, Sacrifice this land: Add {W}{B}."

    replacementEffect(EntersTapped())

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.BLUE)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Tap, Costs.SacrificeSelf)
        effect = Effects.Composite(
            Effects.AddMana(Color.WHITE),
            Effects.AddMana(Color.BLACK),
        )
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "319"
        artist = "Don Hazeltine"
        imageUri = "https://cards.scryfall.io/normal/front/0/0/004eefa4-947b-45fc-b45c-5263bfd763bc.jpg?1562895051"
    }
}

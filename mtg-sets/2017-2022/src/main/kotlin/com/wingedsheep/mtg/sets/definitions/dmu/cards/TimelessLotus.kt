package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

val TimelessLotus = card("Timeless Lotus") {
    manaCost = "{5}"
    colorIdentity = "WUBRG"
    typeLine = "Legendary Artifact"
    oracleText = "Timeless Lotus enters tapped.\n{T}: Add {W}{U}{B}{R}{G}."

    replacementEffect(EntersTapped())

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.WHITE) then
            Effects.AddMana(Color.BLUE) then
            Effects.AddMana(Color.BLACK) then
            Effects.AddMana(Color.RED) then
            Effects.AddMana(Color.GREEN)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "239"
        artist = "Lindsey Look"
        flavorText = "Urza exploited nature to power his war machines; in time, nature exploited his war machines to renew itself."
        imageUri = "https://cards.scryfall.io/normal/front/6/f/6f566387-3342-4325-ba4c-eee7626072ac.jpg?1783921266"
    }
}

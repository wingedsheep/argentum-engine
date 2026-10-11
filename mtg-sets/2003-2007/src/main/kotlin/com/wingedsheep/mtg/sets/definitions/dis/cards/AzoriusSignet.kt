package com.wingedsheep.mtg.sets.definitions.dis.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Azorius Signet
 * {2}
 * Artifact
 * {1}, {T}: Add {W}{U}.
 */
val AzoriusSignet = card("Azorius Signet") {
    manaCost = "{2}"
    colorIdentity = "WU"
    typeLine = "Artifact"
    oracleText = "{1}, {T}: Add {W}{U}."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}"), Costs.Tap)
        effect = Effects.AddMana(Color.WHITE) then Effects.AddMana(Color.BLUE)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "159"
        artist = "Greg Hildebrandt"
        flavorText = "The maze-like design embodies the core of Azorius law—strict structure designed to test wills and stall change."
        imageUri = "https://cards.scryfall.io/normal/front/d/9/d91a527d-51f1-4fb1-9016-fc923fd43a6a.jpg"
    }
}

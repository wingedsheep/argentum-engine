package com.wingedsheep.mtg.sets.definitions.gpt.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Orzhov Signet
 * {2}
 * Artifact
 * {1}, {T}: Add {W}{B}.
 */
val OrzhovSignet = card("Orzhov Signet") {
    manaCost = "{2}"
    colorIdentity = "WB"
    typeLine = "Artifact"
    oracleText = "{1}, {T}: Add {W}{B}."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}"), Costs.Tap)
        effect = Effects.Composite(Effects.AddMana(Color.WHITE, 1), Effects.AddMana(Color.BLACK, 1))
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "155"
        artist = "Greg Hildebrandt"
        flavorText = "The form of the sigil is just as important as the sigil itself. If it's carried on a medallion, its bearer is a master. If it's tattooed on the body, its bearer is a slave."
        imageUri = "https://cards.scryfall.io/normal/front/f/9/f9298a1d-5b41-46d8-929c-b6980d1e6eb7.jpg?1753108346"
    }
}

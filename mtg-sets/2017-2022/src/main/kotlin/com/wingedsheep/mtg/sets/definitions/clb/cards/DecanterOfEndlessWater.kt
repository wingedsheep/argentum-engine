package com.wingedsheep.mtg.sets.definitions.clb.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.NoMaximumHandSize
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Decanter of Endless Water
 * {3}
 * Artifact
 * You have no maximum hand size.
 * {T}: Add one mana of any color.
 */
val DecanterOfEndlessWater = card("Decanter of Endless Water") {
    manaCost = "{3}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "You have no maximum hand size.\n{T}: Add one mana of any color."

    staticAbility {
        ability = NoMaximumHandSize
    }

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddAnyColorMana()
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "309"
        artist = "Titus Lunter"
        flavorText = "\"Now, remember: Under no circumstances should you utter the word 'geyser' in the same room as—oh, blast it!\"\n—Bindlehaim, Upper City merchant"
        imageUri = "https://cards.scryfall.io/normal/front/5/1/518bbc36-58ea-44c4-a857-75a3aff058f5.jpg?1783922678"
    }
}

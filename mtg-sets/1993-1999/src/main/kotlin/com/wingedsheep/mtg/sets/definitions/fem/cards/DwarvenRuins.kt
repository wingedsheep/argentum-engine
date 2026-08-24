package com.wingedsheep.mtg.sets.definitions.fem.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersTapped

/**
 * Dwarven Ruins
 * Land
 * This land enters tapped.
 * {T}: Add {R}.
 * {T}, Sacrifice this land: Add {R}{R}.
 *
 * One of the five "ruins" lands — see also [EbonStronghold], [HavenwoodBattleground],
 * [RuinsOfTrokair] and [SvyeluniteTemple].
 */
val DwarvenRuins = card("Dwarven Ruins") {
    colorIdentity = "R"
    typeLine = "Land"
    oracleText = "This land enters tapped.\n{T}: Add {R}.\n{T}, Sacrifice this land: Add {R}{R}."

    replacementEffect(EntersTapped())

    activatedAbility {
        cost = Costs.Tap
        manaAbility = true
        effect = Effects.AddMana(Color.RED)
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Tap, Costs.SacrificeSelf)
        manaAbility = true
        effect = Effects.AddMana(Color.RED, 2)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "94"
        artist = "Mark Poole"
        imageUri = "https://cards.scryfall.io/normal/front/0/d/0dfe1352-27be-4c99-a58f-b961f911f270.jpg?1783947879"
    }
}

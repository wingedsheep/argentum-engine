package com.wingedsheep.mtg.sets.definitions.hou.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Desert of the True
 * Land — Desert
 * This land enters tapped.
 * {T}: Add {W}.
 * Cycling {1}{W} ({1}{W}, Discard this card: Draw a card.)
 */
val DesertOfTheTrue = card("Desert of the True") {
    manaCost = ""
    colorIdentity = "W"
    typeLine = "Land — Desert"
    oracleText = "This land enters tapped.\n{T}: Add {W}.\nCycling {1}{W} ({1}{W}, Discard this card: Draw a card.)"

    replacementEffect(EntersTapped())

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.WHITE)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    keywordAbility(KeywordAbility.cycling("{1}{W}"))

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "174"
        artist = "Jung Park"
        imageUri = "https://cards.scryfall.io/normal/front/e/2/e2d9308c-8af4-4587-b740-aea874d5ccae.jpg?1783935998"
    }
}

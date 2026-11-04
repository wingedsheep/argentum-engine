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
 * Desert of the Glorified
 * Land — Desert
 * This land enters tapped.
 * {T}: Add {B}.
 * Cycling {1}{B} ({1}{B}, Discard this card: Draw a card.)
 */
val DesertOfTheGlorified = card("Desert of the Glorified") {
    manaCost = ""
    colorIdentity = "B"
    typeLine = "Land — Desert"
    oracleText = "This land enters tapped.\n{T}: Add {B}.\nCycling {1}{B} ({1}{B}, Discard this card: Draw a card.)"

    replacementEffect(EntersTapped())

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.BLACK)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    keywordAbility(KeywordAbility.cycling("{1}{B}"))

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "171"
        artist = "Jonas De Ro"
        imageUri = "https://cards.scryfall.io/normal/front/0/4/04a19aea-209d-4354-8c59-d08244185eb8.jpg?1783935997"
    }
}

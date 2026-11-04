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
 * Desert of the Fervent
 * Land — Desert
 * This land enters tapped.
 * {T}: Add {R}.
 * Cycling {1}{R} ({1}{R}, Discard this card: Draw a card.)
 */
val DesertOfTheFervent = card("Desert of the Fervent") {
    manaCost = ""
    colorIdentity = "R"
    typeLine = "Land — Desert"
    oracleText = "This land enters tapped.\n{T}: Add {R}.\nCycling {1}{R} ({1}{R}, Discard this card: Draw a card.)"

    replacementEffect(EntersTapped())

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.RED)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    keywordAbility(KeywordAbility.cycling("{1}{R}"))

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "170"
        artist = "Titus Lunter"
        imageUri = "https://cards.scryfall.io/normal/front/f/5/f547d664-25ce-4a24-b3ae-7bf3cbdf4703.jpg?1783935999"
    }
}

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
 * Desert of the Indomitable
 * Land — Desert
 * This land enters tapped.
 * {T}: Add {G}.
 * Cycling {1}{G} ({1}{G}, Discard this card: Draw a card.)
 */
val DesertOfTheIndomitable = card("Desert of the Indomitable") {
    manaCost = ""
    colorIdentity = "G"
    typeLine = "Land — Desert"
    oracleText = "This land enters tapped.\n{T}: Add {G}.\nCycling {1}{G} ({1}{G}, Discard this card: Draw a card.)"

    replacementEffect(EntersTapped())

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.GREEN)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    keywordAbility(KeywordAbility.cycling("{1}{G}"))

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "172"
        artist = "James Paick"
        imageUri = "https://cards.scryfall.io/normal/front/7/d/7d42aa0c-0408-4b0d-bec5-9861dbcf9ee1.jpg?1783935999"
    }
}

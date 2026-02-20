package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.effects.AddManaEffect
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Barren Moor
 * Land
 * Barren Moor enters the battlefield tapped.
 * {T}: Add {B}.
 * Cycling {B}
 */
val BarrenMoor = card("Barren Moor") {
    typeLine = "Land"
    oracleText = "Barren Moor enters the battlefield tapped.\n{T}: Add {B}.\nCycling {B}"

    replacementEffect(EntersTapped())

    activatedAbility {
        cost = AbilityCost.Tap
        effect = AddManaEffect(Color.BLACK)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    keywordAbility(KeywordAbility.cycling("{B}"))

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "312"
        artist = "Heather Hudson"
        flavorText = ""
        imageUri = "https://cards.scryfall.io/large/front/4/5/45be3811-a223-4c45-9b24-0317f2d53c60.jpg?1562911376"
    }
}

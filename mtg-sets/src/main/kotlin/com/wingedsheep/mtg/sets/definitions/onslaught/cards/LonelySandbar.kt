package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.AddManaEffect
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Lonely Sandbar
 * Land
 * Lonely Sandbar enters the battlefield tapped.
 * {T}: Add {U}.
 * Cycling {U}
 */
val LonelySandbar = card("Lonely Sandbar") {
    typeLine = "Land"
    oracleText = "Lonely Sandbar enters the battlefield tapped.\n{T}: Add {U}.\nCycling {U}"

    replacementEffect(EntersTapped())

    activatedAbility {
        cost = AbilityCost.Tap
        effect = AddManaEffect(Color.BLUE)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    keywordAbility(KeywordAbility.cycling("{U}"))

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "320"
        artist = "Heather Hudson"
        flavorText = ""
        imageUri = "https://cards.scryfall.io/large/front/d/8/d8ddab06-aff7-4c40-bcaa-10cbfe899dd9.jpg?1562946694"
    }
}

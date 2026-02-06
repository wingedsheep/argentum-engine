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
 * Forgotten Cave
 * Land
 * Forgotten Cave enters the battlefield tapped.
 * {T}: Add {R}.
 * Cycling {R}
 */
val ForgottenCave = card("Forgotten Cave") {
    typeLine = "Land"

    replacementEffect(EntersTapped())

    activatedAbility {
        cost = AbilityCost.Tap
        effect = AddManaEffect(Color.RED)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    keywordAbility(KeywordAbility.cycling("{R}"))

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "317"
        artist = "Tony Szczudlo"
        flavorText = ""
        imageUri = "https://cards.scryfall.io/large/front/c/5/c5202668-a32c-4473-b272-e86264992576.jpg?1562941555"
    }
}

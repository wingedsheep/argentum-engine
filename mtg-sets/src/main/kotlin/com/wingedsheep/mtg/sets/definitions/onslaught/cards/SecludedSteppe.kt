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
 * Secluded Steppe
 * Land
 * Secluded Steppe enters the battlefield tapped.
 * {T}: Add {W}.
 * Cycling {W}
 */
val SecludedSteppe = card("Secluded Steppe") {
    typeLine = "Land"
    oracleText = "Secluded Steppe enters the battlefield tapped.\n{T}: Add {W}.\nCycling {W}"

    replacementEffect(EntersTapped())

    activatedAbility {
        cost = AbilityCost.Tap
        effect = AddManaEffect(Color.WHITE)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    keywordAbility(KeywordAbility.cycling("{W}"))

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "324"
        artist = "Heather Hudson"
        flavorText = ""
        imageUri = "https://cards.scryfall.io/large/front/e/a/ea454280-f7f4-4315-bb46-b56050c02c97.jpg?1562950810"
    }
}

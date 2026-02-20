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
 * Tranquil Thicket
 * Land
 * Tranquil Thicket enters the battlefield tapped.
 * {T}: Add {G}.
 * Cycling {G}
 */
val TranquilThicket = card("Tranquil Thicket") {
    typeLine = "Land"
    oracleText = "Tranquil Thicket enters the battlefield tapped.\n{T}: Add {G}.\nCycling {G}"

    replacementEffect(EntersTapped())

    activatedAbility {
        cost = AbilityCost.Tap
        effect = AddManaEffect(Color.GREEN)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    keywordAbility(KeywordAbility.cycling("{G}"))

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "326"
        artist = "Heather Hudson"
        flavorText = ""
        imageUri = "https://cards.scryfall.io/large/front/a/f/afcb7cef-8aeb-4c84-88e9-6df17768e292.jpg?1562936584"
    }
}

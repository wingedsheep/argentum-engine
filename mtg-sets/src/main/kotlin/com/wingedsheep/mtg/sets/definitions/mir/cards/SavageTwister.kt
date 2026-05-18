package com.wingedsheep.mtg.sets.definitions.mir.cards

import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Savage Twister
 * {X}{R}{G}
 * Sorcery
 * Savage Twister deals X damage to each creature.
 */
val SavageTwister = card("Savage Twister") {
    manaCost = "{X}{R}{G}"
    colorIdentity = "RG"
    typeLine = "Sorcery"
    oracleText = "Savage Twister deals X damage to each creature."

    spell {
        effect = EffectPatterns.dealDamageToAll(DynamicAmount.XValue, GroupFilter.AllCreatures)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "280"
        artist = "Bob Eggleton"
        flavorText = "\"Frozen, we watched the funnel pluck up three of the goats—pook! pook! pook!—before we ran for the wadi.\"\n—Travelogue of Najat"
        imageUri = "https://cards.scryfall.io/normal/front/e/b/eb73313b-d39a-46ab-abfc-76f94a75dfca.jpg?1677281659"
    }
}

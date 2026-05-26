package com.wingedsheep.mtg.sets.definitions.arn.cards

import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Piety
 * {2}{W}
 * Instant
 * Blocking creatures get +0/+3 until end of turn.
 */
val Piety = card("Piety") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Blocking creatures get +0/+3 until end of turn."

    spell {
        effect = EffectPatterns.modifyStatsForAll(
            power = 0,
            toughness = 3,
            filter = GroupFilter.BlockingCreatures,
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "8"
        artist = "Mark Poole"
        flavorText = "\"Whoever obeys God and His Prophet, fears God and does his duty to Him, will surely find success.\" —The Qur'an, 24:52"
        imageUri = "https://cards.scryfall.io/normal/front/f/6/f649c571-d7ec-4ebc-9e18-b0657cab495b.jpg?1562941252"
    }
}

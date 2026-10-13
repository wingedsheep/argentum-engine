package com.wingedsheep.mtg.sets.definitions.pls.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Gaea's Might
 * {G}
 * Instant
 * Domain — Target creature gets +1/+1 until end of turn for each basic land type among lands
 * you control.
 */
val GaeasMight = card("Gaea's Might") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Domain — Target creature gets +1/+1 until end of turn for each basic land type among lands you control."

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.ModifyStats(DynamicAmounts.domain(), DynamicAmounts.domain(), creature)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "81"
        artist = "Ron Spencer"
        flavorText = "With the Heart of Yavimaya aiding him, Multani didn't even strain at the load."
        imageUri = "https://cards.scryfall.io/normal/front/6/7/67e5adce-7735-4fa5-aa14-8dce012e9fcc.jpg?1783945613"
    }
}

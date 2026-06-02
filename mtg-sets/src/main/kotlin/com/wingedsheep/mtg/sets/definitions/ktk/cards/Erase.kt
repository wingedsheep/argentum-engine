package com.wingedsheep.mtg.sets.definitions.ktk.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.dsl.Effects
/**
 * Erase
 * {W}
 * Instant
 * Exile target enchantment.
 */
val Erase = card("Erase") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Exile target enchantment."

    spell {
        val t = target("target", Targets.Enchantment)
        effect = Effects.Move(t, Zone.EXILE)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "9"
        artist = "Zack Stella"
        flavorText = "\"Truth is hard enough to see, let alone understand. We must remove all distractions to find clarity.\" —Zogye, wandering sage"
        imageUri = "https://cards.scryfall.io/normal/front/d/1/d1dd0e10-2ad7-467f-8d4b-c70b95bf2e9c.jpg?1562793960"
    }
}

package com.wingedsheep.mtg.sets.definitions.grn.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Collar the Culprit
 * {3}{W}
 * Instant
 * Destroy target creature with toughness 4 or greater.
 */
val CollarTheCulprit = card("Collar the Culprit") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Destroy target creature with toughness 4 or greater."

    spell {
        val t = target(TargetFilter.Creature.toughnessAtLeast(4))
        effect = Effects.Destroy(t)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "5"
        artist = "Victor Adame Minguez"
        flavorText = "\"Reports of Gruul rioters in four districts. Start with the big ones and work your way up.\"\n—Libuse, Boros sergeant"
        imageUri = "https://cards.scryfall.io/normal/front/c/d/cdf305b7-d1f7-4770-9201-8f3fb6735cd9.jpg?1783934203"
    }
}

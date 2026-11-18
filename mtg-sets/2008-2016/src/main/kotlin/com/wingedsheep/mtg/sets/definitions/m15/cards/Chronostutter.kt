package com.wingedsheep.mtg.sets.definitions.m15.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Chronostutter
 * {5}{U}
 * Instant
 * Put target creature into its owner's library second from the top.
 */
val Chronostutter = card("Chronostutter") {
    manaCost = "{5}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Put target creature into its owner's library second from the top."

    spell {
        val creature = target(TargetFilter.Creature)
        // 0-indexed: position 1 = second from the top.
        effect = Effects.PutIntoLibraryNthFromTop(creature, 1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "48"
        artist = "Seb McKinnon"
        flavorText = "Timing is everything."
        imageUri = "https://cards.scryfall.io/normal/front/b/b/bbef107d-c19f-4af3-9236-c66123075b41.jpg?1783939194"
    }
}

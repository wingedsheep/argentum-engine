package com.wingedsheep.mtg.sets.definitions.m13.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Downpour
 * {1}{U}
 * Instant
 * Tap up to three target creatures.
 */
val Downpour = card("Downpour") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Tap up to three target creatures."

    spell {
        targets(TargetFilter.Creature, count = 3, optional = true)
        effect = Effects.TapEachTarget()
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "48"
        artist = "Eytan Zana"
        flavorText = "\"The sky holds vast oceans. They are at your fingertips if you know how to call them.\"\n—Talrand, sky summoner"
        imageUri = "https://cards.scryfall.io/normal/front/f/2/f220afb1-8638-4b54-b6af-0043b4cc1cef.jpg"
    }
}

package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ReadTheRunesEffect

/**
 * Read the Runes
 * {X}{U}
 * Instant
 * Draw X cards. For each card drawn this way, discard a card unless you sacrifice a permanent.
 */
val ReadTheRunes = card("Read the Runes") {
    manaCost = "{X}{U}"
    typeLine = "Instant"
    oracleText = "Draw X cards. For each card drawn this way, discard a card unless you sacrifice a permanent."

    spell {
        effect = ReadTheRunesEffect
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "104"
        artist = "Alan Pollack"
        flavorText = "\"The world is a puzzle, and the mind is its key.\""
        imageUri = "https://cards.scryfall.io/normal/front/b/c/bc148c21-cbe6-4cea-899b-e62501b59a00.jpg?1562939380"
    }
}

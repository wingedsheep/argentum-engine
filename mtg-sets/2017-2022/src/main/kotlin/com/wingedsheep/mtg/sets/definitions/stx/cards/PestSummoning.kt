package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Pest Summoning — Strixhaven: School of Mages #211 (canonical printing)
 * {1}{B/G}{B/G} · Sorcery — Lesson
 *
 * Create two 1/1 black and green Pest creature tokens with "When this token dies, you gain 1 life."
 *
 * Two of the set's predefined Pest tokens via [Effects.CreatePest].
 */
val PestSummoning = card("Pest Summoning") {
    manaCost = "{1}{B/G}{B/G}"
    colorIdentity = "BG"
    typeLine = "Sorcery — Lesson"
    oracleText = "Create two 1/1 black and green Pest creature tokens with \"When this token dies, " +
        "you gain 1 life.\""

    spell {
        effect = Effects.CreatePest(count = 2)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "211"
        artist = "Zoltan Boros"
        flavorText = "\"Step 1: Put the pests in mud.\n\"Step 2: Wait.\"\n—*Gyome's Guide to Pest Breeding*"
        imageUri = "https://cards.scryfall.io/normal/front/6/2/6267e19a-a777-4767-8433-86b6624362b6.jpg?1783927303"
    }
}

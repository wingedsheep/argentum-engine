package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Stern Lesson
 * {2}{U}
 * Instant
 * Draw two cards, then discard a card. Create a tapped Powerstone token.
 */
val SternLesson = card("Stern Lesson") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Draw two cards, then discard a card. Create a tapped Powerstone token. (It's an artifact with \"{T}: Add {C}. This mana can't be spent to cast a nonartifact spell.\")"

    spell {
        effect = Effects.DrawCards(2) then
            Patterns.Hand.discardCards(1) then
            Effects.CreatePowerstone(1, tapped = true)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "64"
        artist = "Lie Setiawan"
        flavorText = "\"The brothers are brilliant, but their quarreling will be the death of me.\"\n—Tocasia, journal entry"
        imageUri = "https://cards.scryfall.io/normal/front/3/1/3151caa2-10ed-4f30-83eb-cd1bd3f642ce.jpg?1783920104"
    }
}

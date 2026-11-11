package com.wingedsheep.mtg.sets.definitions.ogw.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Witness the End
 * {3}{B}
 * Sorcery
 * Devoid (This card has no color.)
 * Target opponent exiles two cards from their hand and loses 2 life.
 *
 * The opponent picks the two cards themselves (it's an exile, not a discard).
 */
val WitnesstheEnd = card("Witness the End") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Devoid (This card has no color.)\n" +
        "Target opponent exiles two cards from their hand and loses 2 life."

    keywords(Keyword.DEVOID)

    spell {
        val opponent = target(Targets.Opponent)
        effect = Patterns.Hand.exileFromHand(2, opponent) then Effects.LoseLife(2, opponent)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "82"
        artist = "Igor Kieryluk"
        flavorText = "\"Ulamog stood before us at Sea Gate, ready to succumb . . . and then Kozilek arose. " +
            "Whatever hope I had for our world died.\"\n—Vandri, Kalastria emissary"
        imageUri = "https://cards.scryfall.io/normal/front/0/c/0c06d8ec-55ab-4be9-a470-77c79d643d1f.jpg?1783937913"
    }
}

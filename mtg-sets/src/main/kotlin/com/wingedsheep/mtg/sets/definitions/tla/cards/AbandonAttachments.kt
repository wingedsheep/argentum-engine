package com.wingedsheep.mtg.sets.definitions.tla.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.MayEffect

/**
 * Abandon Attachments — {1}{U/R}
 * Instant — Lesson
 * You may discard a card. If you do, draw two cards.
 */
val AbandonAttachments = card("Abandon Attachments") {
    manaCost = "{1}{U/R}"
    colorIdentity = "UR"
    typeLine = "Instant — Lesson"
    oracleText = "You may discard a card. If you do, draw two cards."

    spell {
        effect = MayEffect(
            Effects.Composite(listOf(
                Patterns.Hand.discardCards(1),
                Effects.DrawCards(2)
            ))
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "205"
        artist = "Shahab Alizadeh"
        flavorText = "\"Learn to let her go, or you cannot let the pure cosmic energy flow " +
            "in from the universe.\"\n—Guru Pathik"
        imageUri = "https://cards.scryfall.io/normal/front/7/4/74ca45a4-97ab-4255-9129-884e8b42b984.jpg?1764121430"
    }
}

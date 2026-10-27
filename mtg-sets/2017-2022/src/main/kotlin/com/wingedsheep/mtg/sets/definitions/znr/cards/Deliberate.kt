package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Deliberate — Zendikar Rising #56
 * {1}{U} · Instant
 *
 * Scry 2, then draw a card.
 */
val Deliberate = card("Deliberate") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Scry 2, then draw a card."

    spell {
        effect = Effects.Scry(2) then Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "56"
        artist = "Slawomir Maniak"
        flavorText = "\"I can't see all that the future holds, but I can keep us one step ahead of death.\""
        imageUri = "https://cards.scryfall.io/normal/front/c/4/c41c0dd0-fef2-4f3c-8f3c-9c9c521b5442.jpg"
    }
}

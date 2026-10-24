package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Practical Research — Strixhaven: School of Mages #212 (canonical printing)
 * {3}{U}{R} · Instant
 *
 * Draw four cards. Then discard two cards unless you discard an instant or sorcery card.
 *
 * Steamcore Scholar's shape: [Effects.DrawCards] then [Patterns.Hand.discardCardsUnlessMatching],
 * one choose-two selection whose minimum drops to one once it holds an instant or sorcery card —
 * so either a single instant/sorcery or any two cards may be discarded (ruling).
 */
val PracticalResearch = card("Practical Research") {
    manaCost = "{3}{U}{R}"
    colorIdentity = "RU"
    typeLine = "Instant"
    oracleText = "Draw four cards. Then discard two cards unless you discard an instant or sorcery card."

    spell {
        effect = Effects.DrawCards(4) then
            Patterns.Hand.discardCardsUnlessMatching(
                count = 2,
                unlessFilter = GameObjectFilter.InstantOrSorcery,
                prompt = "Discard two cards, or a single instant or sorcery card",
            )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "212"
        artist = "Ekaterina Burmak"
        flavorText = "The twins each researched spells to defeat Extus in their own way. Will hit the library. Rowan hit everything else."
        imageUri = "https://cards.scryfall.io/normal/front/7/4/74edd3e3-b2de-4ba0-a508-0418b0151d87.jpg?1783927303"
        ruling("2021-04-16", "As Practical Research resolves, you can discard either one instant or sorcery card or discard two cards, each of which may or may not be an instant or sorcery card.")
    }
}

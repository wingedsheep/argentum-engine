package com.wingedsheep.mtg.sets.definitions.chk.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * Sift Through Sands
 * {1}{U}{U}
 * Instant — Arcane
 * Draw two cards, then discard a card.
 * If you've cast a spell named Peer Through Depths and a spell named Reach Through Mists this
 * turn, you may search your library for a card named The Unspeakable, put it onto the
 * battlefield, then shuffle.
 *
 * The "cast this turn" test reads the per-player cast history (`YouCastSpellsThisTurn`), whose
 * records carry the spell's name, so both halves hold even after those spells have resolved.
 * It is checked on resolution; the "you may" wraps the whole search, so declining skips the
 * shuffle too.
 */
val SiftThroughSands = card("Sift Through Sands") {
    manaCost = "{1}{U}{U}"
    colorIdentity = "U"
    typeLine = "Instant — Arcane"
    oracleText = "Draw two cards, then discard a card.\n" +
        "If you've cast a spell named Peer Through Depths and a spell named Reach Through Mists " +
        "this turn, you may search your library for a card named The Unspeakable, put it onto the " +
        "battlefield, then shuffle."

    spell {
        effect = Effects.DrawCards(2) then
            Patterns.Hand.discardCards(1) then
            Effects.If(
                Conditions.All(
                    Conditions.YouCastSpellsThisTurn(1, GameObjectFilter.Any.named("Peer Through Depths")),
                    Conditions.YouCastSpellsThisTurn(1, GameObjectFilter.Any.named("Reach Through Mists")),
                ),
                Effects.May(
                    Patterns.Library.searchLibrary(
                        filter = GameObjectFilter.Any.named("The Unspeakable"),
                        destination = SearchDestination.BATTLEFIELD,
                    )
                )
            )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "84"
        artist = "Anthony S. Waters"
        imageUri = "https://cards.scryfall.io/normal/front/0/7/077bde4d-4cdb-42db-acc5-441ed8fa4a5b.jpg?1783944322"
    }
}

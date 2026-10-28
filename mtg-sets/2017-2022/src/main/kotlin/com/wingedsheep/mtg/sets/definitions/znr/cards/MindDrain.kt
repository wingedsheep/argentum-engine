package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Mind Drain — Zendikar Rising #114
 * {2}{B} · Sorcery
 *
 * Target opponent discards two cards, mills a card, and loses 1 life. You gain 1 life.
 */
val MindDrain = card("Mind Drain") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Target opponent discards two cards, mills a card, and loses 1 life. You gain 1 life. " +
        "(To mill a card, a player puts the top card of their library into their graveyard.)"

    spell {
        val opponent = target(Targets.Opponent)
        effect = Effects.Discard(2, opponent) then
            Patterns.Library.mill(1, opponent) then
            Effects.LoseLife(1, opponent) then
            Effects.GainLife(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "114"
        artist = "Grzegorz Rutkowski"
        flavorText = "\"The meal was exquisite. The conversation, less so.\""
        imageUri = "https://cards.scryfall.io/normal/front/0/6/0683d586-a54b-425a-a24d-2efb2342f150.jpg?1783929372"
        ruling(
            "2020-09-25",
            "If the target opponent can discard only one card or can't discard any cards, they still mill a " +
                "card and lose 1 life, and you gain 1 life."
        )
    }
}

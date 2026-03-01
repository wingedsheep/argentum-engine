package com.wingedsheep.mtg.sets.definitions.scourge.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Brain Freeze
 * {1}{U}
 * Instant
 * Target player mills three cards.
 * Storm (When you cast this spell, copy it for each spell cast before it this turn.
 * You may choose new targets for the copies.)
 */
val BrainFreeze = card("Brain Freeze") {
    manaCost = "{1}{U}"
    typeLine = "Instant"
    oracleText = "Target player mills three cards.\nStorm (When you cast this spell, copy it for each spell cast before it this turn. You may choose new targets for the copies.)"

    spell {
        val t = target("target player", Targets.Player)
        effect = Effects.Mill(3, t)
    }

    keywords(Keyword.STORM)

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "29"
        artist = "Tim Hildebrandt"
        imageUri = "https://cards.scryfall.io/normal/front/5/9/59a43ef5-08f0-44fc-802d-b6cfd56b7d1f.jpg?1562529493"
    }
}

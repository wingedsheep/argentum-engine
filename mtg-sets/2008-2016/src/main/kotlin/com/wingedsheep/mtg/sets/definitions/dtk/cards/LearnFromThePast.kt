package com.wingedsheep.mtg.sets.definitions.dtk.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Learn from the Past
 * {3}{U}
 * Instant
 * Target player shuffles their graveyard into their library.
 * Draw a card.
 */
val LearnFromThePast = card("Learn from the Past") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Target player shuffles their graveyard into their library.\nDraw a card."

    spell {
        val t = target(Targets.Player)
        effect = Patterns.Library.shuffleGraveyardIntoLibrary(t) then Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "60"
        artist = "Chase Stone"
        flavorText = "\"I've been told my whole life that the great Ojutai holds all knowledge, " +
            "but I wonder what our forebears knew.\"\n—Narset"
        imageUri = "https://cards.scryfall.io/normal/front/3/4/347c6e76-dba4-4448-a4f4-d9591d4539c7.jpg?1783938607"

        ruling(
            "2015-02-25",
            "If the player's graveyard is empty, the player will shuffle their library, then you'll draw a card.",
        )
    }
}

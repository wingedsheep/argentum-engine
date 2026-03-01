package com.wingedsheep.mtg.sets.definitions.scourge.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Hunting Pack
 * {5}{G}{G}
 * Instant
 * Create a 4/4 green Beast creature token.
 * Storm (When you cast this spell, copy it for each spell cast before it this turn.)
 */
val HuntingPack = card("Hunting Pack") {
    manaCost = "{5}{G}{G}"
    typeLine = "Instant"
    oracleText = "Create a 4/4 green Beast creature token.\nStorm (When you cast this spell, copy it for each spell cast before it this turn.)"

    spell {
        effect = Effects.CreateToken(
            power = 4,
            toughness = 4,
            colors = setOf(Color.GREEN),
            creatureTypes = setOf("Beast")
        )
    }

    keywords(Keyword.STORM)

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "121"
        artist = "Jim Nelson"
        imageUri = "https://cards.scryfall.io/normal/front/8/b/8b0f5d29-5342-4591-bdc9-c2bc9289ed41.jpg?1562531696"
    }
}

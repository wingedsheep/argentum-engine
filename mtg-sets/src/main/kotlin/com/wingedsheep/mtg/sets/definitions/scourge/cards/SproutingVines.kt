package com.wingedsheep.mtg.sets.definitions.scourge.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Sprouting Vines
 * {2}{G}
 * Instant
 * Search your library for a basic land card, reveal that card, put it into your hand, then shuffle.
 * Storm (When you cast this spell, copy it for each spell cast before it this turn.)
 */
val SproutingVines = card("Sprouting Vines") {
    manaCost = "{2}{G}"
    typeLine = "Instant"
    oracleText = "Search your library for a basic land card, reveal that card, put it into your hand, then shuffle.\nStorm (When you cast this spell, copy it for each spell cast before it this turn.)"

    spell {
        effect = Effects.SearchLibrary(
            filter = GameObjectFilter.BasicLand,
            reveal = true
        )
    }

    keywords(Keyword.STORM)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "128"
        artist = "John Avon"
        imageUri = "https://cards.scryfall.io/normal/front/3/a/3a3246a6-b604-4f9f-adb9-3692e0fa8638.jpg?1562527612"
    }
}

package com.wingedsheep.mtg.sets.definitions.scourge.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * Dragonstorm
 * {8}{R}
 * Sorcery
 * Search your library for a Dragon permanent card, put it onto the battlefield, then shuffle.
 * Storm (When you cast this spell, copy it for each spell cast before it this turn.)
 */
val Dragonstorm = card("Dragonstorm") {
    manaCost = "{8}{R}"
    typeLine = "Sorcery"
    oracleText = "Search your library for a Dragon permanent card, put it onto the battlefield, then shuffle.\nStorm (When you cast this spell, copy it for each spell cast before it this turn.)"

    spell {
        effect = Effects.SearchLibrary(
            filter = GameObjectFilter.Permanent.withSubtype("Dragon"),
            destination = SearchDestination.BATTLEFIELD
        )
    }

    keywords(Keyword.STORM)

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "90"
        artist = "Kev Walker"
        imageUri = "https://cards.scryfall.io/normal/front/4/b/4b9aa594-39e6-4824-aed9-75d1a301ac51.jpg?1562528558"
    }
}

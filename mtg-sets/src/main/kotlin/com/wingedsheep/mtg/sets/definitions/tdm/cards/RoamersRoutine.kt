package com.wingedsheep.mtg.sets.definitions.tdm.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.dsl.LibraryPatterns

/**
 * Roamer's Routine — Tarkir: Dragonstorm #154
 * {2}{G} · Sorcery
 *
 * Search your library for a basic land card, put it onto the battlefield tapped, then shuffle.
 * Harmonize {4}{G} (You may cast this card from your graveyard for its harmonize cost. You may
 * tap a creature you control to reduce that cost by {X}, where X is its power. Then exile this
 * spell.)
 */
val RoamersRoutine = card("Roamer's Routine") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Search your library for a basic land card, put it onto the battlefield tapped, then shuffle.\n" +
        "Harmonize {4}{G} (You may cast this card from your graveyard for its harmonize cost. You may tap a creature " +
        "you control to reduce that cost by {X}, where X is its power. Then exile this spell.)"

    spell {
        effect = LibraryPatterns.searchLibrary(
            filter = GameObjectFilter.BasicLand,
            count = 1,
            destination = SearchDestination.BATTLEFIELD,
            entersTapped = true,
            shuffleAfter = true
        )
    }

    keywordAbility(KeywordAbility.harmonize("{4}{G}"))

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "154"
        artist = "Andrew Mar"
        imageUri = "https://cards.scryfall.io/normal/front/f/b/fb8c2d5c-ba0c-4d50-8898-5c6574b1e974.jpg?1743204581"
    }
}

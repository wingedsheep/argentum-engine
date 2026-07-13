package com.wingedsheep.mtg.sets.definitions.vow.cards

import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * Dig Up
 * {G}
 * Sorcery
 * Cleave {1}{B}{B}{G} (You may cast this spell for its cleave cost. If you do, remove the words
 * in square brackets.)
 * Search your library for a [basic land] card, [reveal it,] put it into your hand, then shuffle.
 *
 * Cleave (CR 702.148) removes the bracketed words when its alternative cost is paid. Dig Up has
 * *two* bracket spans — `[basic land]` and `[reveal it,]` — so paying the cleave cost both widens
 * the search from a basic land to any card and drops the reveal. The printed cast is a basic-land
 * tutor to hand (revealed); the cleaved cast is an unconditional tutor to hand (no reveal).
 *
 * No target, so only the effect differs: base [effect] searches for a basic land and reveals it,
 * [cleaveEffect] searches for any card without revealing. The engine selects the cleave effect
 * when the spell is cast for its cleave cost.
 */
val DigUp = card("Dig Up") {
    manaCost = "{G}"
    colorIdentity = "BG"
    typeLine = "Sorcery"
    oracleText = "Cleave {1}{B}{B}{G} (You may cast this spell for its cleave cost. If you do, " +
        "remove the words in square brackets.)\nSearch your library for a [basic land] card, " +
        "[reveal it,] put it into your hand, then shuffle."

    keywordAbility(KeywordAbility.cleave("{1}{B}{B}{G}"))

    spell {
        // Printed (brackets present): search for a basic land, reveal it, to hand, shuffle.
        effect = Patterns.Library.searchLibrary(
            filter = GameObjectFilter.BasicLand,
            count = 1,
            destination = SearchDestination.HAND,
            reveal = true,
            shuffleAfter = true,
        )

        // Cleaved (brackets removed): search for any card, to hand, shuffle (no reveal).
        cleaveEffect = Patterns.Library.searchLibrary(
            filter = GameObjectFilter.Any,
            count = 1,
            destination = SearchDestination.HAND,
            reveal = false,
            shuffleAfter = true,
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "197"
        artist = "Slawomir Maniak"
        flavorText = "Things buried on Innistrad rarely seem to stay that way."
        imageUri = "https://cards.scryfall.io/normal/front/8/f/8f14c947-2452-4fd6-8f1a-391cf5898100.jpg?1782703052"
    }
}

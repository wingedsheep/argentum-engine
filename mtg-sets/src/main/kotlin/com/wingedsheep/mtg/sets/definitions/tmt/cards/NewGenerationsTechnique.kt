package com.wingedsheep.mtg.sets.definitions.tmt.cards

import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * New Generation's Technique
 * {3}{G}
 * Sorcery
 *
 * Sneak {2}{G} (You may cast this spell for {2}{G} if you also return an
 * unblocked attacker you control to hand during the declare blockers step.)
 * Search your library for up to two basic land cards, put them onto the
 * battlefield tapped, then shuffle.
 */
val NewGenerationsTechnique = card("New Generation's Technique") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Sneak {2}{G} (You may cast this spell for {2}{G} if you also return an unblocked attacker you control to hand during the declare blockers step.)\nSearch your library for up to two basic land cards, put them onto the battlefield tapped, then shuffle."

    sneak("{2}{G}")

    spell {
        effect = Patterns.Library.searchLibrary(
            filter = GameObjectFilter.BasicLand,
            count = 2,
            destination = SearchDestination.BATTLEFIELD,
            entersTapped = true,
            shuffleAfter = true
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "126"
        artist = "Dominik Mayer"
        flavorText = "\"Uno, Moja, Odyn, and Yi carry on the brothers' legacy. New York will be free once more.\""
        imageUri = "https://cards.scryfall.io/normal/front/f/1/f1616a07-e4a1-4574-b341-d6b31d76b3c5.jpg?1771586961"
    }
}

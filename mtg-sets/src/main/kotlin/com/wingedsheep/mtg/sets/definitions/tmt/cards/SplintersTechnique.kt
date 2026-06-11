package com.wingedsheep.mtg.sets.definitions.tmt.cards

import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Splinter's Technique
 * {3}{B}
 * Sorcery
 *
 * Sneak {1}{B} (You may cast this spell for {1}{B} if you also return an
 * unblocked attacker you control to hand during the declare blockers step.)
 * Search your library for a card, put that card into your hand, then shuffle.
 */
val SplintersTechnique = card("Splinter's Technique") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Sneak {1}{B} (You may cast this spell for {1}{B} if you also return an unblocked attacker you control to hand during the declare blockers step.)\nSearch your library for a card, put that card into your hand, then shuffle."

    sneak("{1}{B}")

    spell {
        effect = Patterns.Library.searchLibrary(count = 1, shuffleAfter = true)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "80"
        artist = "Jo Cordisco"
        flavorText = "\"Possess the right thinking. Only then can one receive the gifts of strength, knowledge, and peace.\""
        imageUri = "https://cards.scryfall.io/normal/front/f/d/fd3a5465-074a-4688-b79b-68e232076581.jpg?1769006137"
    }
}

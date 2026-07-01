package com.wingedsheep.mtg.sets.definitions.fdn.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Uncharted Voyage
 * {3}{U}
 * Instant
 *
 * Target creature's owner puts it on their choice of the top or bottom of their library.
 * Surveil 1.
 *
 * A composite of two existing atoms: [Effects.PutOnTopOrBottomOfLibrary] (the same top-or-bottom
 * bounce Dire Downdraft uses — the owner chooses) followed by [Effects.Surveil]. Surveil 1 always
 * happens, even if the bounce fizzles (its target left), because it's an untargeted second clause.
 */
val UnchartedVoyage = card("Uncharted Voyage") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Target creature's owner puts it on their choice of the top or bottom of their library.\n" +
        "Surveil 1. (Look at the top card of your library. You may put it into your graveyard.)"

    spell {
        val creature = target("target creature", Targets.Creature)
        effect = Effects.PutOnTopOrBottomOfLibrary(creature)
            .then(Effects.Surveil(1))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "53"
        artist = "Julian Kok Joon Wen"
        flavorText = "As his ship passed the waterlogged wrecks, Markos realized he wasn't the first person to try sailing out of the Underworld."
        imageUri = "https://cards.scryfall.io/normal/front/e/0/e0846820-e595-4743-8a28-29c57d728677.jpg?1782689218"
    }
}

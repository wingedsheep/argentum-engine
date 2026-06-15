package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Sauron's Ransom
 * {1}{U}{B}
 * Instant
 *
 * Choose an opponent. They look at the top four cards of your library and separate
 * them into a face-down pile and a face-up pile. Put one pile into your hand and the
 * other into your graveyard. The Ring tempts you.
 *
 * A Fact or Fiction variant (CR 700.3 "divvy"): reveal/look at the top four, an
 * opponent partitions them into two piles, then you choose which pile goes to your
 * hand and which goes to your graveyard — reusing [Patterns.Library.factOrFiction]
 * (the shared pile-split primitive) with `count = 4`, chained with "The Ring tempts
 * you". In a two-player game the lone opponent is the chooser automatically.
 *
 * Note: the printed face-down / face-up distinction is an information-visibility
 * nuance only (the chooser of the split sees both piles either way; the controller
 * then picks where each pile goes). The split-and-choose and hand/graveyard routing —
 * the mechanically load-bearing part — are modeled exactly.
 */
val SauronsRansom = card("Sauron's Ransom") {
    manaCost = "{1}{U}{B}"
    colorIdentity = "UB"
    typeLine = "Instant"
    oracleText = "Choose an opponent. They look at the top four cards of your library and separate them into a face-down pile and a face-up pile. Put one pile into your hand and the other into your graveyard. The Ring tempts you."

    spell {
        effect = Patterns.Library.factOrFiction(count = 4)
            .then(Effects.TheRingTemptsYou())
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "225"
        artist = "Alex Brock"
        flavorText = "\"He was dear to you, I see. And now he shall endure the slow torment of years.\""
        imageUri = "https://cards.scryfall.io/normal/front/6/b/6b98850c-ad69-42da-b91a-8dc5e226c444.jpg?1686970005"
    }
}

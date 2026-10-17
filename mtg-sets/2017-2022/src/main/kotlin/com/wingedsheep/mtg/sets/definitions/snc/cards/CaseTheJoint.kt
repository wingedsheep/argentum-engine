package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Case the Joint
 * {3}{U}
 * Instant
 * Draw two cards, then look at the top card of each player's library.
 *
 * The draw runs first, then a bare `gather` over [CardSource.TopOfLibrary] for [Player.Each] —
 * the private look (default `LookAudience.Controller`, not revealed) with no follow-up move, so
 * every library stays exactly as it was. Same look shape as Callous Deceiver's `{1}` ability,
 * widened to each player.
 */
val CaseTheJoint = card("Case the Joint") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Draw two cards, then look at the top card of each player's library."

    spell {
        effect = Effects.Pipeline {
            run(Effects.DrawCards(2))
            gather(CardSource.TopOfLibrary(count = 1, player = Player.Each))
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "37"
        artist = "Stella Spente"
        flavorText = "Steel doors and iron bars are child's play for an Obscura operative."
        imageUri = "https://cards.scryfall.io/normal/front/0/f/0f9edf04-681d-45f9-975f-704154040506.jpg?1783923149"
    }
}

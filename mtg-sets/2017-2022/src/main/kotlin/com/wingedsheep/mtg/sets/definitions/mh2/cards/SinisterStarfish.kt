package com.wingedsheep.mtg.sets.definitions.mh2.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Sinister Starfish — Modern Horizons 2 #99
 * {1}{B} · Creature — Starfish · 0 / 3
 *
 * {T}: Surveil 1. (Look at the top card of your library. You may put it into your graveyard.)
 *
 * A free, repeatable surveil engine: the cost is a bare tap, no mana. `Patterns.Library.surveil`
 * is the single facade for the whole "look at the top N, choose which go to the graveyard"
 * decision flow, so the ability is one effect rather than a look/choose/move pipeline.
 */
val SinisterStarfish = card("Sinister Starfish") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Starfish"
    power = 0
    toughness = 3
    oracleText = "{T}: Surveil 1. (Look at the top card of your library. You may put it into your graveyard.)"

    activatedAbility {
        cost = Costs.Tap
        effect = Patterns.Library.surveil(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "99"
        artist = "Nils Hamm"
        flavorText = "\"Throw that one back. I don't like how it's looking at me.\"\n—Netos, Meletian fisherman"
        imageUri = "https://cards.scryfall.io/normal/front/d/b/db209732-c290-4999-a1aa-2369dfa8790c.jpg?1783926855"
    }
}

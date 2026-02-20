package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.TargetOpponent

/**
 * Head Games
 * {3}{B}{B}
 * Sorcery
 * Target opponent puts the cards from their hand on top of their library.
 * Search that player's library for that many cards. The player puts those
 * cards into their hand, then shuffles.
 */
val HeadGames = card("Head Games") {
    manaCost = "{3}{B}{B}"
    typeLine = "Sorcery"
    oracleText = "Target opponent puts the cards from their hand on top of their library. Search that player's library for that many cards. The player puts those cards into their hand, then shuffles."

    spell {
        target = TargetOpponent()
        effect = Effects.HeadGames()
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "155"
        artist = "Terese Nielsen"
        flavorText = "\"Don't worry. I'm not going to deprive you of your memories. I'm just going to replace them.\""
        imageUri = "https://cards.scryfall.io/large/front/8/6/86ecc098-aa2b-4bae-80d5-4d02128ef837.jpg?1562926804"
    }
}

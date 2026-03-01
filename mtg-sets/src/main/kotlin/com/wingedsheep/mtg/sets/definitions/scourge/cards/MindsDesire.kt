package com.wingedsheep.mtg.sets.definitions.scourge.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Mind's Desire
 * {4}{U}{U}
 * Sorcery
 * Shuffle your library. Then exile the top card of your library. Until end of turn,
 * you may play that card without paying its mana cost.
 * Storm (When you cast this spell, copy it for each spell cast before it this turn.)
 */
val MindsDesire = card("Mind's Desire") {
    manaCost = "{4}{U}{U}"
    typeLine = "Sorcery"
    oracleText = "Shuffle your library. Then exile the top card of your library. Until end of turn, you may play that card without paying its mana cost.\nStorm (When you cast this spell, copy it for each spell cast before it this turn.)"

    spell {
        effect = Effects.ShuffleAndExileTopPlayFree()
    }

    keywords(Keyword.STORM)

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "41"
        artist = "Ron Spencer"
        imageUri = "https://cards.scryfall.io/normal/front/3/c/3c7474e1-cfae-4867-a11a-d5cf9ff7ea5f.jpg?1562527625"
        ruling("2004-10-04", "If the card is not played by end of turn, it remains exiled until end of game.")
        ruling("2004-10-04", "The card is face-up when exiled.")
    }
}

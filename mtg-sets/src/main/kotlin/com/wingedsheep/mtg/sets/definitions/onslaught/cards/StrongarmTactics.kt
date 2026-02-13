package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EachPlayerDiscardsOrLoseLifeEffect

/**
 * Strongarm Tactics
 * {1}{B}
 * Sorcery
 * Each player discards a card. Then each player who didn't discard a creature card this way loses 4 life.
 */
val StrongarmTactics = card("Strongarm Tactics") {
    manaCost = "{1}{B}"
    typeLine = "Sorcery"
    oracleText = "Each player discards a card. Then each player who didn't discard a creature card this way loses 4 life."

    spell {
        effect = EachPlayerDiscardsOrLoseLifeEffect(lifeLoss = 4)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "173"
        artist = "Greg Hildebrandt & Tim Hildebrandt"
        flavorText = "If you can't pay your gambling debts, you just might be tomorrow's main attraction at the Grand Coliseum."
        imageUri = "https://cards.scryfall.io/normal/front/5/7/57dcf434-5c67-440a-8b67-2df7307e92bd.jpg?1562915597"
    }
}

package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Mind Grind
 * {X}{U}{B}
 * Sorcery
 * Each opponent reveals cards from the top of their library until they reveal X land cards, then
 * puts all cards revealed this way into their graveyard. X can't be 0.
 *
 * Per opponent, `gatherUntilMatch(count = X)` walks their library until the Xth land (or the
 * library runs out) and every revealed card goes to their graveyard (Mirko Vosk's pattern).
 * "X can't be 0" is `xMinimum = 1`, which also makes a cast "without paying its mana cost"
 * impossible (X would have to be 0, CR 107.3b).
 */
val MindGrind = card("Mind Grind") {
    manaCost = "{X}{U}{B}"
    colorIdentity = "UB"
    typeLine = "Sorcery"
    oracleText = "Each opponent reveals cards from the top of their library until they reveal X land cards, " +
        "then puts all cards revealed this way into their graveyard. X can't be 0."

    spell {
        xMinimum = 1
        effect = Effects.ForEachPlayer(Player.EachOpponent, Effects.Pipeline {
            val (_, allRevealed) = gatherUntilMatch(
                GameObjectFilter.Land,
                player = Player.You,
                count = DynamicAmounts.xValue()
            )
            reveal(allRevealed)
            toGraveyard(allRevealed, player = Player.You)
        })
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "178"
        artist = "Daarken"
        imageUri = "https://cards.scryfall.io/normal/front/6/7/671eb9e8-1a69-4570-8972-2e7de371cef4.jpg?1783940104"
        ruling("2013-01-24", "If an opponent's library contains fewer than X land cards, all cards from that library are revealed and put into their graveyard.")
        ruling("2013-01-24", "If another spell or ability instructs you to cast Mind Grind \"without paying its mana cost,\" you won't be able to. You must pick 0 as the value of X in the mana cost of a spell being cast \"without paying its mana cost,\" but the X in Mind Grind's mana cost can't be 0.")
    }
}

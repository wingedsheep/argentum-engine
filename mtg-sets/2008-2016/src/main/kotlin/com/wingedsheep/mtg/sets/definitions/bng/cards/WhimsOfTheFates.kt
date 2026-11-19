package com.wingedsheep.mtg.sets.definitions.bng.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Whims of the Fates — Born of the Gods #115 (canonical printing)
 * {5}{R} · Sorcery
 *
 * Starting with you, each player separates all permanents they control into three piles. Then
 * each player chooses one of their piles at random and sacrifices those permanents. (Piles can be
 * empty.)
 *
 * Modeling notes:
 *  - [Player.EachStartingWithYou] is the "starting with you" order; inside the loop the visited
 *    player is "you", so each player gathers and splits *their own* permanents.
 *  - Three piles are two any-number splits: the first selection is Pile 1, the second selection
 *    from the rest is Pile 2, and what is left is Pile 3. Any pile may be empty.
 *  - `choosePile(…, atRandom = true)` draws one of the three uniformly (an empty pile counts —
 *    the ruling's "each pile must have an equal chance"). The draw is silent, so no later player
 *    learns an earlier player's result before splitting; nothing is revealed until the sacrifice.
 *  - Every player's drawn pile is collected and sacrificed in one move after the loop, so all
 *    permanents leave simultaneously (one `PermanentsSacrificedEvent` per controller).
 */
val WhimsOfTheFates = card("Whims of the Fates") {
    manaCost = "{5}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Starting with you, each player separates all permanents they control into three " +
        "piles. Then each player chooses one of their piles at random and sacrifices those " +
        "permanents. (Piles can be empty.)"

    spell {
        effect = Effects.Pipeline(
            descriptionOverride = "Starting with you, each player separates all permanents they " +
                "control into three piles. Then each player chooses one of their piles at random " +
                "and sacrifices those permanents."
        ) {
            val (doomed) = forEachPlayerCollecting(Player.EachStartingWithYou) {
                val permanents = gather(CardSource.ControlledPermanents(Player.You))
                val (pile1, rest) = chooseAnyNumberSplit(
                    from = permanents,
                    selectedLabel = "Pile 1",
                    remainderLabel = "Piles 2 and 3",
                    prompt = "Separate your permanents into three piles. The permanents you " +
                        "select form Pile 1.",
                    useTargetingUI = true,
                    alwaysPrompt = true
                )
                val (pile2, pile3) = chooseAnyNumberSplit(
                    from = rest,
                    selectedLabel = "Pile 2",
                    remainderLabel = "Pile 3",
                    prompt = "The permanents you select form Pile 2; the rest form Pile 3.",
                    useTargetingUI = true,
                    alwaysPrompt = true
                )
                val (chosen, _) = choosePile(
                    pile1,
                    pile2,
                    additionalPiles = listOf(pile3),
                    atRandom = true
                )
                listOf(chosen)
            }
            sacrifice(doomed)
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "115"
        artist = "Seb McKinnon"
        imageUri = "https://cards.scryfall.io/normal/front/a/7/a7ef2fc3-0072-450b-acab-7d19c4903128.jpg?1783939539"

        ruling("2014-02-01", "To choose a pile at random, each pile must have an equal chance of being chosen. There are many ways to do this, including assigning each pile a number and rolling a die.")
        ruling("2014-02-01", "You may put Auras or Equipment into one pile and the creatures they were attached to into a different pile.")
        ruling("2014-02-01", "If you put all permanents you control into one pile, you'll have a two-thirds chance of not having to sacrifice any permanents, but a one-third chance of having to sacrifice all of them.")
    }
}

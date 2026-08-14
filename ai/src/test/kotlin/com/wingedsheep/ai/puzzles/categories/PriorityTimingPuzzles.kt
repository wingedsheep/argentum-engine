package com.wingedsheep.ai.puzzles.categories

import com.wingedsheep.ai.puzzles.AiPuzzle
import com.wingedsheep.ai.puzzles.PuzzleCategory
import com.wingedsheep.ai.puzzles.advanceToPriority
import com.wingedsheep.sdk.core.Step

/**
 * *Which window* — the half of Magic timing that is not about combat.
 *
 * Every other category asks "is this the right play?"; this one asks "is this the right **moment**
 * for a play that is right eventually". Three lessons, one pair each, and the pairing is what makes
 * them measurable: each pair holds the card fixed and moves only the window or the mana, so a
 * failure names the missing concept rather than a valuation disagreement.
 *
 * - **Last responsible moment** (01/02) — an instant kept in hand keeps its options; the opponent's
 *   end step is the last window before it stops being free to wait.
 * - **Don't tap out** (03/04) — the same hand is a hold at four lands and a double-spell at six.
 *   Purely arithmetic: what does the reactive card cost, and is there that much left over?
 * - **Spend the restricted card first** (05/06) — mana that survives to cleanup is mana wasted, and
 *   between a sorcery and an instant that answer the same threat, the sorcery is the one that can
 *   only be cast now.
 *
 * [HoldingInstantsPuzzles] is the combat-step neighbour of this category: it asks whether a trick
 * waits for blockers. Nothing here involves combat at all.
 */
object PriorityTimingPuzzles {

    fun all(): List<AiPuzzle> = listOf(

        AiPuzzle(
            id = "timing-01",
            category = PuzzleCategory.PRIORITY_TIMING,
            expectation = "Hold Murder in our own main phase — the Wall already holds the ground, and five open lands say a real threat is coming",
            aiSeat = 1,
            position = { scenario ->
                scenario.withPlayers()
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withCardInHand(1, "Murder")
                    // A 0/8 defender, so the 2/2 across the table is not attacking and we are not
                    // attacking either. Nothing about this turn is changed by killing it — which is
                    // the whole point: with no tempo on the line, the removal's only value is the
                    // option to answer whatever the five lands and three cards produce.
                    .withCardOnBattlefield(1, "Wall of Stone")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withLandsOnBattlefield(2, "Forest", 5)
                    .withCardsInHand(2, "Craw Wurm", 3)
                    .build()
            },
            check = { shouldNotCast("Murder") },
        ),

        AiPuzzle(
            id = "timing-02",
            category = PuzzleCategory.PRIORITY_TIMING,
            expectation = "Their end step is the last window before our turn — Murder the Serra Angel now",
            aiSeat = 1,
            position = { scenario ->
                scenario.withPlayers()
                    .withActivePlayer(2)
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withCardInHand(1, "Murder")
                    .withCardOnBattlefield(1, "Wall of Stone")
                    // A 4/4 flier the Wall cannot block, and an empty grip behind it: there is no
                    // better target coming and no later window. Waiting here just donates four
                    // damage next turn.
                    .withCardOnBattlefield(2, "Serra Angel")
                    .withLandsOnBattlefield(2, "Plains", 5)
                    .build()
                    .advanceToPriority(1, Step.END)
            },
            check = {
                shouldCast("Murder")
                shouldTarget("Serra Angel")
            },
        ),

        AiPuzzle(
            id = "timing-03",
            category = PuzzleCategory.PRIORITY_TIMING,
            expectation = "Four lands, Counterspell in hand: do not tap out for the Hill Giant",
            aiSeat = 1,
            position = { scenario ->
                scenario.withPlayers()
                    // Hill Giant is {3}{R}: casting it uses all four lands exactly, so the
                    // Counterspell in hand is a blank for the whole of the opponent's turn.
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withCardInHand(1, "Counterspell")
                    .withCardInHand(1, "Hill Giant")
                    .withCardOnBattlefield(1, "Wall of Stone")
                    .withLandsOnBattlefield(2, "Forest", 5)
                    .withCardsInHand(2, "Craw Wurm", 3)
                    .build()
            },
            check = { shouldNotCast("Hill Giant") },
        ),

        AiPuzzle(
            id = "timing-04",
            category = PuzzleCategory.PRIORITY_TIMING,
            expectation = "Six lands: cast the Hill Giant and still leave {U}{U} up for the Counterspell",
            aiSeat = 1,
            position = { scenario ->
                scenario.withPlayers()
                    // The positive control for timing-03 — the identical hand and board with two
                    // more Mountains. If 03 passes and this fails, the AI is not counting mana, it
                    // is refusing to spend it whenever it holds a reactive card.
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withLandsOnBattlefield(1, "Mountain", 4)
                    .withCardInHand(1, "Counterspell")
                    .withCardInHand(1, "Hill Giant")
                    .withCardOnBattlefield(1, "Wall of Stone")
                    .withLandsOnBattlefield(2, "Forest", 5)
                    .withCardsInHand(2, "Craw Wurm", 3)
                    .build()
            },
            check = { shouldCast("Hill Giant") },
        ),

        AiPuzzle(
            id = "timing-05",
            category = PuzzleCategory.PRIORITY_TIMING,
            expectation = "Cash the Opt in their end step — the mana is gone at cleanup either way",
            aiSeat = 1,
            position = { scenario ->
                scenario.withPlayers()
                    .withActivePlayer(2)
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withCardInHand(1, "Opt")
                    .build()
                    .advanceToPriority(1, Step.END)
            },
            // The deliberate mirror of instants-06, which asserts the *opposite* about the same
            // window: a pump spell cast at the end step is thrown away, a cantrip cast there is
            // free. An agent with a blanket end-step discount passes one and fails the other, and
            // that is exactly the distinction the pair exists to force.
            check = { shouldCast("Opt") },
        ),

        AiPuzzle(
            id = "timing-06",
            category = PuzzleCategory.PRIORITY_TIMING,
            expectation = "Two answers for one Angel: spend Fell, the one that can only be cast in a main phase",
            aiSeat = 1,
            position = { scenario ->
                scenario.withPlayers()
                    // Five Swamps pays for either (Fell {1}{B}, Murder {1}{B}{B}) and there is only
                    // one target, so this is not an affordability question — it is which card the
                    // turn structure lets us keep.
                    .withLandsOnBattlefield(1, "Swamp", 5)
                    .withCardInHand(1, "Fell")
                    .withCardInHand(1, "Murder")
                    .withCardOnBattlefield(2, "Serra Angel")
                    .build()
            },
            check = {
                shouldCast("Fell")
                shouldNotCast("Murder")
            },
        ),
    )
}

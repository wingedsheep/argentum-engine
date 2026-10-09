package com.wingedsheep.ai.puzzles.categories

import com.wingedsheep.ai.puzzles.AiPuzzle
import com.wingedsheep.ai.puzzles.PuzzleCategory

/**
 * Which play comes first in a main phase.
 *
 * The probe is a single action, so these are all "of the plays available right now, which one?" —
 * land before spell, the land that produces the colour you need, the spell that uses the mana you
 * have.
 */
object SequencingPuzzles {

    fun all(): List<AiPuzzle> = listOf(

        AiPuzzle(
            id = "sequencing-01",
            category = PuzzleCategory.SEQUENCING,
            expectation = "Play the fourth land first — it is what makes the 4-drop castable this turn",
            aiSeat = 1,
            position = { scenario ->
                scenario.withPlayers()
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    .withCardInHand(1, "Mountain")
                    .withCardInHand(1, "Hill Giant")
                    .build()
            },
            check = { shouldPlayLand("Mountain") },
        ),

        AiPuzzle(
            id = "sequencing-02",
            category = PuzzleCategory.SEQUENCING,
            expectation = "On the last card in hand, still make the land drop",
            aiSeat = 1,
            position = { scenario ->
                scenario.withPlayers()
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withCardInHand(1, "Forest")
                    .build()
            },
            // Deliberately paired with sequencing-04, which is the same decision with one more
            // card in hand. If 04 passes and this fails, the defect is precisely
            // `CardAdvantage.cardValue(0) = -3.0`: emptying your hand reads as a disaster, so the
            // AI would rather hold a land forever than play it. Land drops are free — which is what
            // `AiProfile.landDropIsNotCardLoss` finally tells the evaluator.
            check = { shouldPlayLand("Forest") },
        ),

        AiPuzzle(
            id = "sequencing-03",
            category = PuzzleCategory.SEQUENCING,
            expectation = "Four lands up: cast the 3/3, not the 2/2",
            aiSeat = 1,
            position = { scenario ->
                scenario.withPlayers()
                    .withLandsOnBattlefield(1, "Mountain", 4)
                    .withCardInHand(1, "Hill Giant")
                    .withCardInHand(1, "Gray Ogre")
                    .build()
            },
            check = { shouldCast("Hill Giant") },
        ),

        AiPuzzle(
            id = "sequencing-04",
            category = PuzzleCategory.SEQUENCING,
            expectation = "Develop the board rather than pass with mana available",
            aiSeat = 1,
            position = { scenario ->
                scenario.withPlayers()
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withCardInHand(1, "Grizzly Bears")
                    // A second, uncastable card so this is *not* the last-card-in-hand cliff that
                    // sequencing-02 isolates. Same decision, one card of slack.
                    .withCardInHand(1, "Craw Wurm")
                    .build()
            },
            check = { shouldCast("Grizzly Bears") },
        ),

        AiPuzzle(
            id = "sequencing-05",
            category = PuzzleCategory.SEQUENCING,
            expectation = "Anthem before combat: it turns two 2/2s into exactly lethal",
            aiSeat = 1,
            position = { scenario ->
                scenario.withPlayers()
                    .withLandsOnBattlefield(1, "Plains", 3)
                    .withCardInHand(1, "Glorious Anthem")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withLifeTotal(2, 6)
                    .build()
            },
            check = { shouldCast("Glorious Anthem") },
        ),

        AiPuzzle(
            id = "sequencing-06",
            category = PuzzleCategory.SEQUENCING,
            expectation = "Play the Mountain, not the Forest — red is the colour the hand needs",
            aiSeat = 1,
            position = { scenario ->
                scenario.withPlayers()
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withCardInHand(1, "Mountain")
                    .withCardInHand(1, "Forest")
                    .withCardInHand(1, "Lightning Bolt")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .build()
            },
            check = { shouldPlayLand("Mountain") },
        ),

        // ── Land *order*. Two lands in hand, one of them tapped; only the order is in question. ──
        //
        // These are the first puzzles here whose wrong answer costs a turn rather than a tempo
        // point, and they are deliberately a pair: the same two lands, the same two-Mountain board,
        // and opposite correct answers, differing only in the top of the curve. No constant
        // preference between the two lands solves both — which is the point, because a constant is
        // exactly what the AI has. `BoardPresence` scores an untapped land 0.6 and a tapped one 0.3,
        // so the basic wins by a flat +0.3 whether or not the mana it unlocks is live this turn:
        // 08 passes, 07 fails. Verified order-insensitive — swapping the two lands in hand moves
        // neither verdict, so this is the evaluator answering, not a tie-break.

        AiPuzzle(
            id = "sequencing-07",
            category = PuzzleCategory.SEQUENCING,
            expectation = "Nothing to cast at three mana: play the tapland now, so the Mountain " +
                "arrives untapped on the turn the 4-drop needs it",
            aiSeat = 1,
            position = { scenario ->
                scenario.withPlayers()
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withCardInHand(1, "Mountain")
                    .withCardInHand(1, "Shivan Oasis")
                    .withCardInHand(1, "Hill Giant")
                    .build()
            },
            // The third mana is dead this turn — Hill Giant is {3}{R} — so the tapland's drawback
            // costs nothing *now* and everything later. Oasis first: next turn the Mountain enters
            // untapped and all four lands pay for the Giant. Mountain first: next turn the Oasis
            // enters tapped, leaving three usable mana, and the Giant slips a whole turn.
            check = { shouldPlayLand("Shivan Oasis") },
        ),

        AiPuzzle(
            id = "sequencing-08",
            category = PuzzleCategory.SEQUENCING,
            expectation = "The 3-drop is castable this turn only off the basic — play the Mountain, " +
                "not the tapland",
            aiSeat = 1,
            position = { scenario ->
                scenario.withPlayers()
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withCardInHand(1, "Mountain")
                    .withCardInHand(1, "Shivan Oasis")
                    .withCardInHand(1, "Gray Ogre")
                    .build()
            },
            // sequencing-07 with the curve pulled down one: Gray Ogre is {2}{R}, so the third mana
            // is live the moment it is untapped. Here the tapland is the turn-losing play.
            check = { shouldPlayLand("Mountain") },
        ),

        // ── Hoarding a castable permanent. sequencing-02's cliff, for a spell instead of a land. ──
        //
        // A sorcery-speed permanent with the mana to cast it has no better window to wait for, yet
        // the hand curve charges casting it as card loss: 4.0 for the last card at the default
        // −3.0 (3.0 at `concave-hand-2`), 1.5 for the second. A summoning-sick 1/1 or a mana rock is
        // worth less than that on the board, so passing won — the AI held Wose Pathfinder with ten
        // lands for five turns while it died (2026-10-09 logs, game 21).
        // `AiProfile.permanentCastIsNotCardLoss` refunds the charge for exactly this shape.

        AiPuzzle(
            id = "sequencing-09",
            category = PuzzleCategory.SEQUENCING,
            expectation = "On the last card in hand, deploy the 1/1 — six lands are idle and there " +
                "is no better window for a creature",
            aiSeat = 1,
            position = { scenario ->
                scenario.withPlayers()
                    .withLandsOnBattlefield(1, "Forest", 6)
                    .withCardInHand(1, "Llanowar Elves")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .build()
            },
            check = { shouldCast("Llanowar Elves") },
        ),

        AiPuzzle(
            id = "sequencing-10",
            category = PuzzleCategory.SEQUENCING,
            expectation = "Cast the mana rock now; the six-drop beside it is out of reach either way",
            aiSeat = 1,
            position = { scenario ->
                scenario.withPlayers()
                    .withLandsOnBattlefield(1, "Island", 4)
                    .withCardInHand(1, "Dragonstorm Globe")
                    .withCardInHand(1, "Craw Wurm")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .build()
            },
            // One card of slack, so this is the second-card charge (1.5), not sequencing-09's cliff.
            check = { shouldCast("Dragonstorm Globe") },
        ),
    )
}

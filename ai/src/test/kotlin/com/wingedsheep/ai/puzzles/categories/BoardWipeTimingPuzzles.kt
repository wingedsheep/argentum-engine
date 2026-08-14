package com.wingedsheep.ai.puzzles.categories

import com.wingedsheep.ai.puzzles.AiPuzzle
import com.wingedsheep.ai.puzzles.PuzzleCategory

/**
 * A sweeper is only good when you are behind on board.
 *
 * These are deliberately built on generic sweepers (Wrath of God, Day of Judgment) rather than the
 * Bloomburrow cards `BoardWipeAdvisor` names, so the category measures the *general* mechanism.
 * The advisor is a per-card override on top; if it is doing work, the `production` profile beats
 * `v0` here and `PuzzleReport` shows it.
 */
object BoardWipeTimingPuzzles {

    fun all(): List<AiPuzzle> = listOf(

        AiPuzzle(
            id = "wipe-01",
            category = PuzzleCategory.BOARD_WIPE_TIMING,
            expectation = "Do not Wrath while ahead three creatures to one",
            aiSeat = 1,
            position = { scenario ->
                scenario.withPlayers()
                    .withLandsOnBattlefield(1, "Plains", 4)
                    .withCardInHand(1, "Wrath of God")
                    .withCardOnBattlefield(1, "Hill Giant")
                    .withCardOnBattlefield(1, "Hill Giant")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .build()
            },
            check = { shouldNotCast("Wrath of God") },
        ),

        AiPuzzle(
            id = "wipe-02",
            category = PuzzleCategory.BOARD_WIPE_TIMING,
            expectation = "Wrath while behind zero creatures to three",
            aiSeat = 1,
            position = { scenario ->
                scenario.withPlayers()
                    .withLandsOnBattlefield(1, "Plains", 4)
                    .withCardInHand(1, "Wrath of God")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withCardOnBattlefield(2, "Craw Wurm")
                    .build()
            },
            check = { shouldCast("Wrath of God") },
        ),

        AiPuzzle(
            id = "wipe-03",
            category = PuzzleCategory.BOARD_WIPE_TIMING,
            expectation = "Do not Wrath our own two creatures away when the opponent has none",
            aiSeat = 1,
            position = { scenario ->
                scenario.withPlayers()
                    .withLandsOnBattlefield(1, "Plains", 4)
                    .withCardInHand(1, "Wrath of God")
                    .withCardOnBattlefield(1, "Hill Giant")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .build()
            },
            check = { shouldNotCast("Wrath of God") },
        ),

        AiPuzzle(
            id = "wipe-04",
            category = PuzzleCategory.BOARD_WIPE_TIMING,
            expectation = "Day of Judgment at 4 life facing nine power — the sweeper is the only out",
            aiSeat = 1,
            position = { scenario ->
                scenario.withPlayers()
                    .withLandsOnBattlefield(1, "Plains", 4)
                    .withCardInHand(1, "Day of Judgment")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withLifeTotal(1, 4)
                    .build()
            },
            check = { shouldCast("Day of Judgment") },
        ),

        AiPuzzle(
            id = "wipe-05",
            category = PuzzleCategory.BOARD_WIPE_TIMING,
            expectation = "Do not trade a 6/4 and a 4/4 flier for one 2/2",
            aiSeat = 1,
            position = { scenario ->
                scenario.withPlayers()
                    .withLandsOnBattlefield(1, "Plains", 4)
                    .withCardInHand(1, "Day of Judgment")
                    .withCardOnBattlefield(1, "Craw Wurm")
                    .withCardOnBattlefield(1, "Air Elemental")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .build()
            },
            check = { shouldNotCast("Day of Judgment") },
        ),

        AiPuzzle(
            id = "wipe-06",
            category = PuzzleCategory.BOARD_WIPE_TIMING,
            expectation = "Sweep when the trade is one 2/2 of ours for three of theirs",
            aiSeat = 1,
            position = { scenario ->
                scenario.withPlayers()
                    .withLandsOnBattlefield(1, "Plains", 4)
                    .withCardInHand(1, "Day of Judgment")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Craw Wurm")
                    .withCardOnBattlefield(2, "Air Elemental")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .build()
            },
            check = { shouldCast("Day of Judgment") },
        ),
    )
}

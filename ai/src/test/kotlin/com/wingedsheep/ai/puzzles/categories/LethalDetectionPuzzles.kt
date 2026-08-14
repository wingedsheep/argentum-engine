package com.wingedsheep.ai.puzzles.categories

import com.wingedsheep.ai.puzzles.AiPuzzle
import com.wingedsheep.ai.puzzles.PuzzleCategory
import com.wingedsheep.ai.puzzles.advanceToDeclaration
import com.wingedsheep.sdk.core.Step

/**
 * Can the AI see the kill that is already on the table?
 *
 * Every position here is winnable *this turn* by a line the AI is one action away from. There is
 * no card advantage to weigh and no crack-back to fear: if it misses these, it is not counting.
 */
object LethalDetectionPuzzles {

    fun all(): List<AiPuzzle> = listOf(

        AiPuzzle(
            id = "lethal-01",
            category = PuzzleCategory.LETHAL_DETECTION,
            expectation = "Alpha strike for exactly lethal into an empty board",
            aiSeat = 1,
            position = { scenario ->
                scenario.withPlayers()
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withLifeTotal(2, 6)
                    .build()
                    .advanceToDeclaration(1, Step.DECLARE_ATTACKERS)
            },
            check = { shouldAttackForAtLeast(6) },
        ),

        AiPuzzle(
            id = "lethal-02",
            category = PuzzleCategory.LETHAL_DETECTION,
            expectation = "Attack with everything: one blocker cannot stop 9 power against 6 life",
            aiSeat = 1,
            position = { scenario ->
                scenario.withPlayers()
                    .withCardOnBattlefield(1, "Hill Giant")
                    .withCardOnBattlefield(1, "Hill Giant")
                    .withCardOnBattlefield(1, "Hill Giant")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withLifeTotal(2, 6)
                    .build()
                    .advanceToDeclaration(1, Step.DECLARE_ATTACKERS)
            },
            // Holding one back leaves at most 6 unblocked, and the blocker eats 3 of it.
            check = { shouldAttackWithExactly("Hill Giant", "Hill Giant", "Hill Giant") },
        ),

        AiPuzzle(
            id = "lethal-03",
            category = PuzzleCategory.LETHAL_DETECTION,
            expectation = "Bolt the opponent for the win instead of a creature the Bolt cannot kill",
            aiSeat = 1,
            position = { scenario ->
                scenario.withPlayers()
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withCardInHand(1, "Lightning Bolt")
                    .withCardOnBattlefield(2, "Craw Wurm")
                    .withLifeTotal(2, 3)
                    .build()
            },
            // The Wurm is a 6/4: three damage bounces off it. The opponent is at 3.
            check = {
                shouldCast("Lightning Bolt")
                shouldTargetOpponentFace()
            },
        ),

        AiPuzzle(
            id = "lethal-04",
            category = PuzzleCategory.LETHAL_DETECTION,
            expectation = "Cast Lava Axe for exactly lethal from an empty board",
            aiSeat = 1,
            position = { scenario ->
                scenario.withPlayers()
                    .withLandsOnBattlefield(1, "Mountain", 5)
                    .withCardInHand(1, "Lava Axe")
                    .withLifeTotal(2, 5)
                    .build()
            },
            check = { shouldCast("Lava Axe") },
        ),

        AiPuzzle(
            id = "lethal-05",
            category = PuzzleCategory.LETHAL_DETECTION,
            expectation = "Swing both Wurms for 12 into an empty board at 10 life",
            aiSeat = 1,
            position = { scenario ->
                scenario.withPlayers()
                    .withCardOnBattlefield(1, "Craw Wurm")
                    .withCardOnBattlefield(1, "Craw Wurm")
                    .withLifeTotal(2, 10)
                    .build()
                    .advanceToDeclaration(1, Step.DECLARE_ATTACKERS)
            },
            check = { shouldAttackForAtLeast(10) },
        ),

        AiPuzzle(
            id = "lethal-06",
            category = PuzzleCategory.LETHAL_DETECTION,
            expectation = "Lethal in the air: the ground blocker cannot touch either Drake",
            aiSeat = 1,
            position = { scenario ->
                scenario.withPlayers()
                    .withCardOnBattlefield(1, "Wind Drake")
                    .withCardOnBattlefield(1, "Wind Drake")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withLifeTotal(2, 4)
                    .build()
                    .advanceToDeclaration(1, Step.DECLARE_ATTACKERS)
            },
            check = { shouldAttackWithExactly("Wind Drake", "Wind Drake") },
        ),
    )
}

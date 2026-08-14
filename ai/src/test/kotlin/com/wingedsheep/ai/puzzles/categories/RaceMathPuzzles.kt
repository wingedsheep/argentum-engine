package com.wingedsheep.ai.puzzles.categories

import com.wingedsheep.ai.puzzles.AiPuzzle
import com.wingedsheep.ai.puzzles.PuzzleCategory
import com.wingedsheep.ai.puzzles.advanceToDeclaration
import com.wingedsheep.sdk.core.Step

/**
 * Attack-vs-hold when both players are on a clock.
 *
 * The distinguishing feature from [LethalDetectionPuzzles] is that no line wins on the spot: every
 * position trades this turn's damage against next turn's crack-back, which is precisely what a
 * greedy one-ply evaluator has no machinery to weigh.
 */
object RaceMathPuzzles {

    fun all(): List<AiPuzzle> = listOf(

        AiPuzzle(
            id = "race-01",
            category = PuzzleCategory.RACE_MATH,
            expectation = "Keep the only blocker home: attacking loses to the 6/4 crack-back at 3 life",
            aiSeat = 1,
            position = { scenario ->
                scenario.withPlayers()
                    .withCardOnBattlefield(1, "Hill Giant")
                    .withCardOnBattlefield(2, "Craw Wurm")
                    .withLifeTotal(1, 3)
                    .build()
                    .advanceToDeclaration(1, Step.DECLARE_ATTACKERS)
            },
            check = { shouldNotAttack() },
        ),

        AiPuzzle(
            id = "race-02",
            category = PuzzleCategory.RACE_MATH,
            expectation = "Attack: an empty opposing board cannot punish it, and we are the one on a clock",
            aiSeat = 1,
            position = { scenario ->
                scenario.withPlayers()
                    .withCardOnBattlefield(1, "Hill Giant")
                    .withLifeTotal(1, 3)
                    .build()
                    .advanceToDeclaration(1, Step.DECLARE_ATTACKERS)
            },
            check = { shouldAttackWithExactly("Hill Giant") },
        ),

        AiPuzzle(
            id = "race-03",
            category = PuzzleCategory.RACE_MATH,
            expectation = "Send the flier, keep the ground creature home to block the 3/3",
            aiSeat = 1,
            position = { scenario ->
                scenario.withPlayers()
                    .withCardOnBattlefield(1, "Wind Drake")
                    .withCardOnBattlefield(1, "Hill Giant")
                    .withCardOnBattlefield(2, "Trained Armodon")
                    .withLifeTotal(1, 5)
                    .withLifeTotal(2, 5)
                    .build()
                    .advanceToDeclaration(1, Step.DECLARE_ATTACKERS)
            },
            check = { shouldAttackWithExactly("Wind Drake") },
        ),

        AiPuzzle(
            id = "race-04",
            category = PuzzleCategory.RACE_MATH,
            expectation = "Vigilance makes attacking free: Serra Angel swings and still blocks",
            aiSeat = 1,
            position = { scenario ->
                scenario.withPlayers()
                    .withCardOnBattlefield(1, "Serra Angel")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withLifeTotal(1, 4)
                    .withLifeTotal(2, 8)
                    .build()
                    .advanceToDeclaration(1, Step.DECLARE_ATTACKERS)
            },
            check = { shouldAttackWithExactly("Serra Angel") },
        ),

        AiPuzzle(
            id = "race-05",
            category = PuzzleCategory.RACE_MATH,
            expectation = "Do not attack a 2/2 into an untapped 3/3 with both players at 20",
            aiSeat = 1,
            position = { scenario ->
                scenario.withPlayers()
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .build()
                    .advanceToDeclaration(1, Step.DECLARE_ATTACKERS)
            },
            check = { shouldNotAttack() },
        ),

        AiPuzzle(
            id = "race-06",
            category = PuzzleCategory.RACE_MATH,
            expectation = "The 3/3 is tapped, so the 2/2 gets in for free",
            aiSeat = 1,
            position = { scenario ->
                scenario.withPlayers()
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Hill Giant", tapped = true)
                    .build()
                    .advanceToDeclaration(1, Step.DECLARE_ATTACKERS)
            },
            check = { shouldAttackWithExactly("Grizzly Bears") },
        ),
    )
}

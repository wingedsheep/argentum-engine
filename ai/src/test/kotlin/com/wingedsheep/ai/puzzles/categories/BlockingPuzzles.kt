package com.wingedsheep.ai.puzzles.categories

import com.wingedsheep.ai.puzzles.AiPuzzle
import com.wingedsheep.ai.puzzles.PuzzleCategory
import com.wingedsheep.ai.puzzles.advanceToDeclaration
import com.wingedsheep.sdk.core.Step

/**
 * Chump vs trade vs no-block, and whether the combat keywords are read at all.
 *
 * The AI defends from seat 2 in every one of these; seat 1's attack is scripted by the position so
 * the only judgement being measured is the block.
 */
object BlockingPuzzles {

    fun all(): List<AiPuzzle> = listOf(

        AiPuzzle(
            id = "blocking-01",
            category = PuzzleCategory.BLOCKING,
            expectation = "Chump-block the 6/4 rather than die at 3 life",
            aiSeat = 2,
            position = { scenario ->
                scenario.withPlayers()
                    .withCardOnBattlefield(1, "Craw Wurm")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withLifeTotal(2, 3)
                    .build()
                    .advanceToDeclaration(1, Step.DECLARE_ATTACKERS)
                    .also { it.declareAttackers(mapOf("Craw Wurm" to 2)) }
                    .advanceToDeclaration(2, Step.DECLARE_BLOCKERS)
            },
            check = { shouldBlock("Grizzly Bears", "Craw Wurm") },
        ),

        AiPuzzle(
            id = "blocking-02",
            category = PuzzleCategory.BLOCKING,
            expectation = "Do not chump at 20 life: the Bears would die for three damage",
            aiSeat = 2,
            position = { scenario ->
                scenario.withPlayers()
                    .withCardOnBattlefield(1, "Hill Giant")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .build()
                    .advanceToDeclaration(1, Step.DECLARE_ATTACKERS)
                    .also { it.declareAttackers(mapOf("Hill Giant" to 2)) }
                    .advanceToDeclaration(2, Step.DECLARE_BLOCKERS)
            },
            check = { shouldNotBlock() },
        ),

        AiPuzzle(
            id = "blocking-03",
            category = PuzzleCategory.BLOCKING,
            expectation = "Block the 2/2 with the 3/3: it eats the attacker and lives",
            aiSeat = 2,
            position = { scenario ->
                scenario.withPlayers()
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .build()
                    .advanceToDeclaration(1, Step.DECLARE_ATTACKERS)
                    .also { it.declareAttackers(mapOf("Grizzly Bears" to 2)) }
                    .advanceToDeclaration(2, Step.DECLARE_BLOCKERS)
            },
            check = { shouldBlock("Hill Giant", "Grizzly Bears") },
        ),

        AiPuzzle(
            id = "blocking-04",
            category = PuzzleCategory.BLOCKING,
            expectation = "Deathtouch: a 2/1 Viper trades with a 6/4 Wurm",
            aiSeat = 2,
            position = { scenario ->
                scenario.withPlayers()
                    .withCardOnBattlefield(1, "Craw Wurm")
                    .withCardOnBattlefield(2, "Ambush Viper")
                    .build()
                    .advanceToDeclaration(1, Step.DECLARE_ATTACKERS)
                    .also { it.declareAttackers(mapOf("Craw Wurm" to 2)) }
                    .advanceToDeclaration(2, Step.DECLARE_BLOCKERS)
            },
            check = { shouldBlock("Ambush Viper", "Craw Wurm") },
        ),

        AiPuzzle(
            id = "blocking-05",
            category = PuzzleCategory.BLOCKING,
            expectation = "First strike: White Knight blocks the 2/2 and kills it for free",
            aiSeat = 2,
            position = { scenario ->
                scenario.withPlayers()
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(2, "White Knight")
                    .build()
                    .advanceToDeclaration(1, Step.DECLARE_ATTACKERS)
                    .also { it.declareAttackers(mapOf("Grizzly Bears" to 2)) }
                    .advanceToDeclaration(2, Step.DECLARE_BLOCKERS)
            },
            check = { shouldBlock("White Knight", "Grizzly Bears") },
        ),

        AiPuzzle(
            id = "blocking-06",
            category = PuzzleCategory.BLOCKING,
            expectation = "At 3 life with one blocker, block the 6/4 — the 2/2 is survivable",
            aiSeat = 2,
            position = { scenario ->
                scenario.withPlayers()
                    .withCardOnBattlefield(1, "Craw Wurm")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withLifeTotal(2, 3)
                    .build()
                    .advanceToDeclaration(1, Step.DECLARE_ATTACKERS)
                    .also { it.declareAttackers(mapOf("Craw Wurm" to 2, "Grizzly Bears" to 2)) }
                    .advanceToDeclaration(2, Step.DECLARE_BLOCKERS)
            },
            check = { shouldBlock("Hill Giant", "Craw Wurm") },
        ),

        // The two below are the same mistake from two directions: a block chosen for what it *wins*
        // in a turn that does not come. Both were found by a randomised sweep over vanilla boards
        // that asked one question — "was there a legal set of blocks that survives, and did the AI
        // find one?" — and both killed the AI at three life with the answer sitting on the table.
        AiPuzzle(
            id = "blocking-07",
            category = PuzzleCategory.BLOCKING,
            expectation = "Three attackers, three blockers, 3 life: one body in front of each, " +
                "not two on the Wurm while the Giant walks in for exact lethal",
            aiSeat = 2,
            position = { scenario ->
                scenario.withPlayers()
                    .withCardOnBattlefield(1, "Craw Wurm")        // 6/4
                    .withCardOnBattlefield(1, "Hill Giant")       // 3/3
                    .withCardOnBattlefield(1, "Ordinary Bear")    // 4/5
                    .withCardOnBattlefield(2, "Trained Armodon")  // 3/3
                    .withCardOnBattlefield(2, "Grizzly Bears")    // 2/2
                    .withCardOnBattlefield(2, "Alpha Myr")        // 2/1
                    .withLifeTotal(2, 3)
                    .build()
                    .advanceToDeclaration(1, Step.DECLARE_ATTACKERS)
                    .also {
                        it.declareAttackers(
                            mapOf("Craw Wurm" to 2, "Hill Giant" to 2, "Ordinary Bear" to 2)
                        )
                    }
                    .advanceToDeclaration(2, Step.DECLARE_BLOCKERS)
            },
            check = {
                shouldBlockWithAtLeast(1, "Craw Wurm")
                shouldBlockWithAtLeast(1, "Hill Giant")
                shouldBlockWithAtLeast(1, "Ordinary Bear")
            },
        ),

        AiPuzzle(
            id = "blocking-08",
            category = PuzzleCategory.BLOCKING,
            expectation = "At 3 life the free kill on the 2/2 is a luxury: the two blockers belong " +
                "in front of the 6/4 and the 4/5, taking 2 instead of 4",
            aiSeat = 2,
            position = { scenario ->
                scenario.withPlayers()
                    .withCardOnBattlefield(1, "Craw Wurm")        // 6/4
                    .withCardOnBattlefield(1, "Grizzly Bears")    // 2/2
                    .withCardOnBattlefield(1, "Ordinary Bear")    // 4/5
                    .withCardOnBattlefield(2, "Trained Armodon")  // 3/3
                    .withCardOnBattlefield(2, "Alpha Myr")        // 2/1
                    .withLifeTotal(2, 3)
                    .build()
                    .advanceToDeclaration(1, Step.DECLARE_ATTACKERS)
                    .also {
                        it.declareAttackers(
                            mapOf("Craw Wurm" to 2, "Grizzly Bears" to 2, "Ordinary Bear" to 2)
                        )
                    }
                    .advanceToDeclaration(2, Step.DECLARE_BLOCKERS)
            },
            check = {
                shouldBlockWithAtLeast(1, "Craw Wurm")
                shouldBlockWithAtLeast(1, "Ordinary Bear")
            },
        ),
    )
}

package com.wingedsheep.ai.puzzles.categories

import com.wingedsheep.ai.puzzles.AiPuzzle
import com.wingedsheep.ai.puzzles.PuzzleCategory

/**
 * Does the AI see anything that is not a creature?
 *
 * `BoardFeatures.permanentValue` flat-values every non-creature permanent at `0.5` regardless of
 * text, and `Strategist.heuristicTargetRank` returns `0.0` for one — so a mana rock, a repeatable
 * tapper and an anthem are the same number, and none of them outranks nothing. This category is
 * the plan's canary for **Phase 6 (`CardIntent`)**: it is expected to be the worst-scoring category
 * today, and its recovery is that phase's exit criterion.
 */
object NonCreatureValuationPuzzles {

    fun all(): List<AiPuzzle> = listOf(

        AiPuzzle(
            id = "noncreature-01",
            category = PuzzleCategory.NON_CREATURE_VALUATION,
            expectation = "Disenchant the opposing Icy Manipulator at all — it is the only artifact in play",
            aiSeat = 1,
            position = { scenario ->
                scenario.withPlayers()
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withCardInHand(1, "Disenchant")
                    .withCardOnBattlefield(2, "Icy Manipulator")
                    .build()
            },
            check = {
                shouldCast("Disenchant")
                shouldTarget("Icy Manipulator")
            },
        ),

        AiPuzzle(
            id = "noncreature-02",
            category = PuzzleCategory.NON_CREATURE_VALUATION,
            expectation = "Disenchant points at their anthem, never at our own mana rock",
            aiSeat = 1,
            position = { scenario ->
                scenario.withPlayers()
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withCardInHand(1, "Disenchant")
                    .withCardOnBattlefield(1, "Mind Stone")
                    .withCardOnBattlefield(2, "Glorious Anthem")
                    .build()
            },
            check = {
                shouldCast("Disenchant")
                shouldTarget("Glorious Anthem")
                shouldNotTarget("Mind Stone")
            },
        ),

        AiPuzzle(
            id = "noncreature-03",
            category = PuzzleCategory.NON_CREATURE_VALUATION,
            expectation = "Naturalize the opposing Banishing Light",
            aiSeat = 1,
            position = { scenario ->
                scenario.withPlayers()
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withCardInHand(1, "Naturalize")
                    .withCardOnBattlefield(2, "Banishing Light")
                    .build()
            },
            check = {
                shouldCast("Naturalize")
                shouldTarget("Banishing Light")
            },
        ),

        AiPuzzle(
            id = "noncreature-04",
            category = PuzzleCategory.NON_CREATURE_VALUATION,
            expectation = "Kill the repeatable tapper, not the mana rock",
            aiSeat = 1,
            position = { scenario ->
                scenario.withPlayers()
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withCardInHand(1, "Disenchant")
                    .withCardOnBattlefield(2, "Icy Manipulator")
                    .withCardOnBattlefield(2, "Mind Stone")
                    .build()
            },
            check = {
                shouldCast("Disenchant")
                shouldTarget("Icy Manipulator")
            },
        ),

        AiPuzzle(
            id = "noncreature-05",
            category = PuzzleCategory.NON_CREATURE_VALUATION,
            expectation = "The anthem pumping three creatures beats the inert artifact as a target",
            aiSeat = 1,
            position = { scenario ->
                scenario.withPlayers()
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withCardInHand(1, "Disenchant")
                    .withCardOnBattlefield(2, "Glorious Anthem")
                    .withCardOnBattlefield(2, "Mind Stone")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .build()
            },
            check = {
                shouldCast("Disenchant")
                shouldTarget("Glorious Anthem")
            },
        ),

        AiPuzzle(
            id = "noncreature-06",
            category = PuzzleCategory.NON_CREATURE_VALUATION,
            expectation = "Disenchant the Pacifism off our own 6/4",
            aiSeat = 1,
            position = { scenario ->
                scenario.withPlayers()
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withCardInHand(1, "Disenchant")
                    .withCardOnBattlefield(1, "Craw Wurm")
                    .withCardAttachedTo(2, "Pacifism", "Craw Wurm")
                    .build()
            },
            check = {
                shouldCast("Disenchant")
                shouldTarget("Pacifism")
            },
        ),
    )
}

package com.wingedsheep.ai.puzzles.categories

import com.wingedsheep.ai.puzzles.AiPuzzle
import com.wingedsheep.ai.puzzles.PuzzleCategory

/**
 * Where the removal spell points.
 *
 * This is the category `Strategist.heuristicTargetRank` owns, plus the simulation refinement in
 * `chooseCommittedTargets` layered on top of it. Positions are built so that the *wrong* target is
 * the one raw creature value would pick.
 */
object RemovalTargetingPuzzles {

    fun all(): List<AiPuzzle> = listOf(

        AiPuzzle(
            id = "removal-01",
            category = PuzzleCategory.REMOVAL_TARGETING,
            expectation = "Murder the 6/4, not the 2/2",
            aiSeat = 1,
            position = { scenario ->
                scenario.withPlayers()
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withCardInHand(1, "Murder")
                    .withCardOnBattlefield(2, "Craw Wurm")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .build()
            },
            check = {
                shouldCast("Murder")
                shouldTarget("Craw Wurm")
            },
        ),

        AiPuzzle(
            id = "removal-02",
            category = PuzzleCategory.REMOVAL_TARGETING,
            expectation = "Bolt the 3/3 it can kill, not the 6/4 it bounces off",
            aiSeat = 1,
            position = { scenario ->
                scenario.withPlayers()
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withCardInHand(1, "Lightning Bolt")
                    .withCardOnBattlefield(2, "Craw Wurm")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .build()
            },
            check = {
                shouldCast("Lightning Bolt")
                shouldTarget("Hill Giant")
            },
        ),

        AiPuzzle(
            id = "removal-03",
            category = PuzzleCategory.REMOVAL_TARGETING,
            expectation = "Do not spend removal on a creature Pacifism has already neutralized",
            aiSeat = 1,
            position = { scenario ->
                scenario.withPlayers()
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withCardInHand(1, "Murder")
                    .withCardOnBattlefield(2, "Craw Wurm")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withCardAttachedTo(2, "Pacifism", "Craw Wurm")
                    .build()
            },
            check = {
                shouldCast("Murder")
                shouldTarget("Hill Giant")
            },
        ),

        AiPuzzle(
            id = "removal-04",
            category = PuzzleCategory.REMOVAL_TARGETING,
            expectation = "Removal points at the opponent's creature, never at our own bigger one",
            aiSeat = 1,
            position = { scenario ->
                scenario.withPlayers()
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withCardInHand(1, "Murder")
                    .withCardOnBattlefield(1, "Craw Wurm")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .build()
            },
            check = {
                shouldCast("Murder")
                shouldTarget("Hill Giant")
                shouldNotTarget("Craw Wurm")
            },
        ),

        AiPuzzle(
            id = "removal-05",
            category = PuzzleCategory.REMOVAL_TARGETING,
            expectation = "At 4 life, Bolt the 3/3 that is killing us rather than the opponent's face",
            aiSeat = 1,
            position = { scenario ->
                scenario.withPlayers()
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withCardInHand(1, "Lightning Bolt")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withLifeTotal(1, 4)
                    .build()
            },
            check = {
                shouldCast("Lightning Bolt")
                shouldTarget("Hill Giant")
            },
        ),

        AiPuzzle(
            id = "removal-06",
            category = PuzzleCategory.REMOVAL_TARGETING,
            expectation = "Kill the flier we cannot block, not the bigger creature our Giants hold off",
            aiSeat = 1,
            position = { scenario ->
                scenario.withPlayers()
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withCardInHand(1, "Murder")
                    .withCardOnBattlefield(1, "Hill Giant")
                    .withCardOnBattlefield(1, "Hill Giant")
                    .withCardOnBattlefield(2, "Air Elemental")
                    .withCardOnBattlefield(2, "Craw Wurm")
                    .withLifeTotal(1, 6)
                    .build()
            },
            check = {
                shouldCast("Murder")
                shouldTarget("Air Elemental")
            },
        ),

        // ── Is the target worth the card at all? ──
        // 01–06 all assume the removal is being cast and ask where it points. These three ask the
        // question before that one, and they are a triple rather than a pair because "hold it" is
        // only defensible if both of its exits are pinned too. Same board every time; only the
        // hand size and the turn number move.

        AiPuzzle(
            id = "removal-07",
            category = PuzzleCategory.REMOVAL_TARGETING,
            expectation = "Hold Murder rather than spend it on a 1/1 with three cards of slack in hand",
            aiSeat = 1,
            position = { scenario ->
                scenario.withPlayers()
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withCardInHand(1, "Murder")
                    // Slack, deliberately uncastable off Swamps: without it, emptying the hand costs
                    // 4.0 of card advantage and the AI holds the Murder for a reason that has
                    // nothing to do with the target — which would make this puzzle pass blind.
                    .withCardsInHand(1, "Craw Wurm", 3)
                    // A 0/8 defender, so the 1/1 is neither attacking nor blocking anything that
                    // matters. Nothing about this turn changes by killing it.
                    .withCardOnBattlefield(1, "Wall of Stone")
                    .withCardOnBattlefield(2, "Mons's Goblin Raiders")
                    .withLandsOnBattlefield(2, "Mountain", 5)
                    .withCardsInHand(2, "Craw Wurm", 3)
                    .build()
            },
            check = { shouldNotCast("Murder") },
        ),

        AiPuzzle(
            id = "removal-08",
            category = PuzzleCategory.REMOVAL_TARGETING,
            expectation = "Same 1/1, but the hand is at the discard limit — spend the Murder rather than pitch a card",
            aiSeat = 1,
            position = { scenario ->
                scenario.withPlayers()
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withCardInHand(1, "Murder")
                    // Eight cards: holding the Murder means discarding *something* at cleanup, so
                    // patience has stopped being free. The negative control for removal-07 — if
                    // that one passes and this fails, the AI is not weighing a trade, it is
                    // refusing to spend removal on small creatures full stop.
                    .withCardsInHand(1, "Craw Wurm", 7)
                    .withCardOnBattlefield(1, "Wall of Stone")
                    .withCardOnBattlefield(2, "Mons's Goblin Raiders")
                    .withLandsOnBattlefield(2, "Mountain", 5)
                    .withCardsInHand(2, "Craw Wurm", 3)
                    .build()
            },
            check = {
                shouldCast("Murder")
                shouldTarget("Mons's Goblin Raiders")
            },
        ),

        AiPuzzle(
            id = "removal-09",
            category = PuzzleCategory.REMOVAL_TARGETING,
            expectation = "removal-07's board on turn twenty — the better target is not coming, so stop waiting for it",
            aiSeat = 1,
            position = { scenario ->
                scenario.withPlayers()
                    .withTurnNumber(20)
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withCardInHand(1, "Murder")
                    .withCardsInHand(1, "Craw Wurm", 3)
                    .withCardOnBattlefield(1, "Wall of Stone")
                    .withCardOnBattlefield(2, "Mons's Goblin Raiders")
                    .withLandsOnBattlefield(2, "Mountain", 5)
                    .withCardsInHand(2, "Craw Wurm", 3)
                    .build()
            },
            check = {
                shouldCast("Murder")
                shouldTarget("Mons's Goblin Raiders")
            },
        ),

        AiPuzzle(
            id = "removal-10",
            category = PuzzleCategory.REMOVAL_TARGETING,
            expectation = "The 1/1 is lethal on board at 1 life — kill it, however small it is",
            aiSeat = 1,
            position = { scenario ->
                scenario.withPlayers()
                    // removal-07's hand and removal-07's target, with the Wall taken away and the
                    // life total at one. Nothing about the *trade* has changed; what has changed is
                    // that passing loses the game, and no valuation of the target may outrank that.
                    .withLifeTotal(1, 1)
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withCardInHand(1, "Murder")
                    .withCardsInHand(1, "Craw Wurm", 3)
                    .withCardOnBattlefield(2, "Mons's Goblin Raiders")
                    .withLandsOnBattlefield(2, "Mountain", 5)
                    .withCardsInHand(2, "Craw Wurm", 3)
                    .build()
            },
            check = {
                shouldCast("Murder")
                shouldTarget("Mons's Goblin Raiders")
            },
        ),
    )
}

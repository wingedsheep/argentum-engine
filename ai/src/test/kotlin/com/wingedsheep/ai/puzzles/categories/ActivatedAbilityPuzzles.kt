package com.wingedsheep.ai.puzzles.categories

import com.wingedsheep.ai.puzzles.AiPuzzle
import com.wingedsheep.ai.puzzles.PuzzleCategory
import com.wingedsheep.ai.puzzles.advanceToDeclaration
import com.wingedsheep.ai.puzzles.advanceToPriority
import com.wingedsheep.sdk.core.Step

/**
 * The abilities already on the board, which no puzzle has ever asked the AI to use.
 *
 * `PuzzleMove.playedCard` has spoken `ActivateAbility` since Phase 2 and not one of the 48
 * positions asserted on it — so pingers, tappers, firebreathing and removal-on-a-stick are
 * unmeasured, and so is the *targeting* path they reach: `heuristicTargetRank` is exercised today
 * only through `CastSpell`.
 *
 * Every card here is a plain-text classic, deliberately: `Prodigal Sorcerer` ({T}: 1 damage to any
 * target), `Icy Manipulator` ({1},{T}: tap an artifact, creature or land), `Royal Assassin` ({T}:
 * destroy target tapped creature), `Shivan Dragon` ({R}: +1/+0). If the AI cannot use these it
 * cannot use anything.
 */
object ActivatedAbilityPuzzles {

    fun all(): List<AiPuzzle> = listOf(

        AiPuzzle(
            id = "activate-01",
            category = PuzzleCategory.ACTIVATED_ABILITIES,
            expectation = "Ping the 1/1 the damage actually kills, not the 3/3 it bounces off",
            aiSeat = 1,
            position = { scenario ->
                scenario.withPlayers()
                    .withCardOnBattlefield(1, "Prodigal Sorcerer")
                    .withCardOnBattlefield(2, "Llanowar Elves")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .build()
            },
            check = {
                shouldActivate("Prodigal Sorcerer")
                shouldTarget("Llanowar Elves")
            },
        ),

        AiPuzzle(
            id = "activate-02",
            category = PuzzleCategory.ACTIVATED_ABILITIES,
            expectation = "Ping the opponent's face for the win at 1 life",
            aiSeat = 1,
            position = { scenario ->
                scenario.withPlayers()
                    .withCardOnBattlefield(1, "Prodigal Sorcerer")
                    .withLifeTotal(2, 1)
                    .build()
            },
            // Lethal *through an ability*: `LethalDetectionPuzzles` is attacks and burn spells
            // only. Also the most direct probe of Phase 3's open finding that `heuristicTargetRank`
            // ranks an opponent player at -5.0, the same as our own face, because
            // `ProjectedState.getController` returns null for a player.
            check = {
                shouldActivate("Prodigal Sorcerer")
                shouldTargetOpponentFace()
            },
        ),

        AiPuzzle(
            id = "activate-03",
            category = PuzzleCategory.ACTIVATED_ABILITIES,
            expectation = "Tap the Wall with Icy Manipulator so the Wurm can get through",
            aiSeat = 1,
            position = { scenario ->
                scenario.withPlayers()
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withCardOnBattlefield(1, "Craw Wurm")
                    .withCardOnBattlefield(1, "Icy Manipulator")
                    .withCardOnBattlefield(2, "Wall of Stone")
                    .build()
            },
            // Spend a resource now to unlock combat later — the one-ply blind spot in miniature.
            // Tapping the Wall changes nothing the evaluator scores except a 0.9x on one
            // permanent; the payoff is six damage two steps from here.
            check = {
                shouldActivate("Icy Manipulator")
                shouldTarget("Wall of Stone")
            },
        ),

        AiPuzzle(
            id = "activate-04",
            category = PuzzleCategory.ACTIVATED_ABILITIES,
            expectation = "Do not point one damage at a 3/3 — going face or holding both beat it",
            aiSeat = 1,
            position = { scenario ->
                scenario.withPlayers()
                    .withCardOnBattlefield(1, "Prodigal Sorcerer")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .build()
            },
            // The negative control for activate-01: the same ability, the same board minus the
            // creature it can kill. Toughness has to be read, not just power.
            check = { shouldNotTarget("Hill Giant") },
        ),

        AiPuzzle(
            id = "activate-05",
            category = PuzzleCategory.ACTIVATED_ABILITIES,
            expectation = "Firebreathe the unblocked Dragon toward exactly lethal",
            aiSeat = 1,
            position = { scenario ->
                scenario.withPlayers()
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withCardOnBattlefield(1, "Shivan Dragon")
                    .withLifeTotal(2, 7)
                    .build()
                    .advanceToDeclaration(1, Step.DECLARE_ATTACKERS)
                    .also { it.declareAttackers(mapOf("Shivan Dragon" to 2)) }
                    .advanceToPriority(1, Step.DECLARE_BLOCKERS)
            },
            // Converting floating mana into damage, which nothing else in the suite asks for. A
            // single-action check can only see the *first* pump; the honest form of this position
            // is a line puzzle, and it is why Phase 2b wants one.
            check = { shouldActivate("Shivan Dragon") },
        ),

        AiPuzzle(
            id = "activate-06",
            category = PuzzleCategory.ACTIVATED_ABILITIES,
            expectation = "Assassinate the attacker rather than chump-block it with the 1/1",
            aiSeat = 1,
            position = { scenario ->
                scenario.withPlayers()
                    .withActivePlayer(2)
                    .withCardOnBattlefield(2, "Craw Wurm")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withCardOnBattlefield(1, "Royal Assassin")
                    .build()
                    .advanceToDeclaration(2, Step.DECLARE_ATTACKERS)
                    .also { it.declareAttackers(mapOf("Craw Wurm" to 1)) }
                    // Priority in declare-attackers, before blocks: the window where killing the
                    // attacker is free. Waiting for the blocker declaration throws the Assassin
                    // under the Wurm instead.
                    .advanceToPriority(1, Step.DECLARE_ATTACKERS)
            },
            check = {
                shouldActivate("Royal Assassin")
                shouldTarget("Craw Wurm")
            },
        ),
    )
}

package com.wingedsheep.ai.puzzles.categories

import com.wingedsheep.ai.puzzles.AiPuzzle
import com.wingedsheep.ai.puzzles.PuzzleCategory
import com.wingedsheep.ai.puzzles.advanceToDeclaration
import com.wingedsheep.sdk.core.Step

/**
 * Combat past the four keywords [BlockingPuzzles] covers.
 *
 * `creatureValue` has a table of twenty-odd keyword bonuses (`BoardFeatures.kt:203-238`) and the
 * suite exercises four of them. Trample, menace, reach and indestructible each change what a block
 * or a removal spell is *worth* rather than what it costs, which is the kind of thing a stat-summing
 * evaluator gets confidently wrong.
 *
 * Every position is built so the naive reading picks the wrong side — including the two that pair
 * off deliberately: `keywords-01` and `-02` are the same 4/4 trampler, and a blanket rule about
 * either one fails the other.
 */
object CombatKeywordPuzzles {

    fun all(): List<AiPuzzle> = listOf(

        AiPuzzle(
            id = "keywords-01",
            category = PuzzleCategory.COMBAT_KEYWORDS,
            expectation = "Do not chump a trampler: the 1/1 buys one point of life and dies",
            aiSeat = 2,
            position = { scenario ->
                scenario.withPlayers()
                    .withCardOnBattlefield(1, "Fangren Hunter")
                    .withCardOnBattlefield(2, "Llanowar Elves")
                    .withLifeTotal(2, 12)
                    .build()
                    .advanceToDeclaration(1, Step.DECLARE_ATTACKERS)
                    .also { it.declareAttackers(mapOf("Fangren Hunter" to 2)) }
                    .advanceToDeclaration(2, Step.DECLARE_BLOCKERS)
            },
            // Blocking: the Elves die and three still trample over, 12 -> 9. Not blocking: 12 -> 8.
            // A whole creature for one point of life.
            check = { shouldNotBlock() },
        ),

        AiPuzzle(
            id = "keywords-02",
            category = PuzzleCategory.COMBAT_KEYWORDS,
            expectation = "Block the trampler with the 0/8 — eight toughness eats all four",
            aiSeat = 2,
            position = { scenario ->
                scenario.withPlayers()
                    .withCardOnBattlefield(1, "Fangren Hunter")
                    .withCardOnBattlefield(2, "Wall of Stone")
                    .build()
                    .advanceToDeclaration(1, Step.DECLARE_ATTACKERS)
                    .also { it.declareAttackers(mapOf("Fangren Hunter" to 2)) }
                    .advanceToDeclaration(2, Step.DECLARE_BLOCKERS)
            },
            // The mirror of keywords-01. Trample only spills past *toughness*, so nothing gets
            // through and the Wall survives — a rule of "don't block tramplers" fails here.
            check = { shouldBlock("Wall of Stone", "Fangren Hunter") },
        ),

        AiPuzzle(
            id = "keywords-03",
            category = PuzzleCategory.COMBAT_KEYWORDS,
            expectation = "Gang-block the menace attacker at 2 life — one blocker is not allowed",
            aiSeat = 2,
            position = { scenario ->
                scenario.withPlayers()
                    .withCardOnBattlefield(1, "Goblin Trailblazer")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withLifeTotal(2, 2)
                    .build()
                    .advanceToDeclaration(1, Step.DECLARE_ATTACKERS)
                    .also { it.declareAttackers(mapOf("Goblin Trailblazer" to 2)) }
                    .advanceToDeclaration(2, Step.DECLARE_BLOCKERS)
            },
            // Two power against two life. Menace makes the single block illegal, so the choice is
            // the double block or death — and both Bears survive it.
            check = { shouldBlockWithAtLeast(2, "Goblin Trailblazer") },
        ),

        AiPuzzle(
            id = "keywords-04",
            category = PuzzleCategory.COMBAT_KEYWORDS,
            expectation = "Reach blocks the flier: the Spider eats the Drake and lives",
            aiSeat = 2,
            position = { scenario ->
                scenario.withPlayers()
                    .withCardOnBattlefield(1, "Wind Drake")
                    .withCardOnBattlefield(1, "Trained Armodon")
                    .withCardOnBattlefield(2, "Giant Spider")
                    .withLifeTotal(2, 8)
                    .build()
                    .advanceToDeclaration(1, Step.DECLARE_ATTACKERS)
                    .also { it.declareAttackers(mapOf("Wind Drake" to 2, "Trained Armodon" to 2)) }
                    .advanceToDeclaration(2, Step.DECLARE_BLOCKERS)
            },
            // Life is set high enough that survival is not the question — the only question is
            // whether the AI knows a 2/4 reach creature may block a flier at all. On the Drake it
            // kills a 2/2 for free; on the Armodon nothing dies either way.
            check = { shouldBlock("Giant Spider", "Wind Drake") },
        ),

        AiPuzzle(
            id = "keywords-05",
            category = PuzzleCategory.COMBAT_KEYWORDS,
            expectation = "Do not block a deathtouch 2/1 with a Serra Angel at 18 life",
            aiSeat = 2,
            position = { scenario ->
                scenario.withPlayers()
                    .withCardOnBattlefield(1, "Ambush Viper")
                    .withCardOnBattlefield(2, "Serra Angel")
                    .withLifeTotal(2, 18)
                    .build()
                    .advanceToDeclaration(1, Step.DECLARE_ATTACKERS)
                    .also { it.declareAttackers(mapOf("Ambush Viper" to 2)) }
                    .advanceToDeclaration(2, Step.DECLARE_BLOCKERS)
            },
            // `blocking-04` asks the AI to *use* deathtouch. This asks it to respect the other
            // side's: the block trades a 4/4 flier for a 2/1 to save two life.
            check = { shouldNotBlock() },
        ),

        AiPuzzle(
            id = "keywords-06",
            category = PuzzleCategory.COMBAT_KEYWORDS,
            expectation = "Murder the Wurm — destroy does nothing to an indestructible creature",
            aiSeat = 1,
            position = { scenario ->
                scenario.withPlayers()
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withCardInHand(1, "Murder")
                    .withCardOnBattlefield(2, "Zetalpa, Primal Dawn")
                    .withCardOnBattlefield(2, "Craw Wurm")
                    .build()
            },
            // Legal but useless, which is the failure mode `removal-03` (Pacifism) probes from the
            // other direction. `creatureValue` pays +3.0 for indestructible on top of flying,
            // double strike, vigilance and trample, so its own keyword table aims the spell at the
            // one creature the spell cannot kill.
            check = {
                shouldCast("Murder")
                shouldTarget("Craw Wurm")
                shouldNotTarget("Zetalpa, Primal Dawn")
            },
        ),
    )
}

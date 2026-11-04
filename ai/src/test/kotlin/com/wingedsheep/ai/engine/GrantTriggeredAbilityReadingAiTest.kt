package com.wingedsheep.ai.engine

import com.wingedsheep.ai.audit.AiReadingAudit
import com.wingedsheep.ai.puzzles.AiPuzzle
import com.wingedsheep.ai.puzzles.PuzzleCategory
import com.wingedsheep.ai.puzzles.PuzzleRunner
import com.wingedsheep.ai.puzzles.advanceToDeclaration
import com.wingedsheep.ai.puzzles.advanceToPriority
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.MtgSetCatalog
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * "Until end of turn, whenever this creature deals combat damage to a player, draw two cards"
 * (Lyra, Tolarian Archangel; Herd Heirloom; The Pride of Hull Clade): a granted trigger the
 * creature fires only by fighting, so — like a pump — it is spent in this turn's combat or wears
 * off unused at cleanup. Unread, the activation had no end-of-turn reading and the leaf score was
 * a coin flip on it: Lyra paid {3}{U}{U} for the draw in our own postcombat main, with no combat
 * left to deal the damage in.
 */
class GrantTriggeredAbilityReadingAiTest : ScenarioTestBase() {

    private val runner by lazy { PuzzleRunner(cardRegistry) { scenario() } }

    private fun run(puzzle: AiPuzzle) {
        val result = runner.run(puzzle, AiProfile.LIVE)
        withClue("${puzzle.expectation}: chose ${result.move} — ${result.failure}") {
            result.passed shouldBe true
        }
    }

    private fun lyraWithNoFightLeft(step: Step) = AiPuzzle(
        id = "lyra-no-fight-$step",
        category = PuzzleCategory.HOLDING_INSTANTS,
        expectation = "Lyra's combat-damage draw wears off at cleanup with no combat left to spend it in ($step)",
        aiSeat = 2,
        position = { s ->
            s.withPlayers()
                .withActivePlayer(2)
                .withLandsOnBattlefield(2, "Island", 5)
                .withCardOnBattlefield(2, "Lyra, Tolarian Archangel")
                .apply { repeat(4) { withCardInLibrary(2, "Hill Giant"); withCardInLibrary(1, "Hill Giant") } }
                .build()
                .advanceToPriority(2, step)
        },
        check = { shouldNotActivate("Lyra, Tolarian Archangel") },
    )

    init {
        test("the granted combat-damage trigger has an end-of-turn timing reading") {
            val lyra = MtgSetCatalog.all.flatMap { it.cards }.first { it.name == "Lyra, Tolarian Archangel" }
            AiReadingAudit.audit(lyra).map { it.key } shouldNotContain "timing: GrantTriggeredAbilityEffect"
        }

        test("Lyra doesn't buy its draw in our postcombat main, once combat is over") {
            run(lyraWithNoFightLeft(Step.POSTCOMBAT_MAIN))
        }

        test("Lyra doesn't buy its draw in our end step") {
            run(lyraWithNoFightLeft(Step.END))
        }

        test("Lyra attacking unblocked buys the draw before damage") {
            run(
                AiPuzzle(
                    id = "lyra-unblocked",
                    category = PuzzleCategory.HOLDING_INSTANTS,
                    expectation = "Lyra is attacking unblocked: buy the draw-two before damage",
                    aiSeat = 1,
                    position = { s ->
                        s.withPlayers()
                            .withLandsOnBattlefield(1, "Island", 5)
                            .withCardOnBattlefield(1, "Lyra, Tolarian Archangel")
                            .apply { repeat(4) { withCardInLibrary(2, "Hill Giant"); withCardInLibrary(1, "Hill Giant") } }
                            .build()
                            .advanceToDeclaration(1, Step.DECLARE_ATTACKERS)
                            .also { it.declareAttackers(mapOf("Lyra, Tolarian Archangel" to 2)) }
                            .advanceToDeclaration(2, Step.DECLARE_BLOCKERS)
                            .also { it.declareBlockers(emptyMap()) }
                            .advanceToPriority(1, Step.DECLARE_BLOCKERS)
                    },
                    check = { shouldActivate("Lyra, Tolarian Archangel") },
                ),
            )
        }
    }
}

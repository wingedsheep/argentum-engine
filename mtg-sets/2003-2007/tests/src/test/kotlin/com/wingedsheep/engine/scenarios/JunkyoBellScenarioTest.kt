package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Junkyo Bell (CHK #258) — "At the beginning of your upkeep, you may have target creature you
 * control get +X/+X until end of turn, where X is the number of creatures you control. If you do,
 * sacrifice that creature at the beginning of the next end step."
 *
 * Covers the accepted path (pump sized by the creature count, then the delayed sacrifice of exactly
 * the targeted creature), the declined path (no pump and — "if you do" — no sacrifice), and the
 * "creature you control" target restriction.
 */
class JunkyoBellScenarioTest : ScenarioTestBase() {

    /** Starts in the opponent's turn and advances to our upkeep, where the Bell triggers. */
    private fun TestGame.advanceToOurUpkeep() {
        passUntilPhase(Phase.ENDING, Step.END)
        passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
    }

    init {
        context("Junkyo Bell") {
            test("accepting pumps the target by the creature count and sacrifices it at the next end step") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Junkyo Bell")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(1, "Llanowar Elves")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val bear = game.findPermanent("Grizzly Bears")!!
                val elves = game.findPermanent("Llanowar Elves")!!
                val giant = game.findPermanent("Hill Giant")!!

                game.advanceToOurUpkeep()

                // The optional trigger asks "you may" as it goes on the stack, then for its target.
                game.answerYesNo(true)
                val decision = game.getPendingDecision()
                decision.shouldBeInstanceOf<ChooseTargetsDecision>()
                val legal = decision.legalTargets[0] ?: emptyList()
                withClue("only creatures you control are legal targets") {
                    legal shouldContain bear
                    legal shouldContain elves
                    legal shouldNotContain giant
                }
                game.selectTargets(listOf(bear))
                game.resolveStack()

                withClue("two creatures you control: the 2/2 becomes a 4/4") {
                    game.state.projectedState.getPower(bear) shouldBe 4
                    game.state.projectedState.getToughness(bear) shouldBe 4
                    game.state.projectedState.getPower(elves) shouldBe 1
                }

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()

                withClue("the pumped creature — and only it — is sacrificed") {
                    game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                    game.isOnBattlefield("Llanowar Elves") shouldBe true
                }
            }

            test("declining neither pumps nor sacrifices the creature") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Junkyo Bell")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val bear = game.findPermanent("Grizzly Bears")!!

                game.advanceToOurUpkeep()

                game.answerYesNo(false)
                game.resolveStack()

                game.state.projectedState.getPower(bear) shouldBe 2

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()

                game.isOnBattlefield("Grizzly Bears") shouldBe true
            }
        }
    }
}

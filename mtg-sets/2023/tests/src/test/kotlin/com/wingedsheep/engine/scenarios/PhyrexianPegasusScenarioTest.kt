package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Phyrexian Pegasus (MOM #324) — "Flying. Whenever this creature attacks, another target
 * attacking creature without flying gains flying until end of turn."
 */
class PhyrexianPegasusScenarioTest : ScenarioTestBase() {

    private fun board() = scenario()
        .withPlayers("Player", "Opponent")
        .withCardOnBattlefield(1, "Phyrexian Pegasus", summoningSickness = false)
        .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
        .withCardOnBattlefield(1, "Hill Giant", summoningSickness = false)
        .withCardOnBattlefield(1, "Wind Drake", summoningSickness = false)
        .withCardInLibrary(1, "Plains")
        .withCardInLibrary(2, "Plains")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("only other attacking creatures without flying are legal targets; the target gains flying") {
            val game = board()
            val bears = game.findPermanent("Grizzly Bears")!!
            val giant = game.findPermanent("Hill Giant")!!

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(
                mapOf("Phyrexian Pegasus" to 2, "Grizzly Bears" to 2, "Wind Drake" to 2)
            ).error shouldBe null

            val decision = game.getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
            withClue("Pegasus itself, the flying Wind Drake and the non-attacking Hill Giant are excluded") {
                decision.legalTargets[0]!! shouldContainExactlyInAnyOrder listOf(bears)
            }
            game.selectTargets(listOf(bears)).error shouldBe null
            game.resolveStack()

            game.state.projectedState.hasKeyword(bears, Keyword.FLYING) shouldBe true
            game.state.projectedState.hasKeyword(giant, Keyword.FLYING) shouldBe false
        }

        test("flying wears off at end of turn") {
            val game = board()
            val bears = game.findPermanent("Grizzly Bears")!!

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Phyrexian Pegasus" to 2, "Grizzly Bears" to 2)).error shouldBe null
            if (game.getPendingDecision() is ChooseTargetsDecision) {
                game.selectTargets(listOf(bears)).error shouldBe null
            }
            game.resolveStack()
            game.state.projectedState.hasKeyword(bears, Keyword.FLYING) shouldBe true

            game.passUntilPhase(Phase.ENDING, Step.CLEANUP)
            game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            game.state.projectedState.hasKeyword(bears, Keyword.FLYING) shouldBe false
        }
    }
}

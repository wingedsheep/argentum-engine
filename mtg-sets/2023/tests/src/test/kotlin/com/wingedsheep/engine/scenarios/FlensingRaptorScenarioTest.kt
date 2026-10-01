package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe

/**
 * Flensing Raptor (ONE #12) — {2}{W} 2/2 Creature — Phyrexian Bird.
 *
 *   Flying
 *   Toxic 1
 *   When this creature enters, another target creature you control with toxic gets +1/+1 and
 *   gains flying until end of turn.
 */
class FlensingRaptorScenarioTest : ScenarioTestBase() {

    init {
        test("Raptor has flying and toxic 1") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Flensing Raptor")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val raptor = game.findPermanent("Flensing Raptor")!!
            game.state.projectedState.hasKeyword(raptor, Keyword.FLYING) shouldBe true
            game.state.projectedState.getKeywords(raptor).contains("TOXIC_1") shouldBe true
        }

        test("targets only another toxic creature you control; it gets +1/+1 and flying until end of turn") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Flensing Raptor")
                .withLandsOnBattlefield(1, "Plains", 3)
                .withCardOnBattlefield(1, "Crawling Chorus")
                .withCardOnBattlefield(1, "Crawling Chorus")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Crawling Chorus")
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Plains")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val p1 = game.player1Id
            val myChoruses = game.findPermanents("Crawling Chorus")
                .filter { game.state.projectedState.getController(it) == p1 }
            myChoruses.size shouldBe 2
            val target = myChoruses.first()
            val other = myChoruses.last()
            val bears = game.findPermanent("Grizzly Bears")!!

            game.castSpell(1, "Flensing Raptor").error shouldBe null
            game.resolveStack()

            val decision = game.state.pendingDecision as? ChooseTargetsDecision
                ?: error("expected a ChooseTargetsDecision; got ${game.state.pendingDecision}")
            withClue("only my toxic creatures other than the Raptor are legal (not Bears, not the opponent's Chorus)") {
                decision.legalTargets[0]!! shouldContainExactlyInAnyOrder myChoruses
            }
            game.selectTargets(listOf(target)).error shouldBe null
            game.resolveStack()

            game.state.projectedState.getPower(target) shouldBe 2
            game.state.projectedState.getToughness(target) shouldBe 2
            game.state.projectedState.hasKeyword(target, Keyword.FLYING) shouldBe true
            withClue("the untargeted Chorus and the Bears are unchanged") {
                game.state.projectedState.getPower(other) shouldBe 1
                game.state.projectedState.hasKeyword(other, Keyword.FLYING) shouldBe false
                game.state.projectedState.getPower(bears) shouldBe 2
            }

            game.passUntilPhase(Phase.ENDING, Step.CLEANUP)
            game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            withClue("the pump and flying wear off at end of turn") {
                game.state.projectedState.getPower(target) shouldBe 1
                game.state.projectedState.hasKeyword(target, Keyword.FLYING) shouldBe false
            }
        }

        test("with no other toxic creature, the trigger has no target and nothing is pumped") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Flensing Raptor")
                .withLandsOnBattlefield(1, "Plains", 3)
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Plains")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Flensing Raptor").error shouldBe null
            game.resolveStack()

            (game.state.pendingDecision is ChooseTargetsDecision) shouldBe false
            game.state.stack.size shouldBe 0
            val raptor = game.findPermanent("Flensing Raptor")!!
            val bears = game.findPermanent("Grizzly Bears")!!
            game.state.projectedState.getPower(raptor) shouldBe 2
            game.state.projectedState.getPower(bears) shouldBe 2
            game.state.projectedState.hasKeyword(bears, Keyword.FLYING) shouldBe false
        }
    }
}

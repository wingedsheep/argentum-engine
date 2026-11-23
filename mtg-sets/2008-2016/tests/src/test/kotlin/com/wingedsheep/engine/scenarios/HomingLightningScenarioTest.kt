package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

/**
 * Homing Lightning — {2}{R}{R} Instant — "Homing Lightning deals 4 damage to target creature and
 * each other creature with the same name as that creature."
 *
 * The damage reaches every creature sharing the target's name, whoever controls it, and leaves
 * differently named creatures alone. A face-down creature has no name (CR 708.2a): as the target it
 * drags no real-name sibling along, and as a namesake it is never swept up.
 */
class HomingLightningScenarioTest : ScenarioTestBase() {

    init {
        test("deals 4 to the target and every same-named creature on both sides") {
            val g = scenario()
                .withPlayers()
                .withCardInHand(1, "Homing Lightning")
                .withLandsOnBattlefield(1, "Mountain", 4)
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardOnBattlefield(2, "Grizzly Bears", isToken = true)
                .withCardOnBattlefield(2, "Hill Giant")
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .withActivePlayer(1)
                .withPriorityPlayer(1)
                .build()

            val bears = g.findAllPermanents("Grizzly Bears")
            bears shouldHaveSize 3
            val giant = g.findPermanent("Hill Giant")!!
            val target = bears.first { g.state.projectedState.getController(it) == g.player2Id }

            g.castSpell(1, "Homing Lightning", target).error shouldBe null
            g.resolveStack()

            g.findAllPermanents("Grizzly Bears") shouldHaveSize 0
            withClue("a differently named creature is untouched") {
                g.state.getBattlefield().contains(giant) shouldBe true
            }
        }

        test("a face-down namesake is not hit") {
            val g = scenario()
                .withPlayers()
                .withCardInHand(1, "Homing Lightning")
                .withLandsOnBattlefield(1, "Mountain", 4)
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .withActivePlayer(1)
                .withPriorityPlayer(1)
                .build()

            val (target, hidden) = g.findAllPermanents("Grizzly Bears")
            g.state = g.state.updateEntity(hidden) { it.with(FaceDownComponent) }

            g.castSpell(1, "Homing Lightning", target).error shouldBe null
            g.resolveStack()

            g.state.getBattlefield().contains(target) shouldBe false
            withClue("the face-down Grizzly Bears has no name, so it shares none with the target") {
                g.state.getBattlefield().contains(hidden) shouldBe true
            }
        }

        test("a face-down target is hit alone") {
            val g = scenario()
                .withPlayers()
                .withCardInHand(1, "Homing Lightning")
                .withLandsOnBattlefield(1, "Mountain", 4)
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .withActivePlayer(1)
                .withPriorityPlayer(1)
                .build()

            val (target, faceUp) = g.findAllPermanents("Grizzly Bears")
            g.state = g.state.updateEntity(target) { it.with(FaceDownComponent) }

            g.castSpell(1, "Homing Lightning", target).error shouldBe null
            g.resolveStack()

            g.state.getBattlefield().contains(target) shouldBe false
            withClue("the face-down target has no name, so its face-up real-name sibling stays") {
                g.state.getBattlefield().contains(faceUp) shouldBe true
            }
        }
    }
}

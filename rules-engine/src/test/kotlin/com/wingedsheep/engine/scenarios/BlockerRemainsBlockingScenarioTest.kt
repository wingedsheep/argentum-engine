package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.combat.BlockingComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe

/**
 * A blocking creature stays a blocking creature after every attacker it blocked has left combat:
 * only the blocker's *own* removal from combat ends that status (CR 509.1g). It simply assigns no
 * combat damage (CR 510.1d). "Deals damage to a blocking creature" (Kusari-Gama) and "target
 * blocking creature" read that status after first-strike or regular combat damage kills the
 * attacker.
 */
class BlockerRemainsBlockingScenarioTest : ScenarioTestBase() {

    init {
        test("a blocker whose attacker died to first-strike damage is still blocking, blocking nothing") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Grizzly Bears")         // 2/2 attacker
                .withCardOnBattlefield(2, "First Strike Knight")   // 3/1 first strike blocker
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Plains")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareBlockers(mapOf("First Strike Knight" to listOf("Grizzly Bears"))).error shouldBe null
            // The regular damage step has no damage left to deal, so stop at end of combat — still
            // inside combat, before the end-of-combat cleanup (CR 511.3).
            game.passUntilPhase(Phase.COMBAT, Step.END_COMBAT)

            withClue("First strike killed the attacker before it could deal regular damage") {
                game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                game.isOnBattlefield("First Strike Knight") shouldBe true
            }
            val knight = game.findPermanent("First Strike Knight")!!
            val blocking = game.state.getEntity(knight)?.get<BlockingComponent>()
            withClue("The Knight is still a blocking creature (CR 509.1g)") {
                (blocking != null) shouldBe true
            }
            withClue("…blocking no creature, so it assigns no combat damage (CR 510.1d)") {
                blocking!!.blockedAttackerIds.shouldBeEmpty()
            }

            game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
            withClue("Combat ends and the status clears with it") {
                game.state.getEntity(knight)?.get<BlockingComponent>() shouldBe null
            }
        }
    }
}

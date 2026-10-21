package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Hedgewitch's Mask (MID #23) — equipped creature gets +1/+1 and can't be blocked by creatures
 * with power 4 or greater.
 */
class HedgewitchsMaskScenarioTest : ScenarioTestBase() {

    private fun game(): TestGame = scenario()
        .withPlayers("Player1", "Player2")
        .withCardOnBattlefield(1, "Grizzly Bears")
        .withCardAttachedTo(1, "Hedgewitch's Mask", "Grizzly Bears")
        .withCardOnBattlefield(2, "Craw Wurm")
        .withCardOnBattlefield(2, "Hill Giant")
        .withCardInLibrary(1, "Plains")
        .withCardInLibrary(2, "Forest")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("equipped creature gets +1/+1") {
            val game = game()
            val bears = game.findPermanent("Grizzly Bears")!!
            game.state.projectedState.getPower(bears) shouldBe 3
            game.state.projectedState.getToughness(bears) shouldBe 3
        }

        test("equipped creature can't be blocked by a creature with power 4 or greater") {
            val game = game()
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

            game.declareBlockers(mapOf("Craw Wurm" to listOf("Grizzly Bears"))).error shouldNotBe null
        }

        test("equipped creature can still be blocked by a creature with power 3") {
            val game = game()
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

            game.declareBlockers(mapOf("Hill Giant" to listOf("Grizzly Bears"))).error shouldBe null
        }
    }
}

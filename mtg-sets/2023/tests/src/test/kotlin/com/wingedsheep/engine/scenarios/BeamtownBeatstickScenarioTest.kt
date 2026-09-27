package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Beamtown Beatstick — equipped creature gets +1/+0 and menace; whenever it deals combat damage to
 * a player or battle, create a Treasure token.
 */
class BeamtownBeatstickScenarioTest : ScenarioTestBase() {

    private fun board() = scenario()
        .withPlayers("Player", "Opponent")
        .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
        .withCardAttachedTo(1, "Beamtown Beatstick", "Grizzly Bears")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

    private fun TestGame.finishCombat() {
        passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
        declareNoBlockers()
        passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
    }

    init {
        test("combat damage to a player makes a Treasure, dealt at +1/+0") {
            val game = board().build()
            game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
            game.finishCombat()
            withClue("the 2/2 hits for 3 with the Beatstick") { game.getLifeTotal(2) shouldBe 17 }
            game.findPermanents("Treasure").size shouldBe 1
        }

        test("combat damage to a battle makes a Treasure") {
            val game = board().withCardOnBattlefield(1, "Invasion of Innistrad").build()
            game.checkStateBasedActions()
            game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackersWithPermanentTargets(
                permanentAttackers = mapOf("Grizzly Bears" to "Invasion of Innistrad")
            ).error shouldBe null
            game.finishCombat()
            game.findPermanents("Treasure").size shouldBe 1
        }

        test("the equipped creature has menace — one blocker can't block it") {
            val game = board().withCardOnBattlefield(2, "Hill Giant", summoningSickness = false).build()
            game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            val result = game.declareBlockers(mapOf("Hill Giant" to listOf("Grizzly Bears")))
            (result.error != null) shouldBe true
        }
    }
}

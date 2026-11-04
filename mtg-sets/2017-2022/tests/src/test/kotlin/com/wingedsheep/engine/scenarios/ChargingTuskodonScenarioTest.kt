package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CombatResolutionDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Charging Tuskodon (RIX #97) — "Trample. If this creature would deal combat damage to a player,
 * it deals double that damage to that player instead."
 *
 * Unblocked it hits for 8; blocked by a 3/3, the trample excess is assigned by its actual power
 * (1 to the player) and doubled only as it's dealt (ruling).
 */
class ChargingTuskodonScenarioTest : ScenarioTestBase() {

    init {
        test("unblocked, its combat damage to the player is doubled") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Charging Tuskodon", summoningSickness = false)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Charging Tuskodon" to 2)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.END_COMBAT)

            game.getLifeTotal(2) shouldBe 12
        }

        test("trample excess is assigned by power and doubled only to the player") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Charging Tuskodon", summoningSickness = false)
                .withCardOnBattlefield(2, "Hill Giant")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Charging Tuskodon" to 2)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareBlockers(mapOf("Hill Giant" to listOf("Charging Tuskodon"))).error shouldBe null

            var passes = 0
            while (!game.hasPendingDecision() && passes++ < 20) game.passPriority()
            val decision = game.getPendingDecision().shouldBeInstanceOf<CombatResolutionDecision>()
            val giantEdge = decision.edges.single { it.targetId == game.findPermanent("Hill Giant") }
            game.submitCombatDamage(
                mapOf(
                    (giantEdge.sourceId to giantEdge.targetId) to 3,
                    (giantEdge.sourceId to game.player2Id) to 1
                )
            ).error shouldBe null

            withClue("1 trample damage assigned to the player is dealt as 2") {
                game.getLifeTotal(2) shouldBe 18
            }
            withClue("the 3/3 blocker dies and the 4/4 Tuskodon survives its 3 damage") {
                game.isOnBattlefield("Hill Giant") shouldBe false
                game.isOnBattlefield("Charging Tuskodon") shouldBe true
            }
        }
    }
}

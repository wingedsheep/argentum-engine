package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.DistributeDecision
import com.wingedsheep.engine.core.DistributionResponse
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Gang of Devils (AVR #136) — "When this creature dies, it deals 3 damage divided as you choose
 * among one, two, or three targets."
 *
 * The source has left the battlefield by the time the trigger is put on the stack and resolves, so
 * the division and the damage both run off its last-known information.
 */
class GangOfDevilsScenarioTest : ScenarioTestBase() {

    init {
        context("Gang of Devils") {

            fun board() = scenario()
                .withPlayers("Alice", "Bob")
                .withCardOnBattlefield(1, "Gang of Devils")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardInHand(1, "Terror")
                .withLandsOnBattlefield(1, "Swamp", 2)
                .withCardInLibrary(1, "Swamp")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            test("dying splits 3 damage between a creature and a player") {
                val game = board()
                val devils = game.findPermanent("Gang of Devils")!!
                val bears = game.findPermanent("Grizzly Bears")!!

                game.castSpell(1, "Terror", devils).error shouldBe null
                game.resolveStack()
                game.selectTargets(listOf(bears, game.player2Id)).error shouldBe null

                val decision = game.getPendingDecision().shouldBeInstanceOf<DistributeDecision>()
                withClue("3 damage, at least 1 per target") {
                    decision.totalAmount shouldBe 3
                    decision.minPerTarget shouldBe 1
                }
                game.submitDecision(
                    DistributionResponse(decision.id, mapOf(bears to 2, game.player2Id to 1)),
                ).error shouldBe null
                game.resolveStack()

                game.isInGraveyard(1, "Gang of Devils") shouldBe true
                game.isInGraveyard(2, "Grizzly Bears") shouldBe true
                game.getLifeTotal(2) shouldBe 19
            }

            test("a single target takes all 3 damage") {
                val game = board()
                val devils = game.findPermanent("Gang of Devils")!!

                game.castSpell(1, "Terror", devils).error shouldBe null
                game.resolveStack()
                game.selectTargets(listOf(game.player2Id)).error shouldBe null
                game.resolveStack()

                game.getLifeTotal(2) shouldBe 17
            }
        }
    }
}

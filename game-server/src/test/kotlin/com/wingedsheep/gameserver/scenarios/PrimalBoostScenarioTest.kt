package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

class PrimalBoostScenarioTest : ScenarioTestBase() {

    private val stateProjector = StateProjector()

    init {
        context("Primal Boost") {
            test("casting gives target creature +4/+4 until end of turn") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardInHand(1, "Primal Boost")
                    .withLandsOnBattlefield(1, "Forest", 3)
                    .withCardOnBattlefield(1, "Glory Seeker") // 2/2 creature
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val targetId = game.findPermanent("Glory Seeker")!!

                // Cast Primal Boost targeting Glory Seeker
                val result = game.castSpell(1, "Primal Boost", targetId)
                withClue("Should cast successfully") {
                    result.error shouldBe null
                }
                game.resolveStack()

                // Verify Glory Seeker is now 6/6
                val projected = stateProjector.project(game.state)
                withClue("Glory Seeker should be 6/6 after Primal Boost (+4/+4)") {
                    projected.getPower(targetId) shouldBe 6
                    projected.getToughness(targetId) shouldBe 6
                }
            }

            test("cycling triggers optional +1/+1 on target creature") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardInHand(1, "Primal Boost")
                    .withLandsOnBattlefield(1, "Forest", 3)
                    .withCardOnBattlefield(1, "Glory Seeker") // 2/2 creature
                    .withCardInLibrary(1, "Mountain") // Card to draw from cycling
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val targetId = game.findPermanent("Glory Seeker")!!

                // Cycle Primal Boost
                val cycleResult = game.cycleCard(1, "Primal Boost")
                withClue("Cycling should succeed") {
                    cycleResult.error shouldBe null
                }

                // Cycling trigger fires - MayEffect asks yes/no
                withClue("Should have pending may decision") {
                    game.hasPendingDecision() shouldBe true
                }
                game.answerYesNo(true)

                // Select target creature
                game.selectTargets(listOf(targetId))

                // Resolve the triggered ability
                game.resolveStack()

                // Verify Glory Seeker is now 3/3
                val projected = stateProjector.project(game.state)
                withClue("Glory Seeker should be 3/3 after cycling trigger (+1/+1)") {
                    projected.getPower(targetId) shouldBe 3
                    projected.getToughness(targetId) shouldBe 3
                }

                // Verify Primal Boost went to graveyard
                withClue("Primal Boost should be in graveyard") {
                    game.isInGraveyard(1, "Primal Boost") shouldBe true
                }
            }

            test("cycling trigger can be declined with may effect") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardInHand(1, "Primal Boost")
                    .withLandsOnBattlefield(1, "Forest", 3)
                    .withCardOnBattlefield(1, "Glory Seeker") // 2/2 creature
                    .withCardInLibrary(1, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val targetId = game.findPermanent("Glory Seeker")!!

                // Cycle Primal Boost
                val cycleResult = game.cycleCard(1, "Primal Boost")
                withClue("Cycling should succeed") {
                    cycleResult.error shouldBe null
                }

                // Decline the may effect
                withClue("Should have pending may decision") {
                    game.hasPendingDecision() shouldBe true
                }
                game.answerYesNo(false)

                // Verify Glory Seeker is still 2/2
                val projected = stateProjector.project(game.state)
                withClue("Glory Seeker should still be 2/2 after declining cycling trigger") {
                    projected.getPower(targetId) shouldBe 2
                    projected.getToughness(targetId) shouldBe 2
                }
            }
        }
    }
}

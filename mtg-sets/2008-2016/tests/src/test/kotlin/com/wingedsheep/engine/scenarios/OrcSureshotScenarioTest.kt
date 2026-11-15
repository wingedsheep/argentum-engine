package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Orc Sureshot (FRF #78) — {3}{B} Creature — Orc Archer 4/2.
 *
 *   Whenever another creature you control enters, target creature an opponent controls gets -1/-1
 *   until end of turn.
 */
class OrcSureshotScenarioTest : ScenarioTestBase() {

    init {
        context("Orc Sureshot") {

            test("another creature you control entering gives target opponent's creature -1/-1") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Orc Sureshot")
                    .withCardInHand(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withCardOnBattlefield(2, "Centaur Courser")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val giant = game.findPermanent("Hill Giant")!!
                val courser = game.findPermanent("Centaur Courser")!!

                game.castSpell(1, "Grizzly Bears").error shouldBe null
                game.resolveStack()

                val decision = game.getPendingDecision()
                decision.shouldBeInstanceOf<ChooseTargetsDecision>()
                game.selectTargets(listOf(giant)).error shouldBe null
                game.resolveStack()

                game.state.projectedState.getPower(giant) shouldBe 2
                game.state.projectedState.getToughness(giant) shouldBe 2
                withClue("only the target shrinks") {
                    game.state.projectedState.getPower(courser) shouldBe 3
                }
            }

            test("Orc Sureshot entering itself doesn't trigger") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Orc Sureshot")
                    .withLandsOnBattlefield(1, "Swamp", 4)
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withCardOnBattlefield(2, "Centaur Courser")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Orc Sureshot").error shouldBe null
                game.resolveStack()

                game.isOnBattlefield("Orc Sureshot") shouldBe true
                game.hasPendingDecision() shouldBe false
                game.state.stack.isEmpty() shouldBe true
                game.state.projectedState.getPower(game.findPermanent("Hill Giant")!!) shouldBe 3
            }
        }
    }
}

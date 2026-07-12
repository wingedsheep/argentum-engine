package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Sawblade Slinger (VOW #217) — {3}{G} Creature — Human Archer, 4/3.
 *
 *   When this creature enters, choose up to one —
 *   • Destroy target artifact an opponent controls.
 *   • This creature fights target Zombie an opponent controls.
 *
 * Exercises the ETB "choose up to one" modal: mode 1 destroys an opponent's artifact; declining
 * both modes leaves the board untouched.
 */
class SawbladeSlingerScenarioTest : ScenarioTestBase() {

    init {
        context("Sawblade Slinger ETB modal") {

            test("choosing mode 1 destroys the targeted opponent artifact") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Sawblade Slinger")
                    .withCardOnBattlefield(2, "Artifact Creature") // 2/2 Golem, an artifact
                    .withLandsOnBattlefield(1, "Forest", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val artifact = game.findPermanent("Artifact Creature")!!

                game.castSpell(1, "Sawblade Slinger").error shouldBe null
                game.resolveStack()

                val modeDecision = game.getPendingDecision() as? ChooseOptionDecision
                    ?: error("expected a ChooseOptionDecision for the ETB modal; got ${game.getPendingDecision()}")
                game.submitDecision(OptionChosenResponse(modeDecision.id, optionIndex = 0))

                if (game.hasPendingDecision()) game.selectTargets(listOf(artifact))
                game.resolveStack()

                withClue("the targeted artifact was destroyed") {
                    game.isOnBattlefield("Artifact Creature") shouldBe false
                }
                withClue("Sawblade Slinger itself is on the battlefield") {
                    game.isOnBattlefield("Sawblade Slinger") shouldBe true
                }
            }

            test("declining both modes leaves the board untouched") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Sawblade Slinger")
                    .withCardOnBattlefield(2, "Artifact Creature")
                    .withLandsOnBattlefield(1, "Forest", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Sawblade Slinger").error shouldBe null
                game.resolveStack()

                val modeDecision = game.getPendingDecision() as? ChooseOptionDecision
                    ?: error("expected a ChooseOptionDecision for the ETB modal; got ${game.getPendingDecision()}")
                val declineIndex = modeDecision.options.indexOfFirst { it.contains("Don't choose", ignoreCase = true) }
                withClue("a decline option should be offered (minChooseCount = 0)") {
                    (declineIndex >= 0) shouldBe true
                }
                game.submitDecision(OptionChosenResponse(modeDecision.id, optionIndex = declineIndex))
                game.resolveStack()

                withClue("the opponent's artifact survives when no mode is chosen") {
                    game.isOnBattlefield("Artifact Creature") shouldBe true
                }
            }
        }
    }
}

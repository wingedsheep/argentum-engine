package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Eye of Vecna (AFR #243) — {2} Legendary Artifact.
 *
 *   When Eye of Vecna enters, you draw a card and you lose 2 life.
 *   At the beginning of your upkeep, you may pay {2}. If you do, you draw a card and you lose 2 life.
 */
class EyeOfVecnaScenarioTest : ScenarioTestBase() {

    init {
        test("entering draws a card and costs its controller 2 life") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Eye of Vecna")
                .withLandsOnBattlefield(1, "Plains", 2)
                .withCardInLibrary(1, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Eye of Vecna").error shouldBe null
            game.resolveStack()

            game.isInHand(1, "Grizzly Bears") shouldBe true
            game.getLifeTotal(1) shouldBe 18
            game.getLifeTotal(2) shouldBe 20
        }

        test("paying {2} in upkeep draws a card and loses 2 life") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Eye of Vecna")
                .withLandsOnBattlefield(1, "Plains", 2)
                .withCardInLibrary(1, "Grizzly Bears")
                .withCardInLibrary(1, "Hill Giant")
                .withActivePlayer(1)
                .inPhase(Phase.BEGINNING, Step.UNTAP)
                .build()
            val handBefore = game.handSize(1)

            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            game.resolveStack()
            game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
            game.answerYesNo(true)
            if (game.getPendingDecision() is SelectManaSourcesDecision) {
                game.submitManaSourcesAutoPay()
            }
            game.resolveStack()

            game.handSize(1) shouldBe handBefore + 1
            game.getLifeTotal(1) shouldBe 18
        }

        test("declining the upkeep payment does nothing") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Eye of Vecna")
                .withLandsOnBattlefield(1, "Plains", 2)
                .withCardInLibrary(1, "Grizzly Bears")
                .withCardInLibrary(1, "Hill Giant")
                .withActivePlayer(1)
                .inPhase(Phase.BEGINNING, Step.UNTAP)
                .build()
            val handBefore = game.handSize(1)

            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            game.resolveStack()
            game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
            game.answerYesNo(false)
            game.resolveStack()

            game.handSize(1) shouldBe handBefore
            game.getLifeTotal(1) shouldBe 20
        }
    }
}

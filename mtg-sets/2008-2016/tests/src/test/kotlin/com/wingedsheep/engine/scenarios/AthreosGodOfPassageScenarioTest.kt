package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Athreos, God of Passage {1}{W}{B}:
 * "Whenever another creature you own dies, return it to your hand unless target opponent pays 3 life."
 */
class AthreosGodOfPassageScenarioTest : ScenarioTestBase() {

    private fun setUp(opponentLife: Int = 20) = scenario()
        .withPlayers("Player1", "Player2")
        .withCardOnBattlefield(1, "Athreos, God of Passage")
        .withCardOnBattlefield(1, "Grizzly Bears")
        .withCardInHand(1, "Lightning Bolt")
        .withLandsOnBattlefield(1, "Mountain", 1)
        .withLifeTotal(2, opponentLife)
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    /** Bolt your own Grizzly Bears; Athreos's trigger targets the only opponent and resolves. */
    private fun TestGame.killBearsAndTargetOpponent() {
        castSpell(1, "Lightning Bolt", findPermanent("Grizzly Bears")!!).error shouldBe null
        resolveStack()
    }

    init {
        test("the opponent declines to pay: the dead creature returns to its owner's hand") {
            val game = setUp()
            game.killBearsAndTargetOpponent()

            withClue("the targeted opponent is offered the 3 life") { game.hasPendingDecision() shouldBe true }
            game.answerYesNo(false)

            game.isInHand(1, "Grizzly Bears") shouldBe true
            game.isInGraveyard(1, "Grizzly Bears") shouldBe false
            game.getLifeTotal(2) shouldBe 20
        }

        test("the opponent pays 3 life: the creature stays in the graveyard") {
            val game = setUp()
            game.killBearsAndTargetOpponent()
            game.answerYesNo(true)

            game.getLifeTotal(2) shouldBe 17
            game.isInGraveyard(1, "Grizzly Bears") shouldBe true
            game.isInHand(1, "Grizzly Bears") shouldBe false
        }

        test("an opponent with less than 3 life can't pay, so the creature returns without a choice") {
            val game = setUp(opponentLife = 2)
            game.killBearsAndTargetOpponent()

            game.hasPendingDecision() shouldBe false
            game.isInHand(1, "Grizzly Bears") shouldBe true
            game.getLifeTotal(2) shouldBe 2
        }

        test("Athreos isn't a creature while devotion to white and black is below seven") {
            val game = setUp()
            val athreos = game.findPermanent("Athreos, God of Passage")!!
            game.state.projectedState.isCreature(athreos) shouldBe false
        }
    }
}

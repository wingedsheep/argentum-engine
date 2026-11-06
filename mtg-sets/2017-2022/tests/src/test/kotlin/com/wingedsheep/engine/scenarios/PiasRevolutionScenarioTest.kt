package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Pia's Revolution {2}{R}:
 * "Whenever a nontoken artifact is put into your graveyard from the battlefield, return that card to
 * your hand unless target opponent has this enchantment deal 3 damage to them."
 */
class PiasRevolutionScenarioTest : ScenarioTestBase() {

    private fun setUp(artifactIsToken: Boolean = false, artifactOwner: Int = 1) = scenario()
        .withPlayers("Player1", "Player2")
        .withCardOnBattlefield(1, "Pia's Revolution")
        .withCardOnBattlefield(artifactOwner, "Millstone", isToken = artifactIsToken)
        .withCardInHand(1, "Shatter")
        .withLandsOnBattlefield(1, "Mountain", 2)
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun TestGame.shatterMillstone() {
        castSpell(1, "Shatter", findPermanent("Millstone")!!).error shouldBe null
        resolveStack()
    }

    init {
        test("the targeted opponent, not you, chooses; taking 3 damage keeps the artifact in the graveyard") {
            val game = setUp()
            game.shatterMillstone()

            val decision = game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
            withClue("the opponent makes the choice") { decision.playerId shouldBe game.player2Id }
            game.answerYesNo(true)

            game.getLifeTotal(2) shouldBe 17
            game.getLifeTotal(1) shouldBe 20
            game.isInGraveyard(1, "Millstone") shouldBe true
            game.isInHand(1, "Millstone") shouldBe false
        }

        test("the opponent refuses the damage: the artifact card returns to your hand") {
            val game = setUp()
            game.shatterMillstone()

            game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>().playerId shouldBe game.player2Id
            game.answerYesNo(false)

            game.getLifeTotal(2) shouldBe 20
            game.isInHand(1, "Millstone") shouldBe true
            game.isInGraveyard(1, "Millstone") shouldBe false
        }

        test("an artifact token going to the graveyard doesn't trigger") {
            val game = setUp(artifactIsToken = true)
            game.shatterMillstone()

            game.hasPendingDecision() shouldBe false
            game.state.stack.isEmpty() shouldBe true
            game.getLifeTotal(2) shouldBe 20
        }

        test("an artifact the opponent owns going to their graveyard doesn't trigger") {
            val game = setUp(artifactOwner = 2)
            game.shatterMillstone()

            game.hasPendingDecision() shouldBe false
            game.getLifeTotal(2) shouldBe 20
            game.isInGraveyard(2, "Millstone") shouldBe true
        }
    }
}

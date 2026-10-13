package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AbilityId
import io.kotest.matchers.shouldBe

/**
 * Phyrexian Dragon Engine: "When this creature enters from your graveyard, you may discard your
 * hand. If you do, draw three cards." Pins that the trigger keys on the *origin* zone — unearth
 * fires it, a cast from hand does not — and that the "may" is honoured both ways.
 */
class PhyrexianDragonEngineScenarioTest : ScenarioTestBase() {
    private val engine = "Phyrexian Dragon Engine"
    private val unearth = AbilityId("unearth_{3}{R}{R}")

    private fun graveyardBoard() = scenario().withPlayers("A", "B")
        .withCardInGraveyard(1, engine)
        .withCardInHand(1, "Grizzly Bears")
        .withCardInHand(1, "Shock")
        .withLandsOnBattlefield(1, "Mountain", 5)
        .withCardInLibrary(1, "Island").withCardInLibrary(1, "Island")
        .withCardInLibrary(1, "Island").withCardInLibrary(1, "Island")
        .withCardInLibrary(2, "Island")
        .withActivePlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()

    private fun TestGame.unearthIt() {
        val action = getLegalActions(1).mapNotNull { it.action as? ActivateAbility }
            .single { it.sourceId == findCardsInGraveyard(1, engine).single() && it.abilityId == unearth }
        execute(action).error shouldBe null
        resolveStack()
    }

    init {
        test("unearthing it lets you discard your hand to draw three") {
            val game = graveyardBoard()
            game.unearthIt()
            game.isOnBattlefield(engine) shouldBe true
            game.hasPendingDecision() shouldBe true
            game.answerYesNo(true).error shouldBe null
            game.resolveStack()
            game.isInGraveyard(1, "Grizzly Bears") shouldBe true
            game.isInGraveyard(1, "Shock") shouldBe true
            game.handSize(1) shouldBe 3
        }

        test("declining keeps the hand and draws nothing") {
            val game = graveyardBoard()
            game.unearthIt()
            game.hasPendingDecision() shouldBe true
            game.answerYesNo(false).error shouldBe null
            game.resolveStack()
            game.isInHand(1, "Grizzly Bears") shouldBe true
            game.isInHand(1, "Shock") shouldBe true
            game.handSize(1) shouldBe 2
        }

        test("casting it from hand does not trigger") {
            val game = scenario().withPlayers("A", "B")
                .withCardInHand(1, engine)
                .withCardInHand(1, "Shock")
                .withLandsOnBattlefield(1, "Mountain", 3)
                .withCardInLibrary(1, "Island").withCardInLibrary(2, "Island")
                .withActivePlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()
            game.castSpell(1, engine).error shouldBe null
            game.resolveStack()
            game.isOnBattlefield(engine) shouldBe true
            game.hasPendingDecision() shouldBe false
            game.isInHand(1, "Shock") shouldBe true
        }
    }
}

package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe

/**
 * Umori, the Collector (IKO #231).
 *
 * "As Umori enters, choose a card type. Spells you cast of the chosen type cost {1} less to cast."
 *
 * The as-enters choice is an `EntersWithChoice(MODE)` over every card type — creature and land
 * included, unlike Stenn — read by the cost static via `ofChosenCardTypeComponent(ChoiceSlot.MODE)`.
 */
class UmoriTheCollectorScenarioTest : ScenarioTestBase() {

    private val umori = "Umori, the Collector"

    private fun TestGame.castUmoriChoosing(type: String) {
        castSpell(1, umori).error shouldBe null
        resolveStack()
        val decision = getPendingDecision() as? ChooseOptionDecision
            ?: error("Expected Umori's card-type choice, got ${getPendingDecision()}")
        withClue("any card type may be chosen, creature and land included") {
            decision.options shouldContainAll listOf("Creature", "Land", "Sorcery", "Artifact")
        }
        submitDecision(OptionChosenResponse(decision.id, decision.options.indexOf(type))).error shouldBe null
        resolveStack()
        isOnBattlefield(umori) shouldBe true
    }

    private fun setup() = scenario()
        .withPlayers("Player1", "Player2")
        .withCardInHand(1, umori)
        .withCardInHand(1, "Grizzly Bears")
        .withLandsOnBattlefield(1, "Forest", 5)
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("creature chosen: Grizzly Bears costs {G} and is castable off the one Forest left") {
            val game = setup()
            game.castUmoriChoosing("Creature")

            game.castSpell(1, "Grizzly Bears").error shouldBe null
            game.resolveStack()
            game.isOnBattlefield("Grizzly Bears") shouldBe true
        }

        test("sorcery chosen: a creature spell is not reduced, so one Forest can't pay for Grizzly Bears") {
            val game = setup()
            game.castUmoriChoosing("Sorcery")

            (game.castSpell(1, "Grizzly Bears").error != null) shouldBe true
            game.isInHand(1, "Grizzly Bears") shouldBe true
        }
    }
}

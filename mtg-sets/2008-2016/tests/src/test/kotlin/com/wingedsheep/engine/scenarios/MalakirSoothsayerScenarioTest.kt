package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Malakir Soothsayer — {4}{B} 4/4 Vampire Shaman Ally.
 * "Cohort — {T}, Tap an untapped Ally you control: You draw a card and you lose 1 life."
 */
class MalakirSoothsayerScenarioTest : ScenarioTestBase() {
    init {
        val cohortId = cardRegistry.getCard("Malakir Soothsayer")!!.script.activatedAbilities.single().id

        fun board(partner: String) = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "Malakir Soothsayer")
            .withCardOnBattlefield(1, partner)
            .withCardInLibrary(1, "Swamp")
            .withCardInLibrary(1, "Swamp")
            .withActivePlayer(1)
            .withPriorityPlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        test("tapping it and another Ally draws a card and loses 1 life") {
            val game = board("Kor Sky Climber")
            val soothsayer = game.findPermanent("Malakir Soothsayer")!!
            val ally = game.findPermanent("Kor Sky Climber")!!
            val handBefore = game.handSize(1)

            val result = game.execute(
                ActivateAbility(
                    game.player1Id, soothsayer, cohortId,
                    costPayment = AdditionalCostPayment(tappedPermanents = listOf(ally)),
                )
            )
            withClue("cohort should be payable with another Ally: ${result.error}") { result.error shouldBe null }
            game.resolveStack()

            game.state.getEntity(soothsayer)?.has<TappedComponent>() shouldBe true
            game.state.getEntity(ally)?.has<TappedComponent>() shouldBe true
            game.handSize(1) shouldBe handBefore + 1
            game.getLifeTotal(1) shouldBe 19
            game.getLifeTotal(2) shouldBe 20
        }

        test("a non-Ally creature can't pay the cohort cost") {
            val game = board("Grizzly Bears")
            val soothsayer = game.findPermanent("Malakir Soothsayer")!!
            val bears = game.findPermanent("Grizzly Bears")!!

            game.execute(
                ActivateAbility(
                    game.player1Id, soothsayer, cohortId,
                    costPayment = AdditionalCostPayment(tappedPermanents = listOf(bears)),
                )
            ).error shouldNotBe null
            game.getLifeTotal(1) shouldBe 20
        }
    }
}

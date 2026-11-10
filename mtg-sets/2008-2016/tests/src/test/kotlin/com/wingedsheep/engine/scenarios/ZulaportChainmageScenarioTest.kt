package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Zulaport Chainmage — {3}{B} 4/2 Human Shaman Ally.
 * "Cohort — {T}, Tap an untapped Ally you control: Target opponent loses 2 life."
 */
class ZulaportChainmageScenarioTest : ScenarioTestBase() {
    init {
        val cohortId = cardRegistry.getCard("Zulaport Chainmage")!!.script.activatedAbilities.single().id

        fun board() = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "Zulaport Chainmage")
            .withCardOnBattlefield(1, "Kor Sky Climber")
            .withCardOnBattlefield(1, "Grizzly Bears")
            .withActivePlayer(1)
            .withPriorityPlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        test("cohort taps both Allies and the opponent loses 2 life") {
            val game = board()
            val chainmage = game.findPermanent("Zulaport Chainmage")!!
            val ally = game.findPermanent("Kor Sky Climber")!!

            val result = game.execute(
                ActivateAbility(
                    game.player1Id, chainmage, cohortId, listOf(ChosenTarget.Player(game.player2Id)),
                    costPayment = AdditionalCostPayment(tappedPermanents = listOf(ally)),
                )
            )
            withClue("cohort should be payable with another Ally: ${result.error}") { result.error shouldBe null }
            game.resolveStack()

            game.getLifeTotal(2) shouldBe 18
            game.getLifeTotal(1) shouldBe 20
            game.state.getEntity(chainmage)?.has<TappedComponent>() shouldBe true
            game.state.getEntity(ally)?.has<TappedComponent>() shouldBe true
        }

        test("a non-Ally creature can't pay the cohort cost") {
            val game = board()
            val chainmage = game.findPermanent("Zulaport Chainmage")!!
            val bears = game.findPermanent("Grizzly Bears")!!

            game.execute(
                ActivateAbility(
                    game.player1Id, chainmage, cohortId, listOf(ChosenTarget.Player(game.player2Id)),
                    costPayment = AdditionalCostPayment(tappedPermanents = listOf(bears)),
                )
            ).error shouldNotBe null
            game.getLifeTotal(2) shouldBe 20
        }
    }
}

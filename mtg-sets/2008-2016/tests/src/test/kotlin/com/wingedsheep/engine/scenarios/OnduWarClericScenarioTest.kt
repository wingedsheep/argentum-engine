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
 * Ondu War Cleric — {1}{W} 2/2 Human Cleric Ally.
 * "Cohort — {T}, Tap an untapped Ally you control: You gain 2 life."
 */
class OnduWarClericScenarioTest : ScenarioTestBase() {
    init {
        val cohortId = cardRegistry.getCard("Ondu War Cleric")!!.script.activatedAbilities.single().id

        fun board() = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "Ondu War Cleric")
            .withCardOnBattlefield(1, "Kor Sky Climber")
            .withCardOnBattlefield(1, "Grizzly Bears")
            .withActivePlayer(1)
            .withPriorityPlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        test("cohort taps both Allies and gains 2 life") {
            val game = board()
            val cleric = game.findPermanent("Ondu War Cleric")!!
            val ally = game.findPermanent("Kor Sky Climber")!!

            val result = game.execute(
                ActivateAbility(
                    game.player1Id, cleric, cohortId,
                    costPayment = AdditionalCostPayment(tappedPermanents = listOf(ally)),
                )
            )
            withClue("cohort should be payable with another Ally: ${result.error}") { result.error shouldBe null }
            game.resolveStack()

            game.state.getEntity(cleric)?.has<TappedComponent>() shouldBe true
            game.state.getEntity(ally)?.has<TappedComponent>() shouldBe true
            game.getLifeTotal(1) shouldBe 22
            game.getLifeTotal(2) shouldBe 20
        }

        test("a non-Ally creature can't pay the cohort cost") {
            val game = board()
            val cleric = game.findPermanent("Ondu War Cleric")!!
            val bears = game.findPermanent("Grizzly Bears")!!

            game.execute(
                ActivateAbility(
                    game.player1Id, cleric, cohortId,
                    costPayment = AdditionalCostPayment(tappedPermanents = listOf(bears)),
                )
            ).error shouldNotBe null
            game.getLifeTotal(1) shouldBe 20
        }
    }
}

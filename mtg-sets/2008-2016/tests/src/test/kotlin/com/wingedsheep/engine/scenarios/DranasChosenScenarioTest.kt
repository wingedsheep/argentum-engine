package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Drana's Chosen — {3}{B} 2/2 Vampire Shaman Ally.
 * "Cohort — {T}, Tap an untapped Ally you control: Create a tapped 2/2 black Zombie creature token."
 */
class DranasChosenScenarioTest : ScenarioTestBase() {
    init {
        val cohortId = cardRegistry.getCard("Drana's Chosen")!!.script.activatedAbilities.single().id

        fun board(partner: String) = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "Drana's Chosen")
            .withCardOnBattlefield(1, partner)
            .withActivePlayer(1)
            .withPriorityPlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        test("tapping it and another Ally creates a tapped 2/2 black Zombie") {
            val game = board("Kor Sky Climber")
            val chosen = game.findPermanent("Drana's Chosen")!!
            val ally = game.findPermanent("Kor Sky Climber")!!

            val result = game.execute(
                ActivateAbility(
                    game.player1Id, chosen, cohortId,
                    costPayment = AdditionalCostPayment(tappedPermanents = listOf(ally)),
                )
            )
            withClue("cohort should be payable with another Ally: ${result.error}") { result.error shouldBe null }
            game.resolveStack()

            withClue("both Allies were tapped to pay the cost") {
                game.state.getEntity(chosen)?.has<TappedComponent>() shouldBe true
                game.state.getEntity(ally)?.has<TappedComponent>() shouldBe true
            }
            val zombies = game.findPermanents("Zombie Token")
            zombies shouldHaveSize 1
            val zombie = zombies.single()
            game.state.projectedState.getPower(zombie) shouldBe 2
            game.state.projectedState.getToughness(zombie) shouldBe 2
            game.state.projectedState.getColors(zombie) shouldBe setOf(Color.BLACK.name)
            withClue("the token enters tapped") {
                game.state.getEntity(zombie)?.has<TappedComponent>() shouldBe true
            }
        }

        test("a non-Ally creature can't pay the cohort cost") {
            val game = board("Grizzly Bears")
            val chosen = game.findPermanent("Drana's Chosen")!!
            val bears = game.findPermanent("Grizzly Bears")!!

            game.execute(
                ActivateAbility(
                    game.player1Id, chosen, cohortId,
                    costPayment = AdditionalCostPayment(tappedPermanents = listOf(bears)),
                )
            ).error shouldNotBe null
            game.findPermanents("Zombie Token") shouldHaveSize 0
        }
    }
}

package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Ravenous Squirrel (MH2 #211) — "Whenever you sacrifice an artifact or creature, put a +1/+1
 * counter on this creature. {1}{B}{G}, Sacrifice an artifact or creature: You gain 1 life and draw
 * a card." Paying its own cost with a creature both triggers the counter and resolves the draw.
 */
class RavenousSquirrelScenarioTest : ScenarioTestBase() {

    init {
        context("Ravenous Squirrel") {

            test("sacrificing a creature to its ability gains 1, draws 1, and grows the Squirrel") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Ravenous Squirrel")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val squirrel = game.findPermanent("Ravenous Squirrel")!!
                val bears = game.findPermanent("Grizzly Bears")!!
                val ability = cardRegistry.getCard("Ravenous Squirrel")!!.activatedAbilities.first().id
                val lifeBefore = game.getLifeTotal(1)
                val handBefore = game.handSize(1)

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = squirrel,
                        abilityId = ability,
                        costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(bears)),
                    )
                )
                withClue("activation should succeed: ${result.error}") { result.error shouldBe null }
                if (game.getPendingDecision() is SelectManaSourcesDecision) {
                    game.submitManaSourcesAutoPay()
                }
                game.resolveStack()

                withClue("Grizzly Bears was sacrificed") { game.isInGraveyard(1, "Grizzly Bears") shouldBe true }
                withClue("you gain 1 life") { game.getLifeTotal(1) shouldBe lifeBefore + 1 }
                withClue("you draw a card") { game.handSize(1) shouldBe handBefore + 1 }
                withClue("the sacrifice trigger put a +1/+1 counter on the Squirrel") {
                    game.state.getEntity(squirrel)?.get<CountersComponent>()
                        ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
                }
            }
        }
    }
}

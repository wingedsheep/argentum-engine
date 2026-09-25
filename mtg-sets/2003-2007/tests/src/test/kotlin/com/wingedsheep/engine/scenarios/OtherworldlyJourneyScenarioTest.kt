package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Otherworldly Journey (CHK #37) — "Exile target creature. At the beginning of the next end step,
 * return that card to the battlefield under its owner's control with a +1/+1 counter on it."
 */
class OtherworldlyJourneyScenarioTest : ScenarioTestBase() {

    init {
        context("Otherworldly Journey") {

            test("exiles the creature, then returns it at end step to its owner with a +1/+1 counter") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardInHand(1, "Otherworldly Journey")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                game.castSpell(1, "Otherworldly Journey", bears).error shouldBe null
                game.resolveStack()

                withClue("the creature is in exile until the end step") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe false
                    game.isInExile(2, "Grizzly Bears") shouldBe true
                }

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()

                val returned = game.findPermanent("Grizzly Bears")
                withClue("it came back at the beginning of the end step") { (returned != null) shouldBe true }
                withClue("under its owner's control") {
                    game.state.projectedState.getController(returned!!) shouldBe game.player2Id
                }
                withClue("with a +1/+1 counter, as a 3/3") {
                    game.state.getEntity(returned!!)!!.get<CountersComponent>()!!
                        .getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
                    game.state.projectedState.getPower(returned) shouldBe 3
                    game.state.projectedState.getToughness(returned) shouldBe 3
                }
            }
        }
    }
}

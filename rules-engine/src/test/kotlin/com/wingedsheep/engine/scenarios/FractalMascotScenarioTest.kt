package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Fractal Mascot (Secrets of Strixhaven #189).
 *
 * Fractal Mascot ({4}{G}{U}, 6/6, Fractal Elk):
 *   Trample
 *   When this creature enters, tap target creature an opponent controls. Put a stun counter on it.
 *
 * Exercises the ETB-targeted tap + stun-counter composition (Tap.then(AddCounters STUN)).
 */
class FractalMascotScenarioTest : ScenarioTestBase() {

    private fun stunCounters(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.STUN) ?: 0

    private fun isTapped(game: TestGame, id: EntityId): Boolean =
        game.state.getEntity(id)?.has<TappedComponent>() ?: false

    init {
        context("Fractal Mascot — ETB tap + stun counter") {

            test("entering taps the opponent's creature and puts a stun counter on it") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Fractal Mascot")
                    .withLandsOnBattlefield(1, "Forest", 5)
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withCardOnBattlefield(2, "Grizzly Bears") // untapped 2/2
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                withClue("Grizzly Bears starts untapped") { isTapped(game, bears) shouldBe false }

                val cast = game.castSpell(1, "Fractal Mascot")
                withClue("Casting Fractal Mascot should succeed: ${cast.error}") {
                    cast.error shouldBe null
                }
                game.resolveStack() // Mascot enters; ETB trigger goes on the stack

                // ETB targets a creature an opponent controls — select the opponent's Grizzly Bears
                // (auto-assigned when it's the only legal target).
                if (game.hasPendingDecision()) {
                    game.selectTargets(listOf(bears))
                }
                game.resolveStack() // ETB resolves: tap + stun counter

                withClue("The opponent's creature is tapped") { isTapped(game, bears) shouldBe true }
                withClue("The opponent's creature has one stun counter") {
                    stunCounters(game, bears) shouldBe 1
                }
            }
        }
    }
}

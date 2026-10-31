package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Mysterious Pathlighter (ELD #22).
 *
 *   Each creature you control that has an Adventure enters with an additional +1/+1 counter on it.
 *
 * "Has an Adventure" is the card's layout, so an adventurer cast straight from hand as a creature
 * (never having gone on its Adventure) still qualifies; a plain creature does not.
 */
class MysteriousPathlighterScenarioTest : ScenarioTestBase() {

    private fun TestGame.plusCounters(name: String): Int =
        state.getEntity(findPermanent(name)!!)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    private fun board(creature: String, controllerOfPathlighter: Int = 1): TestGame =
        scenario()
            .withPlayers("Player1", "Player2")
            .withCardOnBattlefield(controllerOfPathlighter, "Mysterious Pathlighter")
            .withCardInHand(1, creature)
            .withLandsOnBattlefield(1, "Plains", 2)
            .withLandsOnBattlefield(1, "Forest", 2)
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

    init {
        context("Mysterious Pathlighter") {

            test("a creature with an Adventure cast from hand enters with a +1/+1 counter") {
                val game = board("Shepherd of the Flock")
                game.castSpell(1, "Shepherd of the Flock").error shouldBe null
                game.resolveStack()
                withClue("Shepherd of the Flock has an Adventure even though it never went on it") {
                    game.plusCounters("Shepherd of the Flock") shouldBe 1
                }
            }

            test("a creature without an Adventure gets no counter") {
                val game = board("Grizzly Bears")
                game.castSpell(1, "Grizzly Bears").error shouldBe null
                game.resolveStack()
                game.plusCounters("Grizzly Bears") shouldBe 0
            }

            test("an opponent's Pathlighter doesn't help your adventurers") {
                val game = board("Shepherd of the Flock", controllerOfPathlighter = 2)
                game.castSpell(1, "Shepherd of the Flock").error shouldBe null
                game.resolveStack()
                game.plusCounters("Shepherd of the Flock") shouldBe 0
            }
        }
    }
}

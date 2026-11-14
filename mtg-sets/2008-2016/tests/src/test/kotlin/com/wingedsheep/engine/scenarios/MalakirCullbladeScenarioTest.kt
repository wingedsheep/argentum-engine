package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Malakir Cullblade (ORI #108) — {1}{B} Creature — Vampire Warrior, 1/1.
 *
 *   Whenever a creature an opponent controls dies, put a +1/+1 counter on this creature.
 */
class MalakirCullbladeScenarioTest : ScenarioTestBase() {

    /** Bolt the Grizzly Bears controlled by [bearsController] and return the Cullblade's +1/+1 count. */
    private fun boltBearsOf(bearsController: Int): Pair<TestGame, Int> {
        val game = scenario()
            .withPlayers("Player1", "Player2")
            .withCardOnBattlefield(1, "Malakir Cullblade")
            .withCardInHand(1, "Lightning Bolt")
            .withLandsOnBattlefield(1, "Mountain", 1)
            .withCardOnBattlefield(bearsController, "Grizzly Bears")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        val bears = game.findPermanent("Grizzly Bears")!!
        val bolt = game.castSpell(1, "Lightning Bolt", bears)
        withClue("Bolt should be cast: ${bolt.error}") { bolt.error shouldBe null }
        game.resolveStack()
        withClue("Grizzly Bears died") { game.isInGraveyard(bearsController, "Grizzly Bears") shouldBe true }

        val cullblade = game.findPermanent("Malakir Cullblade")!!
        val counters = game.state.getEntity(cullblade)?.get<CountersComponent>()
            ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0
        return game to counters
    }

    init {
        context("Malakir Cullblade") {

            test("an opponent's creature dying puts a +1/+1 counter on it") {
                val (game, counters) = boltBearsOf(bearsController = 2)

                counters shouldBe 1
                val cullblade = game.findPermanent("Malakir Cullblade")!!
                game.state.projectedState.getPower(cullblade) shouldBe 2
                game.state.projectedState.getToughness(cullblade) shouldBe 2
            }

            test("your own creature dying adds no counter") {
                val (game, counters) = boltBearsOf(bearsController = 1)

                counters shouldBe 0
                game.state.projectedState.getPower(game.findPermanent("Malakir Cullblade")!!) shouldBe 1
            }
        }
    }
}

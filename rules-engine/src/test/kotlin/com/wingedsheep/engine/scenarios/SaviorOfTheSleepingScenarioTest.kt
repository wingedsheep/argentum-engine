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

/** Scenario tests for Savior of the Sleeping. */
class SaviorOfTheSleepingScenarioTest : ScenarioTestBase() {

    private fun stunCounters(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.STUN) ?: 0

    private fun isTapped(game: TestGame, id: EntityId): Boolean =
        game.state.getEntity(id)?.get<TappedComponent>() != null

    private fun plusOneCounters(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    init {
        context("Savior of the Sleeping — grows when your enchantments die") {
            test("a Role token falling off the battlefield adds a +1/+1 counter") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Savior of the Sleeping", summoningSickness = false)
                    .withCardOnBattlefield(1, "Castle")
                    .withCardInHand(1, "Disenchant")
                    .withLandsOnBattlefield(1, "Plains", 3)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val savior = game.findPermanent("Savior of the Sleeping")!!
                plusOneCounters(game, savior) shouldBe 0

                // Castle is a plain static enchantment (no death trigger of its own), so
                // Disenchanting it isolates the Savior's trigger.
                game.castSpell(1, "Disenchant", game.findPermanent("Castle")!!).error shouldBe null
                game.resolveStack()

                withClue("an enchantment you control hit the graveyard from the battlefield") {
                    plusOneCounters(game, savior) shouldBe 1
                }
                withClue("2/3 base plus one +1/+1 counter") {
                    game.state.projectedState.getPower(savior) shouldBe 3
                    game.state.projectedState.getToughness(savior) shouldBe 4
                }
            }
        }
    }
}

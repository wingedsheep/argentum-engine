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

/** Scenario tests for Spiteful Hexmage. */
class SpitefulHexmageScenarioTest : ScenarioTestBase() {

    private fun stunCounters(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.STUN) ?: 0

    private fun isTapped(game: TestGame, id: EntityId): Boolean =
        game.state.getEntity(id)?.get<TappedComponent>() != null

    private fun plusOneCounters(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    init {
        context("Spiteful Hexmage — enters with a Cursed Role") {
            test("the Cursed Role sets the enchanted creature to base 1/1") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Spiteful Hexmage")
                    .withCardOnBattlefield(1, "Craw Wurm", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val wurm = game.findPermanent("Craw Wurm")!!

                game.castSpell(1, "Spiteful Hexmage").error shouldBe null
                game.resolveStack() // Hexmage enters -> ETB trigger asks for its target

                game.selectTargets(listOf(wurm)).error shouldBe null
                game.resolveStack()

                withClue("a Cursed Role token was created") {
                    (game.findPermanent("Cursed Role") != null) shouldBe true
                }
                withClue("Cursed Role sets base P/T to 1/1, overriding the Wurm's 6/4") {
                    game.state.projectedState.getPower(wurm) shouldBe 1
                    game.state.projectedState.getToughness(wurm) shouldBe 1
                }
            }
        }
    }
}

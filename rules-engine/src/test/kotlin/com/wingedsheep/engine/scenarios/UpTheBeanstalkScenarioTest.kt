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

/** Scenario tests for Up the Beanstalk. */
class UpTheBeanstalkScenarioTest : ScenarioTestBase() {

    private fun stunCounters(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.STUN) ?: 0

    private fun isTapped(game: TestGame, id: EntityId): Boolean =
        game.state.getEntity(id)?.get<TappedComponent>() != null

    private fun plusOneCounters(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    init {
        context("Up the Beanstalk — draws on enter and on expensive spells") {
            test("entering draws one card, then a mana value 6 spell draws another") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Up the Beanstalk")
                    .withCardInHand(1, "Craw Wurm")
                    .withLandsOnBattlefield(1, "Forest", 8)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val handBefore = game.handSize(1)

                game.castSpell(1, "Up the Beanstalk").error shouldBe null
                game.resolveStack()

                withClue("Beanstalk left hand (-1) and its enters trigger drew a card (+1)") {
                    game.handSize(1) shouldBe handBefore
                }

                game.castSpell(1, "Craw Wurm").error shouldBe null
                game.resolveStack()

                withClue("Craw Wurm is mana value 6, so casting it drew another card") {
                    // Craw Wurm left hand (-1), the cast trigger drew (+1).
                    game.handSize(1) shouldBe handBefore
                }
            }
        }
    }
}

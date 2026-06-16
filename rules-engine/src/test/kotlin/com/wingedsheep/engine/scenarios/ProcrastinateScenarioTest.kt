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
 * Procrastinate {X}{U} Sorcery — "Tap target creature. Put twice X stun counters on it."
 *
 * Verifies the tap and the dynamic `2 × X` stun-counter count.
 */
class ProcrastinateScenarioTest : ScenarioTestBase() {

    private fun stunCounters(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.STUN) ?: 0

    private fun isTapped(game: TestGame, id: EntityId): Boolean =
        game.state.getEntity(id)?.get<TappedComponent>() != null

    init {
        context("Procrastinate — tap and twice-X stun counters") {

            test("X=2 taps the creature and adds 4 stun counters") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Procrastinate")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Island", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                game.castXSpell(1, "Procrastinate", xValue = 2, targetId = bears).error shouldBe null
                game.resolveStack()

                withClue("Grizzly Bears should be tapped") {
                    isTapped(game, bears) shouldBe true
                }
                withClue("twice X = 2 × 2 = 4 stun counters") {
                    stunCounters(game, bears) shouldBe 4
                }
            }

            test("X=0 still taps the creature but adds no stun counters") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Procrastinate")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                game.castXSpell(1, "Procrastinate", xValue = 0, targetId = bears).error shouldBe null
                game.resolveStack()

                withClue("Grizzly Bears should be tapped") {
                    isTapped(game, bears) shouldBe true
                }
                withClue("twice X = 0 stun counters") {
                    stunCounters(game, bears) shouldBe 0
                }
            }
        }
    }
}

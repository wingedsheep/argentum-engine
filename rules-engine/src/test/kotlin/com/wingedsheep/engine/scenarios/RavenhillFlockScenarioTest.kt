package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Ravenhill Flock (HOB #52) — {3}{U} Creature — Bird 1/2.
 *
 * "Flying
 *  Whenever you draw a card, put a +1/+1 counter on this creature."
 *
 * The trigger fires per individual card drawn (CR 121.2), so a "draw two" puts on two counters;
 * an opponent's draw puts on none.
 */
class RavenhillFlockScenarioTest : ScenarioTestBase() {

    private fun plusOneCounters(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    init {
        context("Ravenhill Flock") {

            test("it is a 1/2 flier") {
                val g = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Ravenhill Flock")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val flock = g.findPermanent("Ravenhill Flock")!!
                g.state.projectedState.getPower(flock) shouldBe 1
                g.state.projectedState.getToughness(flock) shouldBe 2
                g.state.projectedState.hasKeyword(flock, Keyword.FLYING) shouldBe true
            }

            test("drawing two cards puts on two counters") {
                val g = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Ravenhill Flock")
                    .withCardInHand(1, "Divination")
                    .withLandsOnBattlefield(1, "Island", 3)
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(1, "Island")
                    .withActivePlayer(1)
                    .withPriorityPlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val flock = g.findPermanent("Ravenhill Flock")!!
                g.castSpell(1, "Divination").error shouldBe null
                g.resolveStack()

                withClue("the trigger fires once per card drawn, not once per draw effect") {
                    plusOneCounters(g, flock) shouldBe 2
                    g.state.projectedState.getPower(flock) shouldBe 3
                    g.state.projectedState.getToughness(flock) shouldBe 4
                }
            }

            test("an opponent drawing puts on nothing") {
                val g = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Ravenhill Flock")
                    .withCardInHand(2, "Divination")
                    .withLandsOnBattlefield(2, "Island", 3)
                    .withCardInLibrary(2, "Island")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(2)
                    .withPriorityPlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val flock = g.findPermanent("Ravenhill Flock")!!
                g.castSpell(2, "Divination").error shouldBe null
                g.resolveStack()

                withClue("\"whenever you draw\" is scoped to the Flock's controller") {
                    plusOneCounters(g, flock) shouldBe 0
                }
            }
        }
    }
}

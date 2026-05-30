package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Kavu Monarch:
 * - Kavu creatures have trample. (lord, affects all Kavu including itself)
 * - Whenever another Kavu enters, put a +1/+1 counter on this creature.
 *
 * Base stats are 3/3.
 */
class KavuMonarchScenarioTest : ScenarioTestBase() {

    private val stateProjector = StateProjector()

    private fun ScenarioTestBase.TestGame.getCounters(entityId: EntityId): Int {
        return state.entities[entityId]
            ?.get<CountersComponent>()
            ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0
    }

    init {
        context("Kavu Monarch") {

            test("grants trample to itself and other Kavu") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Kavu Monarch")
                    .withCardOnBattlefield(1, "Kavu Chameleon")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val projected = stateProjector.project(game.state)
                val monarchId = game.findPermanent("Kavu Monarch")!!
                val chameleonId = game.findPermanent("Kavu Chameleon")!!

                withClue("Kavu Monarch should have trample (lord affects itself)") {
                    projected.hasKeyword(monarchId, Keyword.TRAMPLE) shouldBe true
                }
                withClue("Other Kavu should have trample") {
                    projected.hasKeyword(chameleonId, Keyword.TRAMPLE) shouldBe true
                }
            }

            test("gets a +1/+1 counter when another Kavu enters") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Kavu Monarch")
                    .withCardInHand(1, "Kavu Chameleon")
                    .withLandsOnBattlefield(1, "Forest", 5)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val monarchId = game.findPermanent("Kavu Monarch")!!
                withClue("Monarch starts with no counters") {
                    game.getCounters(monarchId) shouldBe 0
                }

                // Cast Kavu Chameleon (another Kavu entering).
                val castResult = game.castSpell(1, "Kavu Chameleon")
                withClue("Kavu Chameleon should cast: ${castResult.error}") {
                    castResult.error shouldBe null
                }
                game.resolveStack()

                withClue("Monarch should gain a +1/+1 counter when another Kavu enters") {
                    game.getCounters(monarchId) shouldBe 1
                }
            }
        }
    }
}

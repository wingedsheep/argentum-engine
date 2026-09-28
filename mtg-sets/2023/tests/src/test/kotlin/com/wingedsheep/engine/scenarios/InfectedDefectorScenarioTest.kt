package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Infected Defector — "When this creature dies, incubate 3."
 */
class InfectedDefectorScenarioTest : ScenarioTestBase() {
    init {
        context("Infected Defector") {
            test("dying creates an Incubator token with three +1/+1 counters for its controller") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Infected Defector")
                    .withCardInHand(2, "Lightning Bolt")
                    .withLandsOnBattlefield(2, "Mountain", 1)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val defector = game.findPermanent("Infected Defector")!!
                game.castSpell(2, "Lightning Bolt", defector).error shouldBe null
                game.resolveStack()
                withClue("3 damage is lethal to a 4/3") {
                    game.isInGraveyard(1, "Infected Defector") shouldBe true
                }

                val incubator = game.findPermanent("Incubator").shouldNotBeNull()
                game.state.getEntity(incubator)?.get<CountersComponent>()
                    ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 3
                withClue("the Incubator belongs to the Defector's controller") {
                    game.state.getBattlefield(game.player1Id).contains(incubator) shouldBe true
                }
            }
        }
    }
}

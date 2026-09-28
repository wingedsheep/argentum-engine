package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Eyes of Gitaxias — "Incubate 3. Draw a card."
 */
class EyesOfGitaxiasScenarioTest : ScenarioTestBase() {
    init {
        context("Eyes of Gitaxias") {
            test("creates an Incubator with three counters and draws a card") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Eyes of Gitaxias")
                    .withLandsOnBattlefield(1, "Island", 3)
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val handBefore = game.state.getHand(game.player1Id).size

                game.castSpell(1, "Eyes of Gitaxias").error shouldBe null
                game.resolveStack()

                val incubator = game.findPermanent("Incubator").shouldNotBeNull()
                game.state.getEntity(incubator)?.get<CountersComponent>()
                    ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 3
                // spell left hand (-1), drew one (+1)
                game.state.getHand(game.player1Id).size shouldBe handBefore
            }
        }
    }
}

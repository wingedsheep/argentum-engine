package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

/**
 * Essence of Orthodoxy — "Whenever this creature or another Phyrexian you control enters, incubate 2."
 */
class EssenceOfOrthodoxyScenarioTest : ScenarioTestBase() {
    init {
        context("Essence of Orthodoxy") {
            test("its own entry incubates 2") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Essence of Orthodoxy")
                    .withLandsOnBattlefield(1, "Plains", 5)
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Essence of Orthodoxy").error shouldBe null
                game.resolveStack()

                val incubators = game.findAllPermanents("Incubator")
                incubators shouldHaveSize 1
                game.state.getEntity(incubators.single())?.get<CountersComponent>()
                    ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 2
                game.state.getBattlefield(game.player1Id).contains(incubators.single()) shouldBe true
            }

            test("another Phyrexian you control entering incubates 2; a non-Phyrexian does not") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Essence of Orthodoxy")
                    .withCardInHand(1, "Infected Defector")
                    .withCardInHand(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Plains", 5)
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Grizzly Bears").error shouldBe null
                game.resolveStack()
                withClue("a non-Phyrexian entering doesn't trigger") {
                    game.findAllPermanents("Incubator") shouldHaveSize 0
                }

                game.castSpell(1, "Infected Defector").error shouldBe null
                game.resolveStack()
                val incubators = game.findAllPermanents("Incubator")
                incubators shouldHaveSize 1
                game.state.getEntity(incubators.single())?.get<CountersComponent>()
                    ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 2
            }

            test("a Phyrexian an opponent controls entering does not trigger it") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Essence of Orthodoxy")
                    .withCardInHand(2, "Infected Defector")
                    .withLandsOnBattlefield(2, "Plains", 5)
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(2, "Infected Defector").error shouldBe null
                game.resolveStack()
                game.findAllPermanents("Incubator") shouldHaveSize 0
            }
        }
    }
}

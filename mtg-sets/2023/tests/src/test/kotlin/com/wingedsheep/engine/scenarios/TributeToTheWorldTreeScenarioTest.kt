package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Tribute to the World Tree (MOM #211) — "Whenever a creature you control enters, draw a card if
 * its power is 3 or greater. Otherwise, put two +1/+1 counters on it."
 */
class TributeToTheWorldTreeScenarioTest : ScenarioTestBase() {

    init {
        context("Tribute to the World Tree") {

            test("a creature with power less than 3 gets two +1/+1 counters and no card is drawn") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Tribute to the World Tree")
                    .withCardInHand(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Grizzly Bears").error shouldBe null
                game.resolveStack()

                val bears = game.findPermanent("Grizzly Bears")!!
                withClue("Grizzly Bears got two +1/+1 counters") {
                    game.state.getEntity(bears)?.get<CountersComponent>()
                        ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 2
                }
                withClue("no card drawn") { game.handSize(1) shouldBe 0 }
            }

            test("a creature with power 3 or greater draws a card and gets no counters") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Tribute to the World Tree")
                    .withCardInHand(1, "Hill Giant")
                    .withLandsOnBattlefield(1, "Mountain", 4)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Hill Giant").error shouldBe null
                game.resolveStack()

                val giant = game.findPermanent("Hill Giant")!!
                withClue("Hill Giant got no counters") {
                    (game.state.getEntity(giant)?.get<CountersComponent>()
                        ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0) shouldBe 0
                }
                withClue("drew one card") { game.handSize(1) shouldBe 1 }
            }

            test("an opponent's creature entering does not trigger it") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Tribute to the World Tree")
                    .withCardInHand(2, "Grizzly Bears")
                    .withLandsOnBattlefield(2, "Forest", 2)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(2, "Grizzly Bears").error shouldBe null
                game.resolveStack()

                val bears = game.findPermanent("Grizzly Bears")!!
                (game.state.getEntity(bears)?.get<CountersComponent>()
                    ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0) shouldBe 0
                game.handSize(1) shouldBe 0
            }
        }
    }
}

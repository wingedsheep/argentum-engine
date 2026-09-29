package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Tangled Skyline (MOM #209) — "When this enchantment enters, you gain 5 life and incubate 5.
 * Phyrexians you control have reach."
 */
class TangledSkylineScenarioTest : ScenarioTestBase() {

    init {
        context("Tangled Skyline") {

            test("enters: gain 5 life and incubate 5") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Tangled Skyline")
                    .withLandsOnBattlefield(1, "Forest", 5)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Tangled Skyline").error shouldBe null
                game.resolveStack()

                withClue("gained 5 life") { game.getLifeTotal(1) shouldBe 25 }
                val incubator = game.findPermanent("Incubator")
                withClue("an Incubator with five +1/+1 counters") {
                    incubator shouldNotBe null
                    game.state.getEntity(incubator!!)?.get<CountersComponent>()
                        ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 5
                }
            }

            test("Phyrexians you control have reach; others and opponents' Phyrexians do not") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Tangled Skyline")
                    .withCardOnBattlefield(1, "Grafted Butcher")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Grafted Butcher")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val projected = game.state.projectedState
                val butchers = game.findAllPermanents("Grafted Butcher")
                val mine = butchers.single { projected.getController(it) == game.player1Id }
                val theirs = butchers.single { projected.getController(it) != game.player1Id }

                projected.hasKeyword(mine, Keyword.REACH) shouldBe true
                projected.hasKeyword(theirs, Keyword.REACH) shouldBe false
                projected.hasKeyword(game.findPermanent("Grizzly Bears")!!, Keyword.REACH) shouldBe false
            }
        }
    }
}

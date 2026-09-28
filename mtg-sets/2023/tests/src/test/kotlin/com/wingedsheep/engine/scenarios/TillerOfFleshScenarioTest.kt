package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Tiller of Flesh — "Whenever you cast a spell that targets one or more permanents, incubate 2."
 */
class TillerOfFleshScenarioTest : ScenarioTestBase() {
    init {
        context("Tiller of Flesh") {
            test("casting a spell that targets a permanent incubates 2") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Tiller of Flesh")
                    .withCardInHand(1, "Lightning Bolt")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                game.castSpell(1, "Lightning Bolt", bears).error shouldBe null
                game.resolveStack()

                game.findPermanent("Grizzly Bears") shouldBe null
                val incubator = game.findPermanent("Incubator").shouldNotBeNull()
                game.state.getEntity(incubator)?.get<CountersComponent>()
                    ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 2
                game.state.getBattlefield(game.player1Id).contains(incubator) shouldBe true
            }

            test("a spell that only targets a player does not trigger") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Tiller of Flesh")
                    .withCardInHand(1, "Lightning Bolt")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpellTargetingPlayer(1, "Lightning Bolt", 2).error shouldBe null
                game.resolveStack()

                game.getLifeTotal(2) shouldBe 17
                game.findPermanent("Incubator") shouldBe null
            }

            test("an opponent's spell targeting a permanent does not trigger") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Tiller of Flesh")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardInHand(2, "Lightning Bolt")
                    .withLandsOnBattlefield(2, "Mountain", 1)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                game.castSpell(2, "Lightning Bolt", bears).error shouldBe null
                game.resolveStack()

                game.findPermanent("Grizzly Bears") shouldBe null
                game.findPermanent("Incubator") shouldBe null
            }
        }
    }
}

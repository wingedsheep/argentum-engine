package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Feral Ghoul (PIP #44): grows on each other creature of yours dying, and on death gives each
 * opponent rad counters equal to its last-known power — counters included.
 */
class FeralGhoulScenarioTest : ScenarioTestBase() {
    init {
        test("grows when another creature you control dies, then gives opponents rad equal to its last-known power") {
            val game = scenario().withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Feral Ghoul")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInHand(1, "Lightning Bolt")
                .withCardInHand(1, "Lightning Bolt")
                .withLandsOnBattlefield(1, "Mountain", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val ghoul = game.findPermanent("Feral Ghoul")!!
            val bears = game.findPermanent("Grizzly Bears")!!

            game.castSpell(1, "Lightning Bolt", bears).error shouldBe null
            game.resolveStack()
            game.isOnBattlefield("Grizzly Bears") shouldBe false
            game.state.getEntity(ghoul)?.get<CountersComponent>()
                ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1

            game.castSpell(1, "Lightning Bolt", ghoul).error shouldBe null
            game.resolveStack()
            game.isOnBattlefield("Feral Ghoul") shouldBe false

            game.state.getEntity(game.player2Id)?.get<CountersComponent>()
                ?.getCount(CounterType.RAD) shouldBe 3
            (game.state.getEntity(game.player1Id)?.get<CountersComponent>()
                ?.getCount(CounterType.RAD) ?: 0) shouldBe 0
        }

        test("an opponent's creature dying does not grow it") {
            val game = scenario().withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Feral Ghoul")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardInHand(1, "Lightning Bolt")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val ghoul = game.findPermanent("Feral Ghoul")!!
            game.castSpell(1, "Lightning Bolt", game.findPermanent("Grizzly Bears")!!).error shouldBe null
            game.resolveStack()
            game.isOnBattlefield("Grizzly Bears") shouldBe false
            (game.state.getEntity(ghoul)?.get<CountersComponent>()
                ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0) shouldBe 0
        }
    }
}

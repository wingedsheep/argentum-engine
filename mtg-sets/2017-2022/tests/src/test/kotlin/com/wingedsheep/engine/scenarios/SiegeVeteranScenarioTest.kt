package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

/**
 * Siege Veteran (BRO #25) — a +1/+1 counter at the beginning of combat on your turn, and a 1/1
 * Soldier artifact token whenever another nontoken Soldier you control dies.
 */
class SiegeVeteranScenarioTest : ScenarioTestBase() {

    private fun game(vararg hand: String): TestGame {
        val builder = scenario()
            .withPlayers("Player1", "Player2")
            .withCardOnBattlefield(1, "Siege Veteran")
            .withCardOnBattlefield(1, "Air Marshal")
            .withCardOnBattlefield(1, "Grizzly Bears")
            .withLandsOnBattlefield(1, "Mountain", 2)
        hand.forEach { builder.withCardInHand(1, it) }
        return builder
            .withCardInLibrary(1, "Plains")
            .withCardInLibrary(2, "Plains")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()
    }

    init {
        test("beginning of combat on your turn puts a +1/+1 counter on target creature you control") {
            val game = game()
            val bears = game.findPermanent("Grizzly Bears")!!

            game.passUntilPhase(Phase.COMBAT, Step.BEGIN_COMBAT)
            game.selectTargets(listOf(bears)).error shouldBe null
            game.resolveStack()

            game.state.getEntity(bears)!!.get<CountersComponent>()
                ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
        }

        test("another nontoken Soldier you control dying creates a Soldier artifact token") {
            val game = game("Lightning Bolt")
            val marshal = game.findPermanent("Air Marshal")!!

            game.castSpell(1, "Lightning Bolt", marshal).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(1, "Air Marshal") shouldBe true
            val tokens = game.findAllPermanents("Soldier Token")
            tokens shouldHaveSize 1
            game.state.projectedState.hasType(tokens.single(), "ARTIFACT") shouldBe true
        }

        test("a Soldier token dying creates no token") {
            val game = game("Lightning Bolt", "Lightning Bolt")
            val marshal = game.findPermanent("Air Marshal")!!
            game.castSpell(1, "Lightning Bolt", marshal).error shouldBe null
            game.resolveStack()
            val token = game.findAllPermanents("Soldier Token").single()

            game.castSpell(1, "Lightning Bolt", token).error shouldBe null
            game.resolveStack()

            game.findAllPermanents("Soldier Token") shouldHaveSize 0
        }

        test("a non-Soldier dying creates no token") {
            val game = game("Lightning Bolt")
            val bears = game.findPermanent("Grizzly Bears")!!

            game.castSpell(1, "Lightning Bolt", bears).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(1, "Grizzly Bears") shouldBe true
            game.findAllPermanents("Soldier Token") shouldHaveSize 0
        }
    }
}

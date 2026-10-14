package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.scripting.references.Player
import io.kotest.matchers.shouldBe

class MirelurkQueenScenarioTest : ScenarioTestBase() {
    private val eachMillsTwo = card("Mirelurk Each Mills Two") {
        manaCost = "{0}"; typeLine = "Instant"
        spell { effect = Effects.Pipeline { mill(2, Player.Each) } }
    }

    private fun TestGame.queenCounters(): Int =
        state.getEntity(findPermanent("Mirelurk Queen")!!)?.get<CountersComponent>()
            ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    private fun board(vararg p2Library: String): TestGame {
        val b = scenario().withPlayers("Player1", "Player2")
            .withCardOnBattlefield(1, "Mirelurk Queen")
            .withCardInHand(1, eachMillsTwo.name).withCardInHand(1, eachMillsTwo.name)
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        repeat(8) { b.withCardInLibrary(1, "Forest") }
        p2Library.forEach { b.withCardInLibrary(2, it) }
        return b.build()
    }

    init {
        cardRegistry.register(eachMillsTwo)

        test("enters: target player gets two rad counters") {
            val game = scenario().withPlayers("Player1", "Player2")
                .withCardInHand(1, "Mirelurk Queen")
                .withLandsOnBattlefield(1, "Island", 5)
                .withCardInLibrary(1, "Forest").withCardInLibrary(2, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            game.castSpell(1, "Mirelurk Queen").error shouldBe null
            game.resolveStack()
            game.selectTargets(listOf(game.player2Id)).error shouldBe null
            game.resolveStack()
            game.state.getEntity(game.player2Id)?.get<CountersComponent>()?.getCount(CounterType.RAD) shouldBe 2
        }

        test("one trigger for a mill that makes every player mill, drawing then growing") {
            val game = board("Grizzly Bears", "Lightning Bolt", "Forest", "Forest")
            val hand = game.handSize(1)
            game.castSpell(1, eachMillsTwo.name).error shouldBe null
            game.resolveStack()
            game.handSize(1) shouldBe hand
            game.queenCounters() shouldBe 1
        }

        test("triggers only once each turn") {
            val game = board("Grizzly Bears", "Lightning Bolt", "Grizzly Bears", "Lightning Bolt")
            game.castSpell(1, eachMillsTwo.name).error shouldBe null
            game.resolveStack()
            game.castSpell(1, eachMillsTwo.name).error shouldBe null
            game.resolveStack()
            game.queenCounters() shouldBe 1
        }

        test("a mill of only land cards does not trigger") {
            val game = board("Forest", "Forest", "Forest", "Forest")
            game.castSpell(1, eachMillsTwo.name).error shouldBe null
            game.resolveStack()
            game.queenCounters() shouldBe 0
        }
    }
}

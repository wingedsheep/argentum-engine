package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.scripting.references.Player
import io.kotest.matchers.shouldBe

class ScreechingScorchbeastScenarioTest : ScenarioTestBase() {
    private val eachMillsTwo = card("Scorchbeast Each Mills Two") {
        manaCost = "{0}"; typeLine = "Instant"
        spell { effect = Effects.Pipeline { mill(2, Player.Each) } }
    }

    private val token = "Zombie Mutant Token"

    /** Resolves the stack, answering every "may" prompt with [answer]; returns how many were asked. */
    private fun TestGame.resolveAnswering(answer: Boolean): Int {
        var asked = 0
        var guard = 0
        resolveStack()
        while (getPendingDecision() is YesNoDecision && guard++ < 10) {
            answerYesNo(answer).error shouldBe null
            asked++
            resolveStack()
        }
        return asked
    }

    private fun board(p1Library: List<String>, p2Library: List<String>): TestGame {
        val b = scenario().withPlayers("Player1", "Player2")
            .withCardOnBattlefield(1, "Screeching Scorchbeast")
            .withCardInHand(1, eachMillsTwo.name).withCardInHand(1, eachMillsTwo.name)
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        p1Library.forEach { b.withCardInLibrary(1, it) }
        p2Library.forEach { b.withCardInLibrary(2, it) }
        return b.build()
    }

    init {
        cardRegistry.register(eachMillsTwo)

        test("attacking gives each player two rad counters") {
            val game = scenario().withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Screeching Scorchbeast")
                .withCardInLibrary(1, "Forest").withCardInLibrary(2, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Screeching Scorchbeast" to 2)).error shouldBe null
            game.resolveStack()
            listOf(game.player1Id, game.player2Id).forEach { id ->
                game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.RAD) shouldBe 2
            }
        }

        test("one trigger for a multi-player mill, creating one token per nonland card milled") {
            val game = board(
                listOf("Grizzly Bears", "Forest", "Forest", "Forest"),
                listOf("Grizzly Bears", "Lightning Bolt", "Forest", "Forest")
            )
            game.castSpell(1, eachMillsTwo.name).error shouldBe null
            game.resolveAnswering(true) shouldBe 1
            game.findPermanents(token).size shouldBe 3
        }

        test("declining keeps the ability live; accepting later stops it for the turn") {
            val game = board(
                listOf("Grizzly Bears", "Grizzly Bears", "Grizzly Bears", "Grizzly Bears", "Forest", "Forest"),
                listOf("Forest", "Forest", "Forest", "Forest", "Forest", "Forest")
            )
            game.castSpell(1, eachMillsTwo.name).error shouldBe null
            game.resolveAnswering(false) shouldBe 1
            game.findPermanents(token).size shouldBe 0

            game.castSpell(1, eachMillsTwo.name).error shouldBe null
            game.resolveAnswering(true) shouldBe 1
            game.findPermanents(token).size shouldBe 2
        }

        test("once tokens are created, a second mill that turn does not trigger") {
            val game = board(
                listOf("Grizzly Bears", "Forest", "Grizzly Bears", "Grizzly Bears", "Forest"),
                listOf("Forest", "Forest", "Forest", "Forest", "Forest")
            )
            game.castSpell(1, eachMillsTwo.name).error shouldBe null
            game.resolveAnswering(true) shouldBe 1
            game.findPermanents(token).size shouldBe 1

            game.castSpell(1, eachMillsTwo.name).error shouldBe null
            game.resolveAnswering(true) shouldBe 0
            game.findPermanents(token).size shouldBe 1
        }

        test("a mill of only land cards does not trigger") {
            val game = board(listOf("Forest", "Forest"), listOf("Forest", "Forest"))
            game.castSpell(1, eachMillsTwo.name).error shouldBe null
            game.resolveAnswering(true) shouldBe 0
            game.findPermanents(token).size shouldBe 0
        }
    }
}

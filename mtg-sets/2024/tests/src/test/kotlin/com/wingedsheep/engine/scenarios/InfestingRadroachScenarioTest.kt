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

class InfestingRadroachScenarioTest : ScenarioTestBase() {
    private val eachMillsTwo = card("Radroach Each Mills Two") {
        manaCost = "{0}"; typeLine = "Instant"
        spell { effect = Effects.Pipeline { mill(2, Player.Each) } }
    }
    private val millSelfTwo = card("Radroach Mill Self Two") {
        manaCost = "{0}"; typeLine = "Instant"
        spell { effect = Effects.Pipeline { mill(2) } }
    }

    /** Resolves the stack, answering every "may return it" prompt with [answer]; returns how many were asked. */
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

    private fun graveyardBoard(p1Library: List<String>, p2Library: List<String>, radroachInGraveyard: Boolean = true): TestGame {
        val b = scenario().withPlayers("Player1", "Player2")
            .withCardInHand(1, eachMillsTwo.name)
            .withCardInHand(1, millSelfTwo.name)
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        if (radroachInGraveyard) b.withCardInGraveyard(1, "Infesting Radroach")
        p1Library.forEach { b.withCardInLibrary(1, it) }
        p2Library.forEach { b.withCardInLibrary(2, it) }
        return b.build()
    }

    init {
        cardRegistry.register(eachMillsTwo)
        cardRegistry.register(millSelfTwo)

        test("combat damage to a player gives them that many rad counters") {
            val game = scenario().withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Infesting Radroach")
                .withCardInLibrary(1, "Forest").withCardInLibrary(2, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Infesting Radroach" to 2)).error shouldBe null
            game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
            game.resolveStack()
            game.getLifeTotal(2) shouldBe 18
            game.state.getEntity(game.player2Id)?.get<CountersComponent>()?.getCount(CounterType.RAD) shouldBe 2
        }

        test("an opponent milling a nonland card lets it return from the graveyard to hand") {
            val game = graveyardBoard(listOf("Forest", "Forest"), listOf("Forest", "Grizzly Bears"))
            game.castSpell(1, eachMillsTwo.name).error shouldBe null
            game.resolveAnswering(true) shouldBe 1
            game.isInHand(1, "Infesting Radroach") shouldBe true
            game.isInGraveyard(1, "Infesting Radroach") shouldBe false
        }

        test("declining the may leaves it in the graveyard") {
            val game = graveyardBoard(listOf("Forest", "Forest"), listOf("Forest", "Grizzly Bears"))
            game.castSpell(1, eachMillsTwo.name).error shouldBe null
            game.resolveAnswering(false) shouldBe 1
            game.isInGraveyard(1, "Infesting Radroach") shouldBe true
        }

        test("your own mill and an opponent's land-only mill do not trigger it") {
            val game = graveyardBoard(listOf("Grizzly Bears", "Grizzly Bears"), listOf("Forest", "Forest"))
            game.castSpell(1, millSelfTwo.name).error shouldBe null
            game.resolveAnswering(true) shouldBe 0
            game.castSpell(1, eachMillsTwo.name).error shouldBe null
            game.resolveAnswering(true) shouldBe 0
            game.isInGraveyard(1, "Infesting Radroach") shouldBe true
        }

        test("two nonland cards milled: the second trigger's intervening if fails once it has returned") {
            val game = graveyardBoard(listOf("Forest", "Forest"), listOf("Grizzly Bears", "Grizzly Bears"))
            game.castSpell(1, eachMillsTwo.name).error shouldBe null
            game.resolveAnswering(true) shouldBe 1
            game.isInHand(1, "Infesting Radroach") shouldBe true
        }

        test("milled at the same time as an opponent's nonland card, it still triggers (ruling)") {
            val game = graveyardBoard(
                listOf("Infesting Radroach", "Forest"),
                listOf("Grizzly Bears", "Forest"),
                radroachInGraveyard = false
            )
            game.castSpell(1, eachMillsTwo.name).error shouldBe null
            game.resolveAnswering(true) shouldBe 1
            game.isInHand(1, "Infesting Radroach") shouldBe true
        }
    }
}

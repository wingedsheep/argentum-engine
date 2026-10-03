package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Rosecot Knight: ETB looks at the top six, may put an artifact or enchantment into hand (rest on
 * the bottom in a random order); if no card went to hand, it gets a +1/+1 counter.
 */
class RosecotKnightScenarioTest : ScenarioTestBase() {

    private fun setup() = scenario()
        .withPlayers()
        .withCardInHand(1, "Rosecot Knight")
        .withLandsOnBattlefield(1, "Plains", 5)
        .withCardInLibrary(1, "Ornithopter")
        .withCardInLibrary(1, "Forest")
        .withCardInLibrary(1, "Grizzly Bears")
        .withCardInLibrary(1, "Forest")
        .withCardInLibrary(1, "Forest")
        .withCardInLibrary(1, "Forest")
        .withCardInLibrary(1, "Island")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun plusOneCounters(game: TestGame): Int {
        val knight = game.findPermanent("Rosecot Knight")!!
        return game.state.getEntity(knight)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0
    }

    init {
        test("taking an artifact into hand means no +1/+1 counter") {
            val game = setup()
            game.castSpell(1, "Rosecot Knight").error shouldBe null
            game.resolveStack()

            val decision = game.getPendingDecision() as SelectCardsDecision
            // Only the artifact is selectable.
            decision.options.size shouldBe 1
            val thopter = decision.options.single()
            game.state.getEntity(thopter)?.get<CardComponent>()?.name shouldBe "Ornithopter"
            game.selectCards(listOf(thopter)).error shouldBe null
            game.resolveStack()

            game.findCardsInHand(1, "Ornithopter").size shouldBe 1
            plusOneCounters(game) shouldBe 0
            // The rest went to the bottom; the seventh card (Island) is now on top.
            game.state.getLibrary(game.player1Id).first().let {
                game.state.getEntity(it)?.get<CardComponent>()?.name
            } shouldBe "Island"
            game.state.getLibrary(game.player1Id).size shouldBe 6
        }

        test("declining to take a card puts a +1/+1 counter on the knight") {
            val game = setup()
            game.castSpell(1, "Rosecot Knight").error shouldBe null
            game.resolveStack()

            game.getPendingDecision() as SelectCardsDecision
            game.selectCards(emptyList()).error shouldBe null
            game.resolveStack()

            game.findCardsInHand(1, "Ornithopter").size shouldBe 0
            game.state.getLibrary(game.player1Id).size shouldBe 7
            plusOneCounters(game) shouldBe 1
        }
    }
}

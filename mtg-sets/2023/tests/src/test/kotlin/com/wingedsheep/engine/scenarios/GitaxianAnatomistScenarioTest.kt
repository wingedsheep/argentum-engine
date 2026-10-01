package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Gitaxian Anatomist (ONE #52) — {3}{U} 2/5 Creature — Phyrexian Wizard.
 *
 * "When this creature enters, you may tap it. If you do, proliferate."
 */
class GitaxianAnatomistScenarioTest : ScenarioTestBase() {

    private fun seed(game: TestGame, id: EntityId, amount: Int) {
        game.state = game.state.updateEntity(id) { c ->
            c.with((c.get<CountersComponent>() ?: CountersComponent()).withAdded(CounterType.PLUS_ONE_PLUS_ONE, amount))
        }
    }

    private fun counters(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    private fun isTapped(game: TestGame, id: EntityId): Boolean =
        game.state.getEntity(id)?.has<TappedComponent>() == true

    private fun setup(): TestGame {
        val game = scenario()
            .withPlayers("Player1", "Player2")
            .withCardInHand(1, "Gitaxian Anatomist")
            .withCardOnBattlefield(1, "Grizzly Bears")
            .withLandsOnBattlefield(1, "Island", 4)
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()
        seed(game, game.findPermanent("Grizzly Bears")!!, 1)
        return game
    }

    init {
        test("tapping it on entry proliferates") {
            val game = setup()
            val bears = game.findPermanent("Grizzly Bears")!!

            game.castSpell(1, "Gitaxian Anatomist").error shouldBe null
            game.resolveStack()
            game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
            game.answerYesNo(true)
            game.selectCards(listOf(bears))
            game.resolveStack()

            val anatomist = game.findPermanent("Gitaxian Anatomist")!!
            isTapped(game, anatomist) shouldBe true
            counters(game, bears) shouldBe 2
        }

        test("declining leaves it untapped and does not proliferate") {
            val game = setup()
            val bears = game.findPermanent("Grizzly Bears")!!

            game.castSpell(1, "Gitaxian Anatomist").error shouldBe null
            game.resolveStack()
            game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
            game.answerYesNo(false)
            game.resolveStack()

            val anatomist = game.findPermanent("Gitaxian Anatomist")!!
            isTapped(game, anatomist) shouldBe false
            counters(game, bears) shouldBe 1
            game.hasPendingDecision() shouldBe false
        }

        test("if it is already tapped when the trigger resolves, it can't be tapped so nothing is proliferated") {
            val game = setup()
            val bears = game.findPermanent("Grizzly Bears")!!

            game.castSpell(1, "Gitaxian Anatomist").error shouldBe null
            // Resolve the creature spell; its enters trigger goes on the stack.
            var guard = 0
            while (game.findPermanent("Gitaxian Anatomist") == null && guard++ < 4) game.passPriority()
            val anatomist = game.findPermanent("Gitaxian Anatomist")!!
            game.state.stack.isNotEmpty() shouldBe true

            // Tapped in response (e.g. by an opponent's effect).
            game.state = game.state.updateEntity(anatomist) { it.with(TappedComponent) }
            game.resolveStack()

            game.hasPendingDecision() shouldBe false
            counters(game, bears) shouldBe 1
        }
    }
}

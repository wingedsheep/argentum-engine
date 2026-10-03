package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Dreamtide Whale (MH3 #59) — {2}{U} 7/5 Whale, vanishing 2.
 *
 *   Whenever a player casts their second spell each turn, proliferate.
 */
class DreamtideWhaleScenarioTest : ScenarioTestBase() {

    private fun timeCounters(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.TIME) ?: 0

    private fun addTime(game: TestGame, id: EntityId, n: Int) {
        game.state = game.state.updateEntity(id) { c ->
            c.with((c.get<CountersComponent>() ?: CountersComponent()).withAdded(CounterType.TIME, n))
        }
    }

    private fun board() = scenario()
        .withPlayers("Player1", "Player2")
        .withCardOnBattlefield(1, "Dreamtide Whale")
        .withCardsInHand(1, "Lightning Bolt", 2)
        .withCardsInHand(2, "Lightning Bolt", 2)
        .withLandsOnBattlefield(1, "Mountain", 2)
        .withLandsOnBattlefield(2, "Mountain", 2)
        .withCardInLibrary(1, "Island")
        .withCardInLibrary(2, "Island")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("your second spell each turn proliferates; the first does not") {
            val game = board()
            val whale = game.findPermanent("Dreamtide Whale")!!
            addTime(game, whale, 1)

            game.castSpellTargetingPlayer(1, "Lightning Bolt", 2).error shouldBe null
            game.resolveStack()
            game.state.pendingDecision shouldBe null
            timeCounters(game, whale) shouldBe 1

            game.castSpellTargetingPlayer(1, "Lightning Bolt", 2).error shouldBe null
            game.resolveStack()
            game.state.pendingDecision shouldNotBe null
            game.selectCards(listOf(whale)).error shouldBe null
            game.resolveStack()

            timeCounters(game, whale) shouldBe 2
            game.getLifeTotal(2) shouldBe 14
        }

        test("an opponent's second spell also proliferates, counted per caster") {
            val game = board()
            val whale = game.findPermanent("Dreamtide Whale")!!
            addTime(game, whale, 1)

            // Player 1 casts one spell; Player 2 responds with two — only Player 2's second fires it.
            game.castSpellTargetingPlayer(1, "Lightning Bolt", 2).error shouldBe null
            game.passPriority().error shouldBe null
            game.castSpellTargetingPlayer(2, "Lightning Bolt", 1).error shouldBe null
            // The turn's second spell, but Player 2's first: no proliferate trigger joins the stack.
            game.state.stack.size shouldBe 2
            // Player 2 holds priority after casting and casts their second spell.
            game.castSpellTargetingPlayer(2, "Lightning Bolt", 1).error shouldBe null
            game.resolveStack()
            game.state.pendingDecision shouldNotBe null
            game.selectCards(listOf(whale)).error shouldBe null
            game.resolveStack()

            timeCounters(game, whale) shouldBe 2
        }
    }
}

package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.OrderObjectsDecision
import com.wingedsheep.engine.core.OrderedResponse
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Ember-Fist Zubera (CHK #166) — "When this creature dies, it deals damage to any target equal to
 * the number of Zubera that died this turn."
 */
class EmberFistZuberaScenarioTest : ScenarioTestBase() {

    /** Resolve everything, answering each prompt: [pick] chooses trigger targets, trigger order is kept, discards take the first cards. */
    private fun TestGame.settle(pick: (ChooseTargetsDecision) -> List<EntityId> = { listOf(it.legalTargets.getValue(0).first()) }) {
        repeat(20) {
            resolveStack()
            when (val d = state.pendingDecision) {
                null -> return
                is ChooseTargetsDecision -> selectTargets(pick(d)).error shouldBe null
                is OrderObjectsDecision -> submitDecision(OrderedResponse(d.id, d.objects)).error shouldBe null
                is SelectCardsDecision -> selectCards(d.options.take(d.minSelections)).error shouldBe null
                else -> error("unexpected decision $d")
            }
        }
    }

    init {
        fun base() = scenario().withPlayers("Player1", "Player2")
            .withActivePlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

        context("Ember-Fist Zubera") {
            test("dying alone deals 1 damage to the chosen target") {
                val game = base().withCardOnBattlefield(1, "Ember-Fist Zubera")
                    .withCardInHand(1, "Shock").withLandsOnBattlefield(1, "Mountain", 1).build()

                game.castSpell(1, "Shock", game.findPermanent("Ember-Fist Zubera")!!).error shouldBe null
                game.settle { listOf(game.player2Id) }

                game.getLifeTotal(2) shouldBe 19
            }

            test("counts an opponent's Zubera that died earlier this turn") {
                val game = base().withCardOnBattlefield(1, "Ember-Fist Zubera")
                    .withCardOnBattlefield(2, "Silent-Chant Zubera")
                    .withCardsInHand(1, "Shock", 2).withLandsOnBattlefield(1, "Mountain", 2).build()

                game.castSpell(1, "Shock", game.findPermanent("Silent-Chant Zubera")!!).error shouldBe null
                game.settle()
                // The engine hands priority to the resolved trigger's controller (Player2) rather
                // than the active player (CR 117.3b); pass it back before casting again.
                if (game.state.priorityPlayerId == game.player2Id) game.passPriority()
                val lifeBefore = game.getLifeTotal(2)
                game.castSpell(1, "Shock", game.findPermanent("Ember-Fist Zubera")!!).error shouldBe null
                game.settle { listOf(game.player2Id) }

                withClue("two Zubera died this turn") { game.getLifeTotal(2) shouldBe lifeBefore - 2 }
            }
        }
    }
}

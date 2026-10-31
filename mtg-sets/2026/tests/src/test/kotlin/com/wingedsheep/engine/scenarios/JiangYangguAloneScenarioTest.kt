package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Jiang Yanggu, Alone (FRA #246) — whenever a creature you control attacks a player alone, loot,
 * then that creature gets a +1/+1 counter for each card you've discarded this turn.
 */
class JiangYangguAloneScenarioTest : ScenarioTestBase() {

    private fun TestGame.plusCounters(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    private fun TestGame.discardFirstOffered() {
        val decision = getPendingDecision()
        if (decision is SelectCardsDecision) {
            selectCards(decision.options.take(1)).error shouldBe null
        }
    }

    init {
        fun combatGame(vararg hand: String): TestGame {
            var b = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Jiang Yanggu, Alone")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Liliana Vess")
                .withActivePlayer(1)
                .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            for (card in hand) b = b.withCardInHand(1, card)
            repeat(4) { b = b.withCardInLibrary(1, "Island") }
            repeat(4) { b = b.withCardInLibrary(2, "Island") }
            return b.build()
        }

        context("Jiang Yanggu, Alone") {
            test("a lone attacker loots and gets a counter per card discarded this turn") {
                val game = combatGame("Hill Giant", "Hill Giant")
                game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
                game.resolveStack()
                game.discardFirstOffered()
                game.resolveStack()

                val bears = game.findPermanent("Grizzly Bears")!!
                game.graveyardSize(1) shouldBe 1
                game.handSize(1) shouldBe 2
                withClue("one card discarded this turn → one +1/+1 counter") {
                    game.plusCounters(bears) shouldBe 1
                }
            }

            test("Jiang itself attacking alone triggers too") {
                val game = combatGame("Hill Giant")
                game.declareAttackers(mapOf("Jiang Yanggu, Alone" to 2)).error shouldBe null
                game.resolveStack()
                game.discardFirstOffered()
                game.resolveStack()

                game.plusCounters(game.findPermanent("Jiang Yanggu, Alone")!!) shouldBe 1
            }

            test("with an empty hand nothing is discarded, so no counters") {
                val game = combatGame()
                game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
                game.resolveStack()

                game.handSize(1) shouldBe 1
                game.plusCounters(game.findPermanent("Grizzly Bears")!!) shouldBe 0
            }

            // CR 506.6: alone against *that player* — Jiang attacking a planeswalker doesn't count.
            test("the player's only attacker triggers while another creature attacks a planeswalker") {
                val game = combatGame("Hill Giant")
                game.declareAttackersWithPermanentTargets(
                    playerAttackers = mapOf("Grizzly Bears" to 2),
                    permanentAttackers = mapOf("Jiang Yanggu, Alone" to "Liliana Vess")
                ).error shouldBe null
                game.resolveStack()
                game.discardFirstOffered()
                game.resolveStack()

                game.graveyardSize(1) shouldBe 1
                game.plusCounters(game.findPermanent("Grizzly Bears")!!) shouldBe 1
                game.plusCounters(game.findPermanent("Jiang Yanggu, Alone")!!) shouldBe 0
            }

            test("a creature attacking a planeswalker alone doesn't trigger") {
                val game = combatGame("Hill Giant")
                game.declareAttackersWithPermanentTargets(
                    permanentAttackers = mapOf("Grizzly Bears" to "Liliana Vess")
                ).error shouldBe null
                game.resolveStack()

                game.hasPendingDecision() shouldBe false
                game.handSize(1) shouldBe 1
            }

            test("attacking with two creatures doesn't trigger") {
                val game = combatGame("Hill Giant")
                game.declareAttackers(mapOf("Grizzly Bears" to 2, "Jiang Yanggu, Alone" to 2)).error shouldBe null
                game.resolveStack()

                game.hasPendingDecision() shouldBe false
                game.handSize(1) shouldBe 1
                game.plusCounters(game.findPermanent("Grizzly Bears")!!) shouldBe 0
            }
        }
    }
}

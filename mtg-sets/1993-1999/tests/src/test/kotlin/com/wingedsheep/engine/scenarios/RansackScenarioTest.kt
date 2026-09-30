package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.core.OrderedResponse
import com.wingedsheep.engine.core.ReorderLibraryDecision
import com.wingedsheep.engine.core.ScriedEvent
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

class RansackScenarioTest : ScenarioTestBase() {
    init {
        test("caster privately chooses and orders both piles of an opponent's library without scrying") {
            val game = scenario()
                .withPlayers()
                .withCardInHand(1, "Ransack")
                .withLandsOnBattlefield(1, "Island", 4)
                .withCardInLibrary(1, "Island")
                .apply { repeat(7) { withCardInLibrary(2, "Forest") } }
                .withActivePlayer(1)
                .withPriorityPlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val before = game.state.getLibrary(game.player2Id)
            val ownLibrary = game.state.getLibrary(game.player1Id)
            val events = mutableListOf<GameEvent>()
            game.castSpellTargetingPlayer(1, "Ransack", 2).error shouldBe null
            game.resolveStack().forEach { events += it.events }
            val select = game.getPendingDecision() as SelectCardsDecision
            select.playerId shouldBe game.player1Id
            select.options shouldBe before.take(5)
            select.minSelections shouldBe 0
            select.maxSelections shouldBe 5
            for (id in before.take(5)) {
                game.getClientState(1).cards.containsKey(id) shouldBe true
                game.getClientState(2).cards.containsKey(id) shouldBe false
            }
            val bottom = before.take(2).reversed()
            events += game.selectCards(bottom).events
            val bottomOrder = game.getPendingDecision() as ReorderLibraryDecision
            bottomOrder.playerId shouldBe game.player1Id
            bottomOrder.cards.toSet() shouldBe bottom.toSet()
            val bottomResult = game.submitDecision(OrderedResponse(bottomOrder.id, bottom))
            bottomResult.error shouldBe null
            events += bottomResult.events
            val top = before.take(5).drop(2).reversed()
            val topOrder = game.getPendingDecision() as ReorderLibraryDecision
            topOrder.playerId shouldBe game.player1Id
            topOrder.cards.toSet() shouldBe top.toSet()
            val topResult = game.submitDecision(OrderedResponse(topOrder.id, top))
            topResult.error shouldBe null
            events += topResult.events
            game.resolveStack().forEach { events += it.events }
            game.getPendingDecision() shouldBe null
            game.state.getLibrary(game.player2Id) shouldBe top + before.drop(5) + bottom
            game.state.getLibrary(game.player1Id) shouldBe ownLibrary
            events.filterIsInstance<ScriedEvent>() shouldBe emptyList()
        }

        for (bottomAll in listOf(false, true)) {
            test("self-target with fewer than five cards permits ${if (bottomAll) "all" else "none"} on bottom") {
                val game = scenario()
                    .withPlayers()
                    .withCardInHand(1, "Ransack")
                    .withLandsOnBattlefield(1, "Island", 4)
                    .apply { repeat(3) { withCardInLibrary(1, "Forest") } }
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .withPriorityPlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val before = game.state.getLibrary(game.player1Id)
                game.castSpellTargetingPlayer(1, "Ransack", 1).error shouldBe null
                game.resolveStack()
                val select = game.getPendingDecision() as SelectCardsDecision
                select.options shouldBe before
                select.minSelections shouldBe 0
                select.maxSelections shouldBe 3
                game.selectCards(if (bottomAll) before else emptyList()).error shouldBe null
                val order = game.getPendingDecision() as ReorderLibraryDecision
                order.playerId shouldBe game.player1Id
                game.submitDecision(OrderedResponse(order.id, before.reversed())).error shouldBe null
                game.resolveStack()
                game.getPendingDecision() shouldBe null
                game.state.getLibrary(game.player1Id) shouldBe before.reversed()
            }
        }

        test("empty target library resolves without a decision") {
            val game = scenario()
                .withPlayers()
                .withCardInHand(1, "Ransack")
                .withLandsOnBattlefield(1, "Island", 4)
                .withCardInLibrary(1, "Island")
                .withActivePlayer(1)
                .withPriorityPlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            game.castSpellTargetingPlayer(1, "Ransack", 2).error shouldBe null
            game.resolveStack()
            game.getPendingDecision() shouldBe null
            game.state.getLibrary(game.player2Id) shouldBe emptyList()
        }
    }
}

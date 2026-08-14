package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/** Scenario tests for Mind Roots. */
class MindRootsScenarioTest : ScenarioTestBase() {

    private fun growthCounters(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.GROWTH) ?: 0

    private fun plusOneCounters(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    init {
        context("Mind Roots — target player discards two, put a discarded land tapped under you") {
            test("the target player discards two; you put one discarded land onto the battlefield tapped") {
                var builder = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Mind Roots")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                // Opponent's hand: a land (Mountain) plus two non-lands to choose among.
                builder = builder.withCardInHand(2, "Mountain")
                builder = builder.withCardInHand(2, "Grizzly Bears")
                builder = builder.withCardInHand(2, "Grizzly Bears")
                repeat(5) { builder = builder.withCardInLibrary(1, "Forest") }
                repeat(5) { builder = builder.withCardInLibrary(2, "Forest") }
                val game = builder.build()

                val landsBefore = game.findPermanents("Mountain").size

                game.castSpellTargetingPlayer(1, "Mind Roots", 2)
                game.resolveStack()

                // First decision: the opponent discards two cards — discard the Mountain + a Bears.
                val mountain = game.findCardsInHand(2, "Mountain").first()
                val aBears = game.findCardsInHand(2, "Grizzly Bears").first()
                game.selectCards(listOf(mountain, aBears))
                game.resolveStack()

                withClue("Opponent discarded two — hand drops to one Grizzly Bears") {
                    game.handSize(2) shouldBe 1
                }

                // Second decision: you may put up to one discarded land tapped under your control.
                withClue("A put-the-land decision should be pending") {
                    game.state.pendingDecision shouldNotBe null
                }
                game.selectCards(listOf(mountain))
                game.resolveStack()

                val mountainsAfter = game.findPermanents("Mountain")
                withClue("The discarded Mountain enters the battlefield (one new Mountain permanent)") {
                    mountainsAfter.size shouldBe landsBefore + 1
                }
                val newMountain = mountainsAfter.first()
                withClue("It is controlled by you (player 1)") {
                    game.state.getEntity(newMountain)?.get<ControllerComponent>()?.playerId shouldBe game.player1Id
                }
                withClue("It enters tapped") {
                    (game.state.getEntity(newMountain)?.get<TappedComponent>() != null) shouldBe true
                }
            }

            test("if no land is discarded there is no land to put — the up-to-one is a no-op") {
                var builder = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Mind Roots")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                // Three non-lands in hand so the opponent actually chooses which two to discard.
                builder = builder.withCardInHand(2, "Grizzly Bears")
                builder = builder.withCardInHand(2, "Grizzly Bears")
                builder = builder.withCardInHand(2, "Grizzly Bears")
                repeat(5) { builder = builder.withCardInLibrary(1, "Forest") }
                repeat(5) { builder = builder.withCardInLibrary(2, "Forest") }
                val game = builder.build()

                game.castSpellTargetingPlayer(1, "Mind Roots", 2)
                game.resolveStack()

                // Opponent discards two non-lands; no land among them.
                val bears = game.findCardsInHand(2, "Grizzly Bears").take(2)
                game.selectCards(bears)
                game.resolveStack()

                withClue("Opponent discarded two — one Grizzly Bears remains in hand") {
                    game.handSize(2) shouldBe 1
                }
                withClue("No land discarded → no permanent should appear under your control") {
                    game.findPermanents("Grizzly Bears").isEmpty() shouldBe true
                }
                withClue("No pending land-put decision when nothing land was discarded") {
                    game.state.pendingDecision shouldBe null
                }
            }
        }
    }
}

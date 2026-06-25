package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Orphans of the Wheat (DSK #22) — {1}{W} 2/1 Creature — Human.
 *
 * "Whenever this creature attacks, tap any number of untapped creatures you control. This creature
 *  gets +1/+1 until end of turn for each creature tapped this way."
 *
 * The attack trigger gathers your untapped creatures, lets you tap any number of them
 * (`SelectCardsDecision`), and pumps this creature +1/+1 per creature tapped this way — a fixed
 * snapshot of the selection count applied until end of turn.
 */
class OrphansOfTheWheatScenarioTest : ScenarioTestBase() {

    init {
        fun tapped(game: TestGame, id: com.wingedsheep.sdk.model.EntityId): Boolean =
            game.state.getEntity(id)?.get<TappedComponent>() != null

        context("Orphans of the Wheat — tap any number on attack") {

            test("tapping two creatures pumps it +2/+2 and taps those creatures") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Orphans of the Wheat", tapped = false, summoningSickness = false)
                    .withCardOnBattlefield(1, "Grizzly Bears", tapped = false, summoningSickness = false)
                    .withCardOnBattlefield(1, "Hill Giant", tapped = false, summoningSickness = false)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val orphans = game.findPermanent("Orphans of the Wheat")!!
                val bear = game.findPermanent("Grizzly Bears")!!
                val giant = game.findPermanent("Hill Giant")!!

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Orphans of the Wheat" to 2)).error shouldBe null

                var guard = 0
                while (game.getPendingDecision() !is SelectCardsDecision && guard++ < 20) {
                    game.resolveStack()
                }
                val decision = game.getPendingDecision() as? SelectCardsDecision
                    ?: error("expected a SelectCardsDecision to tap creatures; got ${game.getPendingDecision()}")
                game.submitDecision(CardsSelectedResponse(decision.id, listOf(bear, giant)))
                game.resolveStack()

                withClue("Both chosen creatures are now tapped") {
                    tapped(game, bear) shouldBe true
                    tapped(game, giant) shouldBe true
                }
                withClue("Orphans got +2/+2 (2/1 base -> 4/3)") {
                    game.state.projectedState.getPower(orphans) shouldBe 4
                    game.state.projectedState.getToughness(orphans) shouldBe 3
                }
            }

            test("tapping zero creatures leaves it at its printed stats") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Orphans of the Wheat", tapped = false, summoningSickness = false)
                    .withCardOnBattlefield(1, "Grizzly Bears", tapped = false, summoningSickness = false)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val orphans = game.findPermanent("Orphans of the Wheat")!!
                val bear = game.findPermanent("Grizzly Bears")!!

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Orphans of the Wheat" to 2)).error shouldBe null

                var guard = 0
                while (game.getPendingDecision() !is SelectCardsDecision && guard++ < 20) {
                    game.resolveStack()
                }
                val decision = game.getPendingDecision() as? SelectCardsDecision
                    ?: error("expected a SelectCardsDecision to tap creatures; got ${game.getPendingDecision()}")
                // Choose to tap nothing.
                game.submitDecision(CardsSelectedResponse(decision.id, emptyList()))
                game.resolveStack()

                withClue("The Bear stays untapped — nothing was tapped this way") {
                    tapped(game, bear) shouldBe false
                }
                withClue("Orphans stays 2/1 — +0/+0") {
                    game.state.projectedState.getPower(orphans) shouldBe 2
                    game.state.projectedState.getToughness(orphans) shouldBe 1
                }
            }
        }
    }
}

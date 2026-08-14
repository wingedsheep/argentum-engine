package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Agonasaur Rex — Cycling {2}{G} plus "When you cycle this card, put two +1/+1 counters on up to one
 * target creature or Vehicle. It gains trample and indestructible until end of turn."
 *
 * Cycling and the cycle trigger are separate abilities (CR 702.29 rulings), so the third case pins
 * the consequence: with nothing to target you may still cycle, and the draw happens anyway. The
 * first two cases prove the payload reaches a creature and a Vehicle alike, and that the keyword
 * grants are end-of-turn while the counters stay.
 */
class AgonasaurRexScenarioTest : ScenarioTestBase() {

    init {
        test("cycling puts two counters on a target creature and grants trample and indestructible") {
            val game = rexGame(extraPermanent = "Grizzly Bears")
            val bears = game.findPermanent("Grizzly Bears")!!

            val cycle = game.cycleCard(1, "Agonasaur Rex")
            withClue("Cycling should succeed: ${cycle.error}") { cycle.error shouldBe null }
            if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
            if (game.hasPendingDecision()) game.selectTargets(listOf(bears))
            game.resolveStack()

            val projected = game.state.projectedState
            withClue("Two +1/+1 counters — a 2/2 becomes a 4/4") {
                game.counters(bears) shouldBe 2
                projected.getPower(bears) shouldBe 4
                projected.getToughness(bears) shouldBe 4
            }
            withClue("Both keywords are granted") {
                projected.hasKeyword(bears, Keyword.TRAMPLE) shouldBe true
                projected.hasKeyword(bears, Keyword.INDESTRUCTIBLE) shouldBe true
            }
            withClue("The Rex itself is in the graveyard — cycling discarded it") {
                game.isInGraveyard(1, "Agonasaur Rex") shouldBe true
            }

            // Into the opponent's upkeep, i.e. past this turn's cleanup step.
            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            withClue("The grants were until end of turn; the counters are permanent") {
                game.state.projectedState.hasKeyword(bears, Keyword.TRAMPLE) shouldBe false
                game.state.projectedState.hasKeyword(bears, Keyword.INDESTRUCTIBLE) shouldBe false
                game.counters(bears) shouldBe 2
            }
        }

        test("a Vehicle is a legal target even while it isn't a creature") {
            val game = rexGame(extraPermanent = "Air Response Unit")
            val vehicle = game.findPermanent("Air Response Unit")!!

            val cycle = game.cycleCard(1, "Agonasaur Rex")
            withClue("Cycling should succeed: ${cycle.error}") { cycle.error shouldBe null }
            if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
            if (game.hasPendingDecision()) game.selectTargets(listOf(vehicle))
            game.resolveStack()

            withClue("\"creature or Vehicle\" matches an uncrewed Vehicle by subtype") {
                game.counters(vehicle) shouldBe 2
                game.state.projectedState.hasKeyword(vehicle, Keyword.INDESTRUCTIBLE) shouldBe true
            }
        }

        test("cycling with nothing to target still draws — the two abilities are independent") {
            val game = rexGame(extraPermanent = null)
            val handBefore = game.handSize(1)

            val cycle = game.cycleCard(1, "Agonasaur Rex")
            withClue("Cycling should succeed with no creature or Vehicle in play: ${cycle.error}") {
                cycle.error shouldBe null
            }
            if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
            if (game.hasPendingDecision()) game.skipTargets()
            game.resolveStack()

            withClue("Cycling drew a card, replacing the discarded Rex") {
                game.handSize(1) shouldBe handBefore
                game.isInGraveyard(1, "Agonasaur Rex") shouldBe true
            }
        }
    }

    /** The Rex in hand, three Forests to cycle it, and optionally one permanent to target. */
    private fun rexGame(extraPermanent: String?): TestGame {
        val builder = scenario()
            .withPlayers("Player1", "Player2")
            .withCardInHand(1, "Agonasaur Rex")
            .withLandsOnBattlefield(1, "Forest", 3)
        if (extraPermanent != null) {
            builder.withCardOnBattlefield(1, extraPermanent, summoningSickness = false)
        }
        repeat(8) {
            builder.withCardInLibrary(1, "Grizzly Bears")
            builder.withCardInLibrary(2, "Grizzly Bears")
        }
        return builder
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()
    }

    private fun TestGame.counters(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0
}

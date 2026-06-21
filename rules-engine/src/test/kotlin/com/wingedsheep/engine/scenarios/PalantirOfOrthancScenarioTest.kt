package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.OrderedResponse
import com.wingedsheep.engine.core.ReorderLibraryDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.core.YesNoResponse
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Palantír of Orthanc — Gap 6.
 *
 * At the beginning of your end step: put an influence counter on Palantír and scry 2. Then the
 * *targeted opponent* (not the controller) may have you draw a card. If they decline, you mill X
 * cards (X = influence counters on Palantír) and that opponent loses life equal to the total mana
 * value of those milled cards.
 *
 * Exercises the two reusable primitives this card introduced:
 *  - the opponent-decides "may" (MayEffect.decisionMaker = the targeted opponent),
 *  - DynamicAmount.ManaValueSumOfCollection ("total mana value of those cards").
 */
class PalantirOfOrthancScenarioTest : ScenarioTestBase() {

    private fun influenceCount(game: TestGame): Int {
        val palantir = game.findPermanent("Palantír of Orthanc")!!
        return game.state.getEntity(palantir)
            ?.get<CountersComponent>()
            ?.getCount(CounterType.INFLUENCE) ?: 0
    }

    /**
     * Drains the scry-2 decisions (keep everything on top, original order) until we land on the
     * targeted opponent's may-decision. Scry surfaces a "put on bottom" select and/or a reorder of
     * the cards kept on top, depending on the choices; resolve whichever appears.
     */
    private fun resolveScryThenReturnMayDecision(game: TestGame): YesNoDecision {
        var guard = 0
        while (guard++ < 10) {
            when (val decision = game.getPendingDecision()) {
                is YesNoDecision -> return decision
                is SelectCardsDecision -> game.skipSelection() // nothing to the bottom
                is ReorderLibraryDecision ->
                    game.submitDecision(OrderedResponse(decision.id, decision.cards)) // keep order
                else -> error("unexpected scry decision: $decision")
            }
            game.resolveStack()
        }
        error("never reached the may-decision; last = ${game.getPendingDecision()}")
    }

    init {
        test("opponent says yes: you draw a card, no mill, no life loss") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Palantír of Orthanc")
                .withCardInLibrary(1, "Grizzly Bears") // top of library (scry fodder + draw)
                .withCardInLibrary(1, "Hill Giant")
                .withCardInLibrary(1, "Island")
                .withLifeTotal(2, 20)
                .inPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
                .withActivePlayer(1)
                .build()

            val handBefore = game.handSize(1)
            val libBefore = game.librarySize(1)

            game.passUntilPhase(Phase.ENDING, Step.END)
            game.resolveStack()

            // Influence counter added.
            influenceCount(game) shouldBe 1

            // Scry 2 surfaces first; keep all on top, then reach the may-decision.
            val mayDecision = resolveScryThenReturnMayDecision(game)

            // The targeted opponent (player 2) makes the decision.
            mayDecision.playerId shouldBe game.player2Id
            game.submitDecision(YesNoResponse(mayDecision.id, true))
            game.resolveStack()

            // Yes -> controller drew exactly one card; nothing milled; opponent unharmed.
            game.handSize(1) shouldBe handBefore + 1
            game.librarySize(1) shouldBe libBefore - 1
            game.graveyardSize(1) shouldBe 0
            game.getLifeTotal(2) shouldBe 20
        }

        test("opponent says no: you mill X and that opponent loses life = total mana value milled") {
            // Influence counters start at 2 (we add one at end step -> X = 3 milled cards).
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Palantír of Orthanc")
                // Library top order (drawn/milled from top): three known cards with known MV.
                // Grizzly Bears MV 2, Hill Giant MV 4, Lightning Bolt MV 1 => total MV 7.
                .withCardInLibrary(1, "Grizzly Bears")
                .withCardInLibrary(1, "Hill Giant")
                .withCardInLibrary(1, "Lightning Bolt")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(1, "Forest")
                .withLifeTotal(2, 20)
                .inPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
                .withActivePlayer(1)
                .build()

            // Pre-load Palantír with 2 influence counters so X = 3 after the end-step increment.
            val palantir = game.findPermanent("Palantír of Orthanc")!!
            run {
                val counters = (game.state.getEntity(palantir)?.get<CountersComponent>()
                    ?: CountersComponent()).withCounters(CounterType.INFLUENCE, 2)
                game.state = game.state.updateEntity(palantir) { it.with(counters) }
            }

            val handBefore = game.handSize(1)

            game.passUntilPhase(Phase.ENDING, Step.END)
            game.resolveStack()

            influenceCount(game) shouldBe 3

            val mayDecision = resolveScryThenReturnMayDecision(game)
            mayDecision.playerId shouldBe game.player2Id
            // Opponent declines.
            game.submitDecision(YesNoResponse(mayDecision.id, false))
            game.resolveStack()

            // No draw for the controller.
            game.handSize(1) shouldBe handBefore
            // X = 3 cards milled (Grizzly Bears, Hill Giant, Lightning Bolt).
            game.graveyardSize(1) shouldBe 3
            game.isInGraveyard(1, "Grizzly Bears") shouldNotBe false
            // Opponent loses life equal to total mana value of milled cards: 2 + 4 + 1 = 7.
            game.getLifeTotal(2) shouldBe 13
        }
    }
}

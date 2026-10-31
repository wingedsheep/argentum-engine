package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Pollywog Symbiote (IKO #63) — {1}{U} Creature — Frog 1/3.
 *
 *   Each creature spell you cast costs {1} less to cast if it has mutate.
 *   Whenever you cast a creature spell, if it has mutate, draw a card, then discard a card.
 *
 * The first "has mutate" spell filter: the reduction must reach the mutate alternative cost, and the
 * cast trigger must read Keyword.MUTATE off a spell on the stack.
 */
class PollywogSymbioteScenarioTest : ScenarioTestBase() {

    init {
        context("Pollywog Symbiote") {

            test("a mutate cast costs {1} less and loots") {
                val game = scenario()
                    .withPlayers("P1", "P2")
                    .withCardOnBattlefield(1, "Pollywog Symbiote")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardInHand(1, "Sea-Dasher Octopus")
                    .withCardInHand(1, "Hill Giant")
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(1, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val bears = game.findPermanent("Grizzly Bears")!!

                val offer = game.getLegalActions(1).singleOrNull {
                    (it.action as? CastSpell)?.alternativeCostType == AlternativeCostType.MUTATE
                }
                withClue("mutate {1}{U} reduced to {U} is affordable with one Island") {
                    (offer != null) shouldBe true
                }
                game.execute((offer!!.action as CastSpell).copy(targets = listOf(ChosenTarget.Permanent(bears))))
                    .error shouldBe null

                // The loot trigger resolves first, on top of the mutating spell.
                game.resolveStack()
                val discard = game.getPendingDecision() as? SelectCardsDecision
                    ?: error("expected a discard selection; got ${game.getPendingDecision()}")
                withClue("drew a card: Hill Giant plus the drawn Island to choose from") {
                    discard.options.size shouldBe 2
                }
                game.selectCards(game.findCardsInHand(1, "Hill Giant")).error shouldBe null

                withClue("Hill Giant discarded, drawn Island kept") {
                    game.isInGraveyard(1, "Hill Giant") shouldBe true
                    game.isInHand(1, "Island") shouldBe true
                    game.handSize(1) shouldBe 1
                }
            }

            test("a creature spell without mutate is not reduced") {
                val game = scenario()
                    .withPlayers("P1", "P2")
                    .withCardOnBattlefield(1, "Pollywog Symbiote")
                    .withCardInHand(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withCardInLibrary(1, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                withClue("Grizzly Bears still costs {1}{G}") {
                    (game.castSpell(1, "Grizzly Bears").error != null) shouldBe true
                }
            }
        }
    }
}

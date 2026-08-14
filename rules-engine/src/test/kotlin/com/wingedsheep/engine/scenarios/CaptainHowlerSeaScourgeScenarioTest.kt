package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Captain Howler, Sea Scourge (DFT #194) — {2}{U}{R} Legendary Creature — Shark Pirate, 5/4.
 *
 *   "Ward—{2}, Pay 2 life.
 *    Whenever you discard one or more cards, target creature gets +2/+0 until end of turn for each
 *    card discarded this way. Whenever that creature deals combat damage to a player this turn,
 *    you draw a card."
 *
 * Two things are worth pinning. The pump doubles the *batch* size (CR 603.2c), so a single
 * two-card discard event is +4/+0 rather than two separate +2/+0 triggers. And the follow-on
 * sentence is a delayed triggered ability watching the pumped creature: it draws only on combat
 * damage dealt **to a player**, and it keeps watching until end of turn rather than firing once.
 */
class CaptainHowlerSeaScourgeScenarioTest : ScenarioTestBase() {

    private val projector = StateProjector()

    init {
        context("the discard payoff scales with the batch") {

            test("a two-card discard event gives +4/+0, not +2/+0") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Captain Howler, Sea Scourge", summoningSickness = false)
                    .withCardOnBattlefield(1, "Hill Giant", summoningSickness = false)
                    .withCardInHand(1, "Faithless Looting")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .stocked()
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val giant = game.findPermanent("Hill Giant")!!

                // Faithless Looting: draw two, then discard two — one CardsDiscardedEvent.
                val cast = game.castSpell(1, "Faithless Looting")
                withClue("Faithless Looting cast should succeed: ${cast.error}") {
                    cast.error shouldBe null
                }
                if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
                game.resolveStack()

                // With exactly the two drawn cards in hand the discard may resolve without a
                // prompt; when it does prompt, bin both Bears.
                if (game.state.pendingDecision is SelectCardsDecision) {
                    game.selectCards(game.findCardsInHand(1, "Grizzly Bears").take(2))
                }

                val targeting = game.state.pendingDecision as? ChooseTargetsDecision
                withClue("The discard trigger pauses to choose target creature") {
                    targeting shouldNotBe null
                }
                game.selectTargets(listOf(giant))
                game.resolveStack()

                withClue("Hill Giant is 3/3 pumped by +2/+0 per card in the two-card batch") {
                    projector.getProjectedPower(game.state, giant) shouldBe 7
                    projector.getProjectedToughness(game.state, giant) shouldBe 3
                }
            }
        }

        context("the delayed draw trigger") {

            test("draws when the pumped creature connects with a player") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Captain Howler, Sea Scourge", summoningSickness = false)
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withCardInHand(1, "Chitin Gravestalker")
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .stocked()
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                val libraryBefore = game.librarySize(1)

                // Cycling {2} discards Chitin Gravestalker — a one-card discard event.
                val cycle = game.cycleCard(1, "Chitin Gravestalker")
                withClue("Cycling should succeed: ${cycle.error}") { cycle.error shouldBe null }
                if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()

                val targeting = game.state.pendingDecision as? ChooseTargetsDecision
                withClue("The discard trigger pauses to choose target creature") {
                    targeting shouldNotBe null
                }
                game.selectTargets(listOf(bears))
                game.resolveStack()

                withClue("Grizzly Bears is 2/2 pumped to 4/2 by the one-card batch") {
                    projector.getProjectedPower(game.state, bears) shouldBe 4
                }

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareNoBlockers()
                game.passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)
                game.resolveStack()

                withClue("The unblocked 4/2 hit for 4") {
                    game.getLifeTotal(2) shouldBe 16
                }
                withClue("Cycling drew one card, the delayed combat-damage trigger drew another") {
                    game.librarySize(1) shouldBe libraryBefore - 2
                }
            }

            test("does not draw when the pumped creature only damages a blocker") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Captain Howler, Sea Scourge", summoningSickness = false)
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withCardOnBattlefield(2, "Hill Giant", summoningSickness = false)
                    .withCardInHand(1, "Chitin Gravestalker")
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .stocked()
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                val libraryBefore = game.librarySize(1)

                val cycle = game.cycleCard(1, "Chitin Gravestalker")
                withClue("Cycling should succeed: ${cycle.error}") { cycle.error shouldBe null }
                if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
                game.selectTargets(listOf(bears))
                game.resolveStack()

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareBlockers(mapOf("Hill Giant" to listOf("Grizzly Bears"))).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)
                game.resolveStack()

                withClue("The blocked attacker dealt no damage to the player") {
                    game.getLifeTotal(2) shouldBe 20
                }
                withClue("Only the cycling draw happened — combat damage to a creature doesn't draw") {
                    game.librarySize(1) shouldBe libraryBefore - 1
                }
            }
        }
    }

    /** Both libraries stocked so nobody decks out on the draws these cases force. */
    private fun ScenarioBuilder.stocked(): ScenarioBuilder = apply {
        repeat(8) {
            withCardInLibrary(1, "Grizzly Bears")
            withCardInLibrary(2, "Grizzly Bears")
        }
    }
}

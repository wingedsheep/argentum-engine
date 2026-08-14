package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Magmakin Artillerist — "Whenever you discard one or more cards, this creature deals that much
 * damage to each opponent."
 *
 * The card is the first consumer of `ContextPropertyKey.TRIGGER_DISCARD_COUNT`, so these cases pin
 * the batch semantics the new key carries (CR 603.2c): the trigger fires once per
 * `CardsDiscardedEvent` and scales with the size of *that* event, not with the number of cards
 * discarded over the whole resolution.
 */
class MagmakinArtilleristScenarioTest : ScenarioTestBase() {

    init {
        context("the discard payoff") {

            test("a two-card discard event deals 2 damage to each opponent") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Magmakin Artillerist", summoningSickness = false)
                    .withCardInHand(1, "Faithless Looting")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .stocked()
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Faithless Looting: draw two, then discard two — a single CardsDiscardedEvent.
                val cast = game.castSpell(1, "Faithless Looting")
                withClue("Faithless Looting cast should succeed: ${cast.error}") {
                    cast.error shouldBe null
                }
                if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
                game.resolveStack()
                if (game.hasPendingDecision()) {
                    game.selectCards(game.findCardsInHand(1, "Grizzly Bears").take(2))
                }
                game.resolveStack()

                withClue("One discard event of two cards → \"that much\" is 2") {
                    game.getLifeTotal(2) shouldBe 18
                }
                withClue("The Artillerist's controller is not an opponent of themselves") {
                    game.getLifeTotal(1) shouldBe 20
                }
            }

            test("a one-card discard event deals 1 damage") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Magmakin Artillerist", summoningSickness = false)
                    .withCardInHand(1, "Chitin Gravestalker")
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .stocked()
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cycling {2} discards Chitin Gravestalker — one card, one event.
                val cycle = game.cycleCard(1, "Chitin Gravestalker")
                withClue("Cycling should succeed: ${cycle.error}") { cycle.error shouldBe null }
                if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
                game.resolveStack()

                withClue("Cycling another card is a one-card discard → 1 damage") {
                    game.getLifeTotal(2) shouldBe 19
                }
            }
        }

        context("its own cycling") {

            test("cycling it from hand deals 1 with no battlefield copy in play") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Magmakin Artillerist")
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .stocked()
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cycle = game.cycleCard(1, "Magmakin Artillerist")
                withClue("Cycling should succeed: ${cycle.error}") { cycle.error shouldBe null }
                if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
                game.resolveStack()

                withClue(
                    "Only the cycle trigger fires — the discard payoff is a battlefield ability " +
                        "and doesn't function from hand"
                ) {
                    game.getLifeTotal(2) shouldBe 19
                }
            }

            test("a copy on the battlefield also sees the cycling discard, for 2 total") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Magmakin Artillerist", summoningSickness = false)
                    .withCardInHand(1, "Magmakin Artillerist")
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .stocked()
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cycle = game.cycleCard(1, "Magmakin Artillerist")
                withClue("Cycling should succeed: ${cycle.error}") { cycle.error shouldBe null }
                if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
                game.resolveStack()

                withClue("1 from the cycled card's own trigger + 1 from the battlefield copy") {
                    game.getLifeTotal(2) shouldBe 18
                }
            }
        }
    }

    /** Both libraries stocked so nobody decks out on the draws these cases force. */
    private fun ScenarioBuilder.stocked(): ScenarioBuilder = apply {
        repeat(6) {
            withCardInLibrary(1, "Grizzly Bears")
            withCardInLibrary(2, "Grizzly Bears")
        }
    }
}

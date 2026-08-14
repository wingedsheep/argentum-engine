package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Marauding Mako — "Whenever you discard one or more cards, put that many +1/+1 counters on this
 * creature." plus Cycling {2}.
 *
 * Batch semantics (CR 603.2c): one trigger per discard *event*, sized by that event. These cases pin
 * the two-card case (one counter per card of a single event) and the "cycling this card doesn't feed
 * its own trigger" edge, since the discard ability only functions on the battlefield.
 */
class MaraudingMakoScenarioTest : ScenarioTestBase() {

    init {
        test("a two-card discard event adds two +1/+1 counters") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Marauding Mako", summoningSickness = false)
                .withCardInHand(1, "Faithless Looting")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .stocked()
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val mako = game.findPermanent("Marauding Mako")!!

            // Faithless Looting: draw two, then discard two — a single discard event.
            val cast = game.castSpell(1, "Faithless Looting")
            withClue("Faithless Looting cast should succeed: ${cast.error}") { cast.error shouldBe null }
            if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
            game.resolveStack()
            if (game.hasPendingDecision()) {
                game.selectCards(game.findCardsInHand(1, "Grizzly Bears").take(2))
            }
            game.resolveStack()

            withClue("\"That many\" reads the size of the one discard event: 2") {
                game.counters(mako) shouldBe 2
                game.state.projectedState.getPower(mako) shouldBe 3
                game.state.projectedState.getToughness(mako) shouldBe 3
            }
        }

        test("cycling another card is a one-card discard event") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Marauding Mako", summoningSickness = false)
                .withCardInHand(1, "Agonasaur Rex")
                .withLandsOnBattlefield(1, "Forest", 3)
                .stocked()
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val mako = game.findPermanent("Marauding Mako")!!

            val cycle = game.cycleCard(1, "Agonasaur Rex")
            withClue("Cycling should succeed: ${cycle.error}") { cycle.error shouldBe null }
            if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
            // The Rex's own cycle trigger targets "up to one" — decline it, we only care about the Mako.
            if (game.hasPendingDecision()) game.skipTargets()
            game.resolveStack()

            withClue("One card discarded → one counter") { game.counters(mako) shouldBe 1 }
        }

        test("cycling the Mako itself doesn't trigger it — the ability only works on the battlefield") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Marauding Mako")
                .withLandsOnBattlefield(1, "Mountain", 2)
                .stocked()
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val handBefore = game.handSize(1)
            val cycle = game.cycleCard(1, "Marauding Mako")
            withClue("Cycling should succeed: ${cycle.error}") { cycle.error shouldBe null }
            if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
            game.resolveStack()

            withClue("It went to the graveyard and replaced itself with a draw") {
                game.isInGraveyard(1, "Marauding Mako") shouldBe true
                game.handSize(1) shouldBe handBefore
            }
        }
    }

    private fun TestGame.counters(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    /** Both libraries stocked so nobody decks out on the draws these cases force. */
    private fun ScenarioBuilder.stocked(): ScenarioBuilder = apply {
        repeat(8) {
            withCardInLibrary(1, "Grizzly Bears")
            withCardInLibrary(2, "Grizzly Bears")
        }
    }
}

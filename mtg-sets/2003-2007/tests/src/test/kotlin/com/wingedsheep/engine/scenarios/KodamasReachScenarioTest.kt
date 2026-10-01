package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Kodama's Reach — {2}{G} Sorcery — Arcane
 * "Search your library for up to two basic land cards, reveal those cards, put one onto the
 * battlefield tapped and the other into your hand, then shuffle."
 *
 * Regression: the card used to send *both* lands to hand. A second selection now decides which
 * found land enters tapped; the other goes to hand.
 */
class KodamasReachScenarioTest : ScenarioTestBase() {

    private fun game(vararg library: String) = scenario()
        .withPlayers("Player1", "Player2")
        .withCardInHand(1, "Kodama's Reach")
        .withLandsOnBattlefield(1, "Forest", 3)
        .apply { library.forEach { withCardInLibrary(1, it) } }
        .withActivePlayer(1)
        .withPriorityPlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("one basic enters tapped and the other goes to hand") {
            val g = game("Plains", "Island", "Ancestral Recall")

            g.castSpell(1, "Kodama's Reach").error shouldBe null
            g.resolveStack()

            val plains = g.findCardsInLibrary(1, "Plains").first()
            val island = g.findCardsInLibrary(1, "Island").first()
            (g.getPendingDecision() is SelectCardsDecision) shouldBe true
            withClue("only basic lands are searchable") {
                (g.getPendingDecision() as SelectCardsDecision).options
                    .contains(g.findCardsInLibrary(1, "Ancestral Recall").first()) shouldBe false
            }
            g.selectCards(listOf(plains, island)).error shouldBe null

            (g.getPendingDecision() is SelectCardsDecision) shouldBe true
            g.selectCards(listOf(plains)).error shouldBe null
            g.resolveStack()

            withClue("the Plains entered the battlefield tapped") {
                g.isOnBattlefield("Plains") shouldBe true
                (g.state.getEntity(g.findPermanent("Plains")!!)?.has<TappedComponent>() ?: false) shouldBe true
                g.isInHand(1, "Plains") shouldBe false
            }
            withClue("the Island went to hand, not the battlefield") {
                g.isInHand(1, "Island") shouldBe true
                g.isOnBattlefield("Island") shouldBe false
            }
        }

        test("a single basic found can still enter the battlefield tapped") {
            val g = game("Plains")

            g.castSpell(1, "Kodama's Reach").error shouldBe null
            g.resolveStack()

            var guard = 0
            while (g.getPendingDecision() is SelectCardsDecision && guard++ < 4) {
                val plains = g.findCardsInLibrary(1, "Plains").firstOrNull()
                if (plains != null) g.selectCards(listOf(plains)) else g.skipSelection()
                g.resolveStack()
            }

            withClue("the only basic found enters tapped") {
                g.isOnBattlefield("Plains") shouldBe true
                (g.state.getEntity(g.findPermanent("Plains")!!)?.has<TappedComponent>() ?: false) shouldBe true
                g.isInHand(1, "Plains") shouldBe false
            }
        }
    }
}

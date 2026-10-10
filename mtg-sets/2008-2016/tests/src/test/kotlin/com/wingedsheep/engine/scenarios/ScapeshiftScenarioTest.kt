package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scapeshift — {2}{G}{G} Sorcery (MOR #136)
 * "Sacrifice any number of lands. Search your library for up to that many land cards, put them
 * onto the battlefield tapped, then shuffle."
 *
 * The search's "up to that many" is a cost-linked amount read after the sacrifice decision has
 * paused and resumed, so it is proven here rather than trusted to the snapshot.
 */
class ScapeshiftScenarioTest : ScenarioTestBase() {

    private fun game() = scenario()
        .withPlayers("Player1", "Player2")
        .withCardInHand(1, "Scapeshift")
        .withLandsOnBattlefield(1, "Forest", 6)
        .withCardInLibrary(1, "Mountain")
        .withCardInLibrary(1, "Island")
        .withCardInLibrary(1, "Plains")
        .withCardInLibrary(1, "Grizzly Bears")
        .withActivePlayer(1)
        .withPriorityPlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("sacrificing two lands lets you fetch up to two lands, which enter tapped") {
            val g = game()

            g.castSpell(1, "Scapeshift").error shouldBe null
            g.resolveStack()

            val sacrifice = g.getPendingDecision() as SelectCardsDecision
            val forests = g.findPermanents("Forest")
            g.selectCards(forests.take(2)).error shouldBe null

            val search = g.getPendingDecision() as SelectCardsDecision
            withClue("the search allows up to as many lands as were sacrificed") {
                search.maxSelections shouldBe 2
            }
            withClue("only land cards are searchable") {
                search.options.contains(g.findCardsInLibrary(1, "Grizzly Bears").first()) shouldBe false
            }
            val mountain = g.findCardsInLibrary(1, "Mountain").first()
            val island = g.findCardsInLibrary(1, "Island").first()
            g.selectCards(listOf(mountain, island)).error shouldBe null
            g.resolveStack()

            withClue("the two chosen Forests were sacrificed") {
                g.findCardsInGraveyard(1, "Forest").size shouldBe 2
                g.findPermanents("Forest").size shouldBe 4
            }
            withClue("the fetched lands entered tapped") {
                listOf("Mountain", "Island").forEach { name ->
                    g.isOnBattlefield(name) shouldBe true
                    (g.state.getEntity(g.findPermanent(name)!!)?.has<TappedComponent>() ?: false) shouldBe true
                }
                g.isOnBattlefield("Plains") shouldBe false
            }
            sacrifice.options.size shouldBe 6
        }

        test("sacrificing no lands finds nothing") {
            val g = game()

            g.castSpell(1, "Scapeshift").error shouldBe null
            g.resolveStack()

            g.selectCards(emptyList()).error shouldBe null
            var guard = 0
            while (g.getPendingDecision() is SelectCardsDecision && guard++ < 3) {
                (g.getPendingDecision() as SelectCardsDecision).maxSelections shouldBe 0
                g.skipSelection()
            }
            g.resolveStack()

            g.findPermanents("Forest").size shouldBe 6
            g.isOnBattlefield("Mountain") shouldBe false
            g.isOnBattlefield("Island") shouldBe false
        }
    }
}

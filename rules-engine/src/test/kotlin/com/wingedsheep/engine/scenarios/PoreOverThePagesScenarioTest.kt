package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Pore Over the Pages (DDQ #28) — draw three, untap up to two lands, then discard one. The lands
 * are chosen on resolution and are not targets, so this drives the pipeline's card-selection
 * decisions rather than cast-time targeting.
 */
class PoreOverThePagesScenarioTest : ScenarioTestBase() {
    init {
        test("draws three, untaps two chosen lands, then discards one") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardInHand(1, "Pore Over the Pages")
                .withLandsOnBattlefield(1, "Island", 5)
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(1, "Mountain")
                .withCardInLibrary(1, "Swamp")
                .withCardInLibrary(1, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val handBefore = game.state.getZone(game.player1Id, Zone.HAND).size
            game.castSpell(1, "Pore Over the Pages").error shouldBe null
            game.resolveStack()

            // Five Islands were tapped to pay {3}{U}{U}.
            val islands = game.findPermanents("Island")
            withClue("all five Islands paid for the spell") {
                islands.count { game.state.getEntity(it)!!.has<TappedComponent>() } shouldBe 5
            }

            // First decision: choose up to two lands to untap. Then: choose a card to discard.
            var untapped = emptyList<EntityId>()
            repeat(8) {
                val d = game.state.pendingDecision as? SelectCardsDecision ?: return@repeat
                val picked = d.options.take(2)
                if (untapped.isEmpty() && picked.isNotEmpty() && picked.all { it in islands }) {
                    untapped = picked
                    game.selectCards(picked)
                } else {
                    game.selectCards(d.options.take(maxOf(d.minSelections, 1)))
                }
                if (game.state.stack.isNotEmpty()) game.resolveStack()
            }

            withClue("exactly the two chosen lands are untapped again") {
                untapped.size shouldBe 2
                untapped.count { !game.state.getEntity(it)!!.has<TappedComponent>() } shouldBe 2
                islands.count { game.state.getEntity(it)!!.has<TappedComponent>() } shouldBe 3
            }
            withClue("net +1 after cast(-1) / draw3(+3) / discard1(-1)") {
                game.state.getZone(game.player1Id, Zone.HAND).size shouldBe handBefore + 1
            }
        }

        test("untapping is optional — choosing no lands still draws and discards") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardInHand(1, "Pore Over the Pages")
                .withLandsOnBattlefield(1, "Island", 5)
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(1, "Mountain")
                .withCardInLibrary(1, "Swamp")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val handBefore = game.state.getZone(game.player1Id, Zone.HAND).size
            game.castSpell(1, "Pore Over the Pages").error shouldBe null
            game.resolveStack()

            repeat(8) {
                val d = game.state.pendingDecision as? SelectCardsDecision ?: return@repeat
                // minSelections is 0 for the "up to two lands" step and 1 for the discard.
                game.selectCards(d.options.take(d.minSelections))
                if (game.state.stack.isNotEmpty()) game.resolveStack()
            }

            withClue("ChooseUpTo(2) accepts an empty pick; the rest of the spell still happens") {
                game.state.getZone(game.player1Id, Zone.HAND).size shouldBe handBefore + 1
                game.findPermanents("Island")
                    .count { game.state.getEntity(it)!!.has<TappedComponent>() } shouldBe 5
            }
        }
    }
}

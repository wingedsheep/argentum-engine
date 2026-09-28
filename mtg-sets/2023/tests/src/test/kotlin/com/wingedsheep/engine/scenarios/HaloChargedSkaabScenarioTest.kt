package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.OrderedResponse
import com.wingedsheep.engine.core.ReorderLibraryDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

class HaloChargedSkaabScenarioTest : ScenarioTestBase() {

    private fun build() = scenario()
        .withPlayers("Player", "Opponent")
        .withCardInHand(1, "Halo-Charged Skaab")
        .withLandsOnBattlefield(1, "Island", 5)
        .withCardInGraveyard(1, "Lightning Bolt")
        .withCardInGraveyard(1, "Grizzly Bears")
        .apply { repeat(5) { withCardInLibrary(1, "Island") } }
        .apply { repeat(5) { withCardInLibrary(2, "Swamp") } }
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("each player mills two, then you may put an instant from graveyard on top") {
            val game = build()
            game.castSpell(1, "Halo-Charged Skaab").error shouldBe null
            game.resolveStack()
            val d = game.getPendingDecision() as SelectCardsDecision
            d.options.size shouldBe 1
            game.selectCards(d.options).error shouldBe null
            (game.getPendingDecision() as? ReorderLibraryDecision)?.let {
                game.submitDecision(OrderedResponse(it.id, it.cards)).error shouldBe null
            }
            game.resolveStack()

            game.graveyardSize(2) shouldBe 2
            game.isInGraveyard(1, "Lightning Bolt") shouldBe false
            val top = game.state.getLibrary(game.player1Id).first()
            game.state.getEntity(top)?.get<CardComponent>()?.name shouldBe "Lightning Bolt"
            game.librarySize(1) shouldBe 4
        }

        test("declining leaves the graveyard alone") {
            val game = build()
            game.castSpell(1, "Halo-Charged Skaab").error shouldBe null
            game.resolveStack()
            game.skipSelection().error shouldBe null
            game.resolveStack()
            game.isInGraveyard(1, "Lightning Bolt") shouldBe true
            game.librarySize(1) shouldBe 3
        }
    }
}

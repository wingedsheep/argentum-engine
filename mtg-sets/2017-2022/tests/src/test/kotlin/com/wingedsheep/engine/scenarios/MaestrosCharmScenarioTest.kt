package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Maestros Charm (SNC #199) — choose one: look at the top five, keep one and bin the rest; each
 * opponent loses 3 life and you gain 3 life; or 5 damage to target creature or planeswalker.
 */
class MaestrosCharmScenarioTest : ScenarioTestBase() {

    private fun board() = scenario()
        .withPlayers()
        .withCardInHand(1, "Maestros Charm")
        .withLandsOnBattlefield(1, "Island", 1)
        .withLandsOnBattlefield(1, "Swamp", 1)
        .withLandsOnBattlefield(1, "Mountain", 1)
        .withCardOnBattlefield(2, "Grizzly Bears")
        .apply {
            listOf("Grizzly Bears", "Forest", "Plains", "Island", "Swamp", "Mountain")
                .forEach { withCardInLibrary(1, it) }
        }
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("mode 1 puts one of the top five into hand and the other four into the graveyard") {
            val game = board()
            game.castSpellWithMode(1, "Maestros Charm", modeIndex = 0).error shouldBe null
            game.resolveStack()

            val decision = game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
            decision.options.size shouldBe 5
            val kept = decision.options.first()
            game.selectCards(listOf(kept))
            game.resolveStack()

            game.state.getHand(game.player1Id) shouldBe listOf(kept)
            game.librarySize(1) shouldBe 1
            game.graveyardSize(1) shouldBe 5 // four looked-at cards plus the Charm
        }

        test("mode 2 drains each opponent for 3") {
            val game = board()
            game.castSpellWithMode(1, "Maestros Charm", modeIndex = 1).error shouldBe null
            game.resolveStack()

            game.getLifeTotal(2) shouldBe 17
            game.getLifeTotal(1) shouldBe 23
        }

        test("mode 3 deals 5 damage to target creature") {
            val game = board()
            val bears = game.findPermanent("Grizzly Bears")!!
            game.castSpellWithMode(1, "Maestros Charm", modeIndex = 2, targetId = bears).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(2, "Grizzly Bears") shouldBe true
        }
    }
}

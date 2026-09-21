package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.nulls.shouldNotBeNull
import com.wingedsheep.engine.core.SelectCardsDecision

class LoyalTutorScenarioTest : ScenarioTestBase() {
    init {
        test("offers only planeswalkers and puts the chosen card on top") {
            val game = scenario().withPlayers()
                .withCardInHand(1, "Loyal Tutor")
                .withLandsOnBattlefield(1, "Plains", 1)
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(1, "Jace Beleren")
                .withCardInLibrary(1, "Grizzly Bears")
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()
            val jace = game.findCardsInLibrary(1, "Jace Beleren").single()
            game.castSpell(1, "Loyal Tutor").error shouldBe null
            game.resolveStack()
            val decision = game.state.pendingDecision as SelectCardsDecision
            decision.options shouldBe listOf(jace)
            decision.minSelections shouldBe 0
            game.selectCards(listOf(jace)).error shouldBe null
            game.resolveStack()
            game.state.getLibrary(game.player1Id).first() shouldBe jace
            game.isInHand(1, "Jace Beleren") shouldBe false
        }
        test("allows failing to find even when a planeswalker exists") {
            val game = scenario().withPlayers()
                .withCardInHand(1, "Loyal Tutor")
                .withLandsOnBattlefield(1, "Plains", 1)
                .withCardInLibrary(1, "Jace Beleren")
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()
            game.castSpell(1, "Loyal Tutor").error shouldBe null
            game.resolveStack()
            game.skipSelection().error shouldBe null
            game.resolveStack()
            game.state.pendingDecision shouldBe null
            game.findCardsInLibrary(1, "Jace Beleren").size shouldBe 1
        }
    }
}

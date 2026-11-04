package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Razaketh's Rite (HOU) — tutor any card to hand, then shuffle; Cycling {B}.
 */
class RazakethsRiteScenarioTest : ScenarioTestBase() {
    init {
        test("searches the library for any card and puts it into hand") {
            val game = scenario().withPlayers()
                .withCardInHand(1, "Razaketh's Rite")
                .withLandsOnBattlefield(1, "Swamp", 5)
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(1, "Lightning Bolt")
                .withCardInLibrary(1, "Island")
                .withActivePlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()

            game.castSpell(1, "Razaketh's Rite").error shouldBe null
            game.resolveStack()

            val decision = game.getPendingDecision()
            decision.shouldBeInstanceOf<SelectCardsDecision>()
            val bolt = game.findCardsInLibrary(1, "Lightning Bolt").single()
            game.selectCards(listOf(bolt)).error shouldBe null

            game.isInHand(1, "Lightning Bolt") shouldBe true
            game.librarySize(1) shouldBe 2
            game.isInGraveyard(1, "Razaketh's Rite") shouldBe true
        }

        test("cycling {B} draws a card") {
            val game = scenario().withPlayers()
                .withCardInHand(1, "Razaketh's Rite")
                .withLandsOnBattlefield(1, "Swamp", 1)
                .withCardInLibrary(1, "Island")
                .withActivePlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()

            game.cycleCard(1, "Razaketh's Rite").error shouldBe null
            game.isInGraveyard(1, "Razaketh's Rite") shouldBe true
            game.isInHand(1, "Island") shouldBe true
        }
    }
}

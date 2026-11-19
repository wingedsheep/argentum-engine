package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Drown in Sorrow (BNG #65) — "All creatures get -2/-2 until end of turn. Scry 1."
 *
 * The shrink hits both players' creatures, then the caster scries.
 */
class DrownInSorrowScenarioTest : ScenarioTestBase() {

    init {
        test("every creature gets -2/-2, then scry 1") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardInHand(1, "Drown in Sorrow")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Hill Giant")
                .withLandsOnBattlefield(1, "Swamp", 3)
                .withCardInLibrary(1, "Swamp")
                .withCardInLibrary(1, "Grizzly Bears")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val giant = game.findPermanent("Hill Giant")!!
            game.castSpell(1, "Drown in Sorrow").error shouldBe null
            game.resolveStack()

            game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
            game.skipSelection().error shouldBe null
            game.resolveStack()

            withClue("your 2/2 dies") { game.isInGraveyard(1, "Grizzly Bears") shouldBe true }
            withClue("the opponent's 3/3 is a 1/1") {
                val projected = game.state.projectedState
                projected.getPower(giant) shouldBe 1
                projected.getToughness(giant) shouldBe 1
            }
        }
    }
}

package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Tromokratis — "has hexproof unless it's attacking or blocking" and "can't be blocked unless all
 * creatures defending player controls block it" (`CantBeBlockedUnlessAllDefendersBlock`).
 */
class TromokratisScenarioTest : ScenarioTestBase() {

    private fun combat(tappedBears: Boolean = false) = scenario()
        .withPlayers()
        .withCardOnBattlefield(1, "Tromokratis")
        .withCardOnBattlefield(2, "Savannah Lions")
        .withCardOnBattlefield(2, "Grizzly Bears", tapped = tappedBears)
        .withCardInLibrary(1, "Island")
        .withCardInLibrary(2, "Forest")
        .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
        .build()

    private fun ScenarioTestBase.TestGame.tromokratisHasHexproof(): Boolean =
        state.projectedState.hasKeyword(findPermanent("Tromokratis")!!, Keyword.HEXPROOF)

    init {
        test("one creature blocking while another doesn't is illegal; all blocking is legal") {
            val game = combat()
            game.declareAttackers(mapOf("Tromokratis" to 2))
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

            game.declareBlockers(mapOf("Savannah Lions" to listOf("Tromokratis"))).error shouldNotBe null

            game.declareBlockers(
                mapOf(
                    "Savannah Lions" to listOf("Tromokratis"),
                    "Grizzly Bears" to listOf("Tromokratis"),
                )
            ).error shouldBe null
        }

        test("a tapped creature the defender controls makes it unblockable") {
            val game = combat(tappedBears = true)
            game.declareAttackers(mapOf("Tromokratis" to 2))
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

            game.declareBlockers(mapOf("Savannah Lions" to listOf("Tromokratis"))).error shouldNotBe null
            game.declareNoBlockers().error shouldBe null
        }

        test("has hexproof until it attacks") {
            val game = combat()
            game.tromokratisHasHexproof() shouldBe true
            game.declareAttackers(mapOf("Tromokratis" to 2))
            game.tromokratisHasHexproof() shouldBe false
        }
    }
}

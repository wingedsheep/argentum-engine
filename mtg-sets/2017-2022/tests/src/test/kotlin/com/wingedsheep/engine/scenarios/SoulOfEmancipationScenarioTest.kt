package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

/**
 * Soul of Emancipation (SNC #223) — "When this creature enters, destroy up to three other target
 * nonland permanents. For each of those permanents, its controller creates a 3/3 white Angel
 * creature token with flying."
 *
 * Each Angel goes to the controller of the permanent it stands for, across players, and a legal
 * target that survives the destroy (indestructible) still hands its controller an Angel.
 */
class SoulOfEmancipationScenarioTest : ScenarioTestBase() {

    private fun build() = scenario()
        .withPlayers("Player1", "Player2")
        .withCardInHand(1, "Soul of Emancipation")
        .withCardOnBattlefield(1, "Grizzly Bears")
        .withCardOnBattlefield(2, "Hill Giant")
        .withCardOnBattlefield(2, "Darksteel Myr")
        .withLandsOnBattlefield(1, "Forest", 3)
        .withLandsOnBattlefield(1, "Plains", 2)
        .withLandsOnBattlefield(1, "Island", 2)
        .withCardInLibrary(1, "Forest")
        .withCardInLibrary(2, "Forest")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("each target's controller gets an Angel, even for a target that survives") {
            val game = build()
            val bears = game.findPermanent("Grizzly Bears")!!
            val giant = game.findPermanent("Hill Giant")!!
            val myr = game.findPermanent("Darksteel Myr")!!

            game.castSpell(1, "Soul of Emancipation").error shouldBe null
            game.resolveStack()
            withClue("the enters trigger asks for its targets") { game.hasPendingDecision() shouldBe true }
            game.selectTargets(listOf(bears, giant, myr)).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(1, "Grizzly Bears") shouldBe true
            game.isInGraveyard(2, "Hill Giant") shouldBe true
            withClue("indestructible Darksteel Myr survives the destroy") {
                game.isOnBattlefield("Darksteel Myr") shouldBe true
            }
            game.isOnBattlefield("Soul of Emancipation") shouldBe true

            val angels = game.findPermanents("Angel Token")
            angels shouldHaveSize 3
            val controllers = angels.map { game.state.projectedState.getController(it) }
            withClue("Player1 lost one permanent, Player2 had two targeted") {
                controllers.count { it == game.player1Id } shouldBe 1
                controllers.count { it == game.player2Id } shouldBe 2
            }
        }

        test("choosing no targets destroys nothing and creates no Angel") {
            val game = build()

            game.castSpell(1, "Soul of Emancipation").error shouldBe null
            game.resolveStack()
            game.skipTargets().error shouldBe null
            game.resolveStack()

            game.isOnBattlefield("Grizzly Bears") shouldBe true
            game.isOnBattlefield("Hill Giant") shouldBe true
            game.findPermanents("Angel Token") shouldHaveSize 0
        }
    }
}

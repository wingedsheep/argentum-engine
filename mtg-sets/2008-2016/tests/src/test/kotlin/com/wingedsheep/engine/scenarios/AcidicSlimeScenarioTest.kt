package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Acidic Slime (M10 #165) — {3}{G}{G} 2/2 Creature — Ooze.
 *   Deathtouch
 *   When this creature enters, destroy target artifact, enchantment, or land.
 */
class AcidicSlimeScenarioTest : ScenarioTestBase() {
    init {
        test("entering destroys target land and the Slime has deathtouch") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardInHand(1, "Acidic Slime")
                .withLandsOnBattlefield(1, "Forest", 5)
                .withLandsOnBattlefield(2, "Island", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val island = game.findPermanent("Island")!!

            game.castSpell(1, "Acidic Slime").error shouldBe null
            game.resolveStack()
            game.getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
            game.selectTargets(listOf(island)).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(2, "Island") shouldBe true
            game.state.projectedState.hasKeyword(game.findPermanent("Acidic Slime")!!, Keyword.DEATHTOUCH) shouldBe true
        }
    }
}

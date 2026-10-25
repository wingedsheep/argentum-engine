package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.PhasedOutComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Divine Smite (Adventures in the Forgotten Realms #12).
 *
 * Target creature or planeswalker an opponent controls phases out. If that permanent is black,
 * exile it instead.
 */
class DivineSmiteScenarioTest : ScenarioTestBase() {

    private fun setup(targetName: String) = scenario()
        .withPlayers("Player1", "Player2")
        .withCardInHand(1, "Divine Smite")
        .withLandsOnBattlefield(1, "Plains", 2)
        .withCardOnBattlefield(2, targetName)
        .withCardInLibrary(1, "Plains")
        .withCardInLibrary(2, "Plains")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("a nonblack creature phases out and stays on the battlefield") {
            val game = setup("Grizzly Bears")
            val bears = game.findPermanent("Grizzly Bears")!!

            game.castSpell(1, "Divine Smite", bears).error shouldBe null
            game.resolveStack()

            withClue("Grizzly Bears is phased out, not exiled") {
                (game.state.getEntity(bears)?.get<PhasedOutComponent>() != null) shouldBe true
                game.isInExile(2, "Grizzly Bears") shouldBe false
            }
        }

        test("a black creature is exiled instead of phasing out") {
            val game = setup("Walking Corpse")
            val corpse = game.findPermanent("Walking Corpse")!!

            game.castSpell(1, "Divine Smite", corpse).error shouldBe null
            game.resolveStack()

            withClue("Walking Corpse is exiled") {
                game.isInExile(2, "Walking Corpse") shouldBe true
                game.findPermanent("Walking Corpse") shouldBe null
            }
        }
    }
}

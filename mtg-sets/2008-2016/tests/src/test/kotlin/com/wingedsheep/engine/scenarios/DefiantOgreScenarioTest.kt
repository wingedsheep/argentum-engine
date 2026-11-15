package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Defiant Ogre (Fate Reforged #96) — {5}{R} Creature — Ogre Warrior 3/5.
 *
 * "When this creature enters, choose one — • Put a +1/+1 counter on this creature.
 *  • Destroy target artifact."
 */
class DefiantOgreScenarioTest : ScenarioTestBase() {

    private fun setup(): TestGame = scenario()
        .withPlayers("Player1", "Player2")
        .withCardInHand(1, "Defiant Ogre")
        .withLandsOnBattlefield(1, "Mountain", 6)
        .withCardOnBattlefield(2, "Ornithopter")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    /** Cast the Ogre, let it resolve, and pick the enters-trigger mode starting with [modePrefix]. */
    private fun TestGame.castAndChoose(modePrefix: String) {
        castSpell(1, "Defiant Ogre").error shouldBe null
        resolveStack()
        val decision = state.pendingDecision.shouldBeInstanceOf<ChooseOptionDecision>()
        val index = decision.options.indexOfFirst { it.startsWith(modePrefix) }
        check(index >= 0) { "$modePrefix not offered; options=${decision.options}" }
        submitDecision(OptionChosenResponse(decision.id, index)).error shouldBe null
    }

    init {
        test("first mode puts a +1/+1 counter on the Ogre") {
            val game = setup()
            game.castAndChoose("Put a +1/+1 counter")
            game.resolveStack()

            val ogre = game.findPermanent("Defiant Ogre")!!
            game.state.projectedState.getPower(ogre) shouldBe 4
            game.state.projectedState.getToughness(ogre) shouldBe 6
            game.isOnBattlefield("Ornithopter") shouldBe true
        }

        test("second mode destroys target artifact") {
            val game = setup()
            game.castAndChoose("Destroy target artifact")
            if (game.state.pendingDecision !is ChooseTargetsDecision) game.resolveStack()
            game.state.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
            game.selectTargets(listOf(game.findPermanent("Ornithopter")!!)).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(2, "Ornithopter") shouldBe true
            val ogre = game.findPermanent("Defiant Ogre")!!
            game.state.projectedState.getPower(ogre) shouldBe 3
        }
    }
}

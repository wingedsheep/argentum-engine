package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Force Spike (LEG): "Counter target spell unless its controller pays {1}."
 */
class ForceSpikeScenarioTest : ScenarioTestBase() {
    private fun bearsOnStack(forests: Int) = scenario()
        .withPlayers("Player1", "Player2")
        .withCardInHand(1, "Grizzly Bears")
        .withLandsOnBattlefield(1, "Forest", forests)
        .withCardInHand(2, "Force Spike")
        .withLandsOnBattlefield(2, "Island", 1)
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()
        .also { game ->
            game.castSpell(1, "Grizzly Bears").error shouldBe null
            game.passPriority()
            game.castSpellTargetingStackSpell(2, "Force Spike", "Grizzly Bears").error shouldBe null
        }

    init {
        test("counters the spell when its controller declines to pay") {
            val game = bearsOnStack(forests = 3)
            game.resolveStack()
            game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
            game.answerYesNo(false)
            game.resolveStack()

            game.isInGraveyard(1, "Grizzly Bears") shouldBe true
            game.isOnBattlefield("Grizzly Bears") shouldBe false
        }

        test("the spell resolves when its controller pays {1}") {
            val game = bearsOnStack(forests = 3)
            game.resolveStack()
            game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
            game.answerYesNo(true)
            game.getPendingDecision().shouldBeInstanceOf<SelectManaSourcesDecision>()
            game.submitManaSourcesAutoPay()
            game.resolveStack()

            game.isOnBattlefield("Grizzly Bears") shouldBe true
        }
    }
}

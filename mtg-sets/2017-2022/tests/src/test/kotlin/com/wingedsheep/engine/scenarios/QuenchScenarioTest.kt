package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Quench (RNA #48) — "Counter target spell unless its controller pays {2}."
 */
class QuenchScenarioTest : ScenarioTestBase() {
    private fun bearsOnStack(forests: Int) = scenario()
        .withPlayers("Alice", "Bob")
        .withCardInHand(1, "Grizzly Bears")
        .withLandsOnBattlefield(1, "Forest", forests)
        .withCardInHand(2, "Quench")
        .withLandsOnBattlefield(2, "Island", 2)
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()
        .also { game ->
            game.castSpell(1, "Grizzly Bears").error shouldBe null
            game.passPriority()
            game.castSpellTargetingStackSpell(2, "Quench", "Grizzly Bears").error shouldBe null
        }

    init {
        test("counters the spell when its controller declines to pay") {
            val game = bearsOnStack(forests = 4)
            game.resolveStack()
            game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
            game.answerYesNo(false)
            game.resolveStack()

            game.isInGraveyard(1, "Grizzly Bears") shouldBe true
        }

        test("the spell resolves when its controller pays {2}") {
            val game = bearsOnStack(forests = 4)
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

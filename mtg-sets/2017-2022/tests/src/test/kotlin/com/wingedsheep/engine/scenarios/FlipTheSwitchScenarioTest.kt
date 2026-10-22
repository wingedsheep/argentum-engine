package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Flip the Switch (MID #54): "Counter target spell unless its controller pays {4}. Create a 2/2
 * black Zombie creature token with decayed."
 *
 * The token follows the pay/decline decision, so it must survive the pause on the counter's
 * payment choice — and per the ruling it is created even when the tax is paid.
 */
class FlipTheSwitchScenarioTest : ScenarioTestBase() {
    private fun bearsOnStack(forests: Int) = scenario()
        .withPlayers("Player1", "Player2")
        .withCardInHand(1, "Grizzly Bears")
        .withLandsOnBattlefield(1, "Forest", forests)
        .withCardInHand(2, "Flip the Switch")
        .withLandsOnBattlefield(2, "Island", 3)
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()
        .also { game ->
            game.castSpell(1, "Grizzly Bears").error shouldBe null
            game.passPriority()
            game.castSpellTargetingStackSpell(2, "Flip the Switch", "Grizzly Bears").error shouldBe null
        }

    private fun assertDecayedZombie(game: TestGame) {
        val tokens = game.findPermanents("Zombie Token")
        tokens shouldHaveSize 1
        val token = tokens.single()
        game.state.projectedState.hasKeyword(token, Keyword.DECAYED) shouldBe true
        game.state.projectedState.getPower(token) shouldBe 2
        game.state.projectedState.getToughness(token) shouldBe 2
    }

    init {
        test("declining to pay counters the spell and creates a decayed Zombie") {
            val game = bearsOnStack(forests = 6)
            game.resolveStack()
            game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
            game.answerYesNo(false)
            game.resolveStack()

            game.isInGraveyard(1, "Grizzly Bears") shouldBe true
            assertDecayedZombie(game)
        }

        test("paying {4} lets the spell resolve but the Zombie is still created") {
            val game = bearsOnStack(forests = 6)
            game.resolveStack()
            game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
            game.answerYesNo(true)
            game.getPendingDecision().shouldBeInstanceOf<SelectManaSourcesDecision>()
            game.submitManaSourcesAutoPay()
            game.resolveStack()

            game.isOnBattlefield("Grizzly Bears") shouldBe true
            assertDecayedZombie(game)
        }
    }
}

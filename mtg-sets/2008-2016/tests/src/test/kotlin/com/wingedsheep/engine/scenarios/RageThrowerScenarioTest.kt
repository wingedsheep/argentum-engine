package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Rage Thrower (ISD #157) — {5}{R} 4/2 Human Shaman.
 *
 *   Whenever another creature dies, this creature deals 2 damage to target player or planeswalker.
 */
class RageThrowerScenarioTest : ScenarioTestBase() {

    private fun buildGame() = scenario()
        .withPlayers("Player1", "Player2")
        .withCardOnBattlefield(1, "Rage Thrower")
        .withCardOnBattlefield(2, "Grizzly Bears")
        .withCardInHand(1, "Lightning Bolt")
        .withLandsOnBattlefield(1, "Mountain", 1)
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("another creature dying deals 2 damage to the target player") {
            val game = buildGame()
            game.castSpell(1, "Lightning Bolt", game.findPermanent("Grizzly Bears")!!).error shouldBe null
            game.resolveStack()

            game.getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
            game.selectTargets(listOf(game.player2Id)).error shouldBe null
            game.resolveStack()

            game.getLifeTotal(2) shouldBe 18
            game.getLifeTotal(1) shouldBe 20
        }

        test("its own death doesn't trigger it") {
            val game = buildGame()
            game.castSpell(1, "Lightning Bolt", game.findPermanent("Rage Thrower")!!).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(1, "Rage Thrower") shouldBe true
            game.hasPendingDecision() shouldBe false
            game.state.stack.isEmpty() shouldBe true
            game.getLifeTotal(2) shouldBe 20
        }
    }
}

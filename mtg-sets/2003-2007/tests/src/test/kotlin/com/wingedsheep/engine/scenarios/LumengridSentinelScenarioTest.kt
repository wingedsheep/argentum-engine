package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Lumengrid Sentinel (MRD #40) — {2}{U} 1/2 Flying.
 *
 * "Whenever an artifact you control enters, you may tap target permanent."
 *
 * The "you may" is answered while choosing the target: picking one is the yes (no second
 * yes/no as the ability resolves), and the target prompt itself offers the decline.
 */
class LumengridSentinelScenarioTest : ScenarioTestBase() {

    private fun setup(): TestGame = scenario()
        .withPlayers("Player1", "Player2")
        .withCardOnBattlefield(1, "Lumengrid Sentinel")
        .withCardInHand(1, "Ornithopter")
        .withCardOnBattlefield(2, "Grizzly Bears")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    /** Cast Ornithopter and resolve it, stopping at the Sentinel's target prompt. */
    private fun castArtifact(game: TestGame): ChooseTargetsDecision {
        game.castSpell(1, "Ornithopter").error shouldBe null
        game.resolveStack()
        val decision = game.getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
        decision.targetRequirements.single().minTargets shouldBe 0
        return decision
    }

    private fun isTapped(game: TestGame, name: String): Boolean =
        game.state.getEntity(game.findPermanent(name)!!)?.has<TappedComponent>() == true

    init {
        test("choosing a target taps it without a second may-prompt") {
            val game = setup()
            castArtifact(game)

            game.selectTargets(listOf(game.findPermanent("Grizzly Bears")!!)).error shouldBe null
            game.resolveStack()

            game.hasPendingDecision() shouldBe false
            game.state.stack.isEmpty() shouldBe true
            isTapped(game, "Grizzly Bears") shouldBe true
        }

        test("declining at the target prompt does nothing") {
            val game = setup()
            castArtifact(game)

            game.selectTargets(emptyList()).error shouldBe null

            game.hasPendingDecision() shouldBe false
            game.state.stack.isEmpty() shouldBe true
            isTapped(game, "Grizzly Bears") shouldBe false
        }
    }
}

package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.TargetsResponse
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

/**
 * Zephyr Winder — "Flying. Whenever this creature deals combat damage to a player, untap up to
 * one target creature."
 */
class ZephyrWinderScenarioTest : ScenarioTestBase() {

    private fun board() = scenario()
        .withPlayers("Player1", "Player2")
        .withCardOnBattlefield(1, "Zephyr Winder", tapped = false, summoningSickness = false)
        .withCardOnBattlefield(1, "Grizzly Bears", tapped = true, summoningSickness = false)
        .withCardInLibrary(1, "Island")
        .withCardInLibrary(2, "Forest")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun TestGame.attackAndAwaitTrigger(): ChooseTargetsDecision {
        passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
        declareAttackers(mapOf("Zephyr Winder" to 2)).error shouldBe null
        passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)
        var guard = 0
        while (state.pendingDecision !is ChooseTargetsDecision && guard++ < 20) resolveStack()
        return state.pendingDecision as? ChooseTargetsDecision
            ?: error("expected Zephyr Winder's trigger to ask for a target; got ${state.pendingDecision}")
    }

    private fun TestGame.tapped(name: String): Boolean =
        state.getEntity(findPermanent(name)!!)!!.has<TappedComponent>()

    init {
        test("combat damage to a player untaps the chosen creature") {
            val game = board()
            val bears = game.findPermanent("Grizzly Bears")!!
            val decision = game.attackAndAwaitTrigger()
            game.getLifeTotal(2) shouldBe 18
            decision.legalTargets[0]!! shouldContain bears

            game.submitDecision(TargetsResponse(decision.id, mapOf(0 to listOf(bears)))).error shouldBe null
            game.resolveStack()

            withClue("Grizzly Bears untapped") { game.tapped("Grizzly Bears") shouldBe false }
            withClue("the attacking Winder is still tapped") { game.tapped("Zephyr Winder") shouldBe true }
        }

        test("it can untap itself after attacking") {
            val game = board()
            val winder = game.findPermanent("Zephyr Winder")!!
            val decision = game.attackAndAwaitTrigger()
            game.submitDecision(TargetsResponse(decision.id, mapOf(0 to listOf(winder)))).error shouldBe null
            game.resolveStack()

            game.tapped("Zephyr Winder") shouldBe false
            game.tapped("Grizzly Bears") shouldBe true
        }

        test("up to one — choosing no target untaps nothing") {
            val game = board()
            val decision = game.attackAndAwaitTrigger()
            game.submitDecision(TargetsResponse(decision.id, mapOf(0 to emptyList()))).error shouldBe null
            game.resolveStack()

            game.tapped("Grizzly Bears") shouldBe true
            game.tapped("Zephyr Winder") shouldBe true
        }
    }
}

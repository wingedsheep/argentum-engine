package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Bazaar Krovod (RTR) — Whenever this creature attacks, another target attacking creature gets
 * +0/+2 until end of turn. Untap that creature.
 */
class BazaarKrovodScenarioTest : ScenarioTestBase() {
    init {
        test("attack trigger gives another attacker +0/+2 and untaps it") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Bazaar Krovod")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Plains")
                .withActivePlayer(1)
                .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                .build()
            val bears = game.findPermanent("Grizzly Bears")!!

            val declare = game.declareAttackers(mapOf("Bazaar Krovod" to 2, "Grizzly Bears" to 2))
            withClue("declare attackers should succeed: ${declare.error}") { declare.error shouldBe null }
            withClue("attacking taps the Bears") { game.state.getEntity(bears)?.has<TappedComponent>() shouldBe true }

            if (game.getPendingDecision() is ChooseTargetsDecision) {
                game.selectTargets(listOf(bears)).error shouldBe null
            }
            game.resolveStack()

            withClue("the Bears got +0/+2 and were untapped") {
                game.state.projectedState.getPower(bears) shouldBe 2
                game.state.projectedState.getToughness(bears) shouldBe 4
                game.state.getEntity(bears)?.has<TappedComponent>() shouldBe false
            }
        }
    }
}

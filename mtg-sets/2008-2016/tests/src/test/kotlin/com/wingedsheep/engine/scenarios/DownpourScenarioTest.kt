package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Downpour (M13 #48) — {1}{U} Instant.
 *   Tap up to three target creatures.
 */
class DownpourScenarioTest : ScenarioTestBase() {
    init {
        test("taps each of three target creatures and leaves the rest alone") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardInHand(1, "Downpour")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardOnBattlefield(2, "Savannah Lions")
                .withCardOnBattlefield(2, "Guardian Lions")
                .withLandsOnBattlefield(1, "Island", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val targets = game.findPermanents("Grizzly Bears") + game.findPermanent("Savannah Lions")!!
            val untargeted = game.findPermanent("Guardian Lions")!!
            val downpour = game.findCardsInHand(1, "Downpour").single()

            val result = game.execute(
                CastSpell(game.player1Id, downpour, targets.map { ChosenTarget.Permanent(it) })
            )
            withClue("cast should succeed: ${result.error}") { result.error shouldBe null }
            game.resolveStack()

            targets.forEach { game.state.getEntity(it)?.has<TappedComponent>() shouldBe true }
            withClue("the fourth creature wasn't targeted") {
                game.state.getEntity(untargeted)?.has<TappedComponent>() shouldBe false
            }
        }
    }
}

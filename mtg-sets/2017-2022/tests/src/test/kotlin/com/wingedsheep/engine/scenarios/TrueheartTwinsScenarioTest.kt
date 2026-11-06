package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.ExertedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Trueheart Twins (AKH #153).
 *
 *   You may exert this creature as it attacks.
 *   Whenever you exert a creature, creatures you control get +1/+0 until end of turn.
 */
class TrueheartTwinsScenarioTest : ScenarioTestBase() {

    init {
        context("Trueheart Twins") {

            fun game() = scenario()
                .withPlayers("Alice", "Bob")
                .withCardOnBattlefield(1, "Trueheart Twins")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Hill Giant")
                .withCardInLibrary(1, "Mountain")
                .withCardInLibrary(2, "Mountain")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            test("exerting it pumps every creature you control, not the opponent's") {
                val game = game()
                val twins = game.findPermanent("Trueheart Twins")!!
                val bears = game.findPermanent("Grizzly Bears")!!
                val giant = game.findPermanent("Hill Giant")!!

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Trueheart Twins" to 2)).error shouldBe null
                game.selectCards(listOf(twins))
                game.state.getEntity(twins)!!.has<ExertedComponent>() shouldBe true
                game.resolveStack()

                val projected = game.state.projectedState
                projected.getPower(twins) shouldBe 5
                projected.getToughness(twins) shouldBe 4
                projected.getPower(bears) shouldBe 3
                projected.getToughness(bears) shouldBe 2
                projected.getPower(giant) shouldBe 3
            }

            test("declining the exert pumps nothing") {
                val game = game()
                val twins = game.findPermanent("Trueheart Twins")!!
                val bears = game.findPermanent("Grizzly Bears")!!

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Trueheart Twins" to 2)).error shouldBe null
                game.selectCards(emptyList())

                game.state.stack.isEmpty() shouldBe true
                val projected = game.state.projectedState
                projected.getPower(twins) shouldBe 4
                projected.getPower(bears) shouldBe 2
            }
        }
    }
}

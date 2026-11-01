package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Screaming Shield (RNA #239) — equipped creature gets +0/+3 and has "{2}, {T}: Target player
 * mills three cards."
 */
class ScreamingShieldScenarioTest : ScenarioTestBase() {
    init {
        test("the equipped creature gets +0/+3 and its granted ability mills three") {
            var builder = scenario()
                .withPlayers("Alice", "Bob")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardAttachedTo(1, "Screaming Shield", "Grizzly Bears")
                .withLandsOnBattlefield(1, "Island", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            repeat(4) { builder = builder.withCardInLibrary(2, "Hill Giant") }
            val game = builder.build()

            val bears = game.findPermanent("Grizzly Bears")!!
            game.state.projectedState.getPower(bears) shouldBe 2
            game.state.projectedState.getToughness(bears) shouldBe 5

            val granted = game.getLegalActions(1)
                .mapNotNull { it.action as? ActivateAbility }
                .firstOrNull { it.sourceId == bears }
                .shouldNotBeNull()
            game.execute(
                granted.copy(targets = listOf(ChosenTarget.Player(game.player2Id)))
            ).error shouldBe null
            game.resolveStack()

            game.graveyardSize(2) shouldBe 3
            game.librarySize(2) shouldBe 1
        }
    }
}

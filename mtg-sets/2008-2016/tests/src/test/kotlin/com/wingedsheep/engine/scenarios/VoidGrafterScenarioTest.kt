package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Void Grafter — {1}{G}{U} 2/4 Eldrazi Drone, devoid, flash.
 * "When this creature enters, another target creature you control gains hexproof until end of turn."
 */
class VoidGrafterScenarioTest : ScenarioTestBase() {
    init {
        test("flashed in on the opponent's turn, it gives another creature you control hexproof") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Void Grafter")
                .withLandsOnBattlefield(1, "Forest", 2)
                .withLandsOnBattlefield(1, "Island", 1)
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(1, "Hill Giant")
                .withActivePlayer(2)
                .withPriorityPlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val bears = game.findPermanent("Grizzly Bears")!!
            val giant = game.findPermanent("Hill Giant")!!

            val result = game.castSpell(1, "Void Grafter")
            withClue("flash cast on the opponent's turn should succeed: ${result.error}") { result.error shouldBe null }
            game.resolveStack()

            game.getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
            game.selectTargets(listOf(bears)).error shouldBe null
            game.resolveStack()

            val grafter = game.findPermanent("Void Grafter")!!
            val projected = game.state.projectedState
            projected.hasKeyword(bears, Keyword.HEXPROOF) shouldBe true
            projected.hasKeyword(giant, Keyword.HEXPROOF) shouldBe false
            projected.hasKeyword(grafter, Keyword.HEXPROOF) shouldBe false
        }

        test("it can't target itself, so alone it grants nothing") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Void Grafter")
                .withLandsOnBattlefield(1, "Forest", 2)
                .withLandsOnBattlefield(1, "Island", 1)
                .withActivePlayer(1)
                .withPriorityPlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Void Grafter").error shouldBe null
            game.resolveStack()
            if (game.getPendingDecision() is ChooseTargetsDecision) {
                error("Void Grafter must not be offered as its own target")
            }
            game.resolveStack()

            val grafter = game.findPermanent("Void Grafter")!!
            game.state.projectedState.hasKeyword(grafter, Keyword.HEXPROOF) shouldBe false
        }
    }
}

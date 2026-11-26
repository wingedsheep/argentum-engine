package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseColorDecision
import com.wingedsheep.engine.core.ColorChosenResponse
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Midvast Protector (AVR #28) — {3}{W} Creature — Human Wizard 2/3
 *
 *   When this creature enters, target creature you control gains protection from the color of
 *   your choice until end of turn.
 *
 * The colour is chosen on resolution, and the protection ends with the turn.
 */
class MidvastProtectorScenarioTest : ScenarioTestBase() {

    init {
        test("the entering trigger grants protection from the chosen color until end of turn") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInHand(1, "Midvast Protector")
                .withLandsOnBattlefield(1, "Plains", 4)
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Plains")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            game.castSpell(1, "Midvast Protector").error shouldBe null
            game.resolveStack()
            game.selectTargets(listOf(bears)).error shouldBe null
            game.resolveStack()
            val decision = game.getPendingDecision().shouldBeInstanceOf<ChooseColorDecision>()
            game.submitDecision(ColorChosenResponse(decision.id, Color.RED)).error shouldBe null
            game.resolveStack()

            withClue("Bears has protection from red") {
                game.state.projectedState.hasKeyword(bears, "PROTECTION_FROM_RED") shouldBe true
            }
            withClue("and only from the chosen color") {
                game.state.projectedState.hasKeyword(bears, "PROTECTION_FROM_BLACK") shouldBe false
            }

            game.passUntilPhase(Phase.ENDING, Step.CLEANUP)
            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            withClue("the protection ends at end of turn") {
                game.state.projectedState.hasKeyword(bears, "PROTECTION_FROM_RED") shouldBe false
            }
        }
    }
}

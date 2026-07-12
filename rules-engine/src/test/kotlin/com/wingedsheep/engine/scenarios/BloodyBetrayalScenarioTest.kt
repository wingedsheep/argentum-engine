package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Bloody Betrayal (VOW #147) — {2}{R} Sorcery.
 *
 *   Gain control of target creature until end of turn. Untap that creature. It gains haste
 *   until end of turn. Create a Blood token.
 *
 * Exercises the control-steal composite: control moves to the caster, the creature is untapped
 * and gains haste, and a Blood token is created.
 */
class BloodyBetrayalScenarioTest : ScenarioTestBase() {

    init {
        context("Bloody Betrayal") {

            test("steals a tapped opponent's creature, untaps it, grants haste, and creates a Blood token") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Bloody Betrayal")
                    .withCardOnBattlefield(2, "Hill Giant", tapped = true)
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val giant = game.findPermanent("Hill Giant")!!

                withClue("Hill Giant starts under Player2's control, tapped") {
                    game.state.getBattlefield(game.player2Id).contains(giant) shouldBe true
                    game.state.getEntity(giant)?.has<com.wingedsheep.engine.state.components.battlefield.TappedComponent>() shouldBe true
                }

                game.castSpell(1, "Bloody Betrayal", targetId = giant).error shouldBe null
                game.resolveStack()

                withClue("Player1 gains control of Hill Giant") {
                    game.state.projectedState.getController(giant) shouldBe game.player1Id
                }
                withClue("Hill Giant is untapped") {
                    game.state.getEntity(giant)?.has<com.wingedsheep.engine.state.components.battlefield.TappedComponent>() shouldBe false
                }
                withClue("Hill Giant gains haste") {
                    game.state.projectedState.hasKeyword(giant, Keyword.HASTE) shouldBe true
                }
                withClue("a Blood token is created") {
                    game.findPermanents("Blood").size shouldBe 1
                }
            }
        }
    }
}

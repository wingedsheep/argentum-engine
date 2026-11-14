package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Claustrophobia (ISD #48) — {1}{U}{U} Enchantment — Aura.
 *
 *   Enchant creature
 *   When this Aura enters, tap enchanted creature.
 *   Enchanted creature doesn't untap during its controller's untap step.
 */
class ClaustrophobiaScenarioTest : ScenarioTestBase() {

    private fun TestGame.isTapped(id: EntityId): Boolean = state.getEntity(id)!!.has<TappedComponent>()

    init {
        context("Claustrophobia") {

            test("taps the enchanted creature, which then stays tapped through its controller's untap step") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Claustrophobia")
                    .withLandsOnBattlefield(1, "Island", 3)
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Hill Giant", tapped = true)
                    .apply {
                        repeat(6) {
                            withCardInLibrary(1, "Island")
                            withCardInLibrary(2, "Island")
                        }
                    }
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                val giant = game.findPermanent("Hill Giant")!!

                val cast = game.castSpell(1, "Claustrophobia", bears)
                withClue("cast should succeed: ${cast.error}") { cast.error shouldBe null }
                game.resolveStack()

                game.isOnBattlefield("Claustrophobia") shouldBe true
                withClue("the ETB tapped the enchanted creature") { game.isTapped(bears) shouldBe true }
                withClue("the enchanted creature has the doesn't-untap flag") {
                    game.state.projectedState.hasKeyword(bears, AbilityFlag.DOESNT_UNTAP) shouldBe true
                }

                // Move to the opponent's turn: their untap step has passed by their main phase.
                game.passUntilPhase(Phase.ENDING, Step.END)
                game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                game.state.activePlayerId shouldBe game.player2Id

                withClue("the opponent's other tapped creature untapped normally") { game.isTapped(giant) shouldBe false }
                withClue("the enchanted creature stayed tapped") { game.isTapped(bears) shouldBe true }
            }
        }
    }
}

package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Sudden Disappearance (DKA #23) — "Exile all nonland permanents target player controls. Return the
 * exiled cards to the battlefield under their owner's control at the beginning of the next end step."
 *
 * Only the targeted player's nonland permanents leave; lands and the caster's board stay. The pile
 * comes back at the end step under its owner's control.
 */
class SuddenDisappearanceScenarioTest : ScenarioTestBase() {

    init {
        context("Sudden Disappearance") {

            test("exiles the target player's nonland permanents and returns them at the end step") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardInHand(1, "Sudden Disappearance")
                    .withLandsOnBattlefield(1, "Plains", 6)
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withCardOnBattlefield(2, "Ornithopter")
                    .withCardOnBattlefield(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpellTargetingPlayer(1, "Sudden Disappearance", 2).error shouldBe null
                game.resolveStack()

                withClue("Bob's nonland permanents are exiled") {
                    game.isInExile(2, "Hill Giant") shouldBe true
                    game.isInExile(2, "Ornithopter") shouldBe true
                }
                withClue("Bob's land and Alice's creature stay") {
                    game.isOnBattlefield("Forest") shouldBe true
                    game.isOnBattlefield("Grizzly Bears") shouldBe true
                }

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()

                withClue("the exiled cards return under their owner's control") {
                    val giant = game.findPermanent("Hill Giant")!!
                    val thopter = game.findPermanent("Ornithopter")!!
                    game.state.projectedState.getController(giant) shouldBe game.player2Id
                    game.state.projectedState.getController(thopter) shouldBe game.player2Id
                    game.isInExile(2, "Hill Giant") shouldBe false
                }
            }
        }
    }
}

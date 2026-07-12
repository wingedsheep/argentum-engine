package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Belligerent Guest (VOW #301) — {2}{R} Creature — Vampire, 3/2, Trample.
 *
 *   Whenever this creature deals combat damage to a player, create a Blood token.
 *
 * Exercises the printed Trample keyword and the combat-damage-to-player trigger creating a
 * Blood token when the attack connects unblocked.
 */
class BelligerentGuestScenarioTest : ScenarioTestBase() {

    init {
        context("Belligerent Guest") {

            test("has printed trample") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Belligerent Guest", summoningSickness = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val guest = game.findPermanent("Belligerent Guest")!!

                withClue("Belligerent Guest has trample") {
                    game.state.projectedState.hasKeyword(guest, Keyword.TRAMPLE) shouldBe true
                }
            }

            test("dealing combat damage to a player creates a Blood token") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Belligerent Guest", summoningSickness = false)
                    .withLifeTotal(2, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Belligerent Guest" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.END_COMBAT)
                game.resolveStack()

                withClue("the opponent takes 3 combat damage (20 -> 17)") {
                    game.getLifeTotal(2) shouldBe 17
                }
                withClue("a Blood token was created") {
                    game.findPermanents("Blood").size shouldBe 1
                }
            }
        }
    }
}

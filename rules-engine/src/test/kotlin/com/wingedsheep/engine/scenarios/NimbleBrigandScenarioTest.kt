package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Nimble Brigand (OTJ #58) — {2}{U} Creature — Human Rogue 1/3.
 *
 *   "This creature can't be blocked if you've committed a crime this turn.
 *    Whenever this creature deals combat damage to a player, draw a card."
 *
 * The conditional evasion is a [com.wingedsheep.sdk.scripting.ConditionalStaticAbility] wrapping
 * [com.wingedsheep.sdk.scripting.CantBeBlocked] gated on `YouCommittedCrimeThisTurn`. Verifies the
 * CANT_BE_BLOCKED flag is absent before a crime and present once the controller commits one. The
 * combat-damage draw uses the standard `DealsCombatDamageToPlayer` trigger + `DrawCards(1)`.
 */
class NimbleBrigandScenarioTest : ScenarioTestBase() {

    private val stateProjector = StateProjector()

    init {
        context("Nimble Brigand") {

            test("is blockable until you've committed a crime this turn") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Nimble Brigand")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val brigand = game.findPermanent("Nimble Brigand")!!

                // No crime yet -> can still be blocked.
                var projected = stateProjector.project(game.state)
                projected.hasKeyword(brigand, AbilityFlag.CANT_BE_BLOCKED) shouldBe false

                // Mark that the controller committed a crime this turn.
                game.state = game.state.copy(
                    playersWhoCommittedCrimeThisTurn = setOf(game.player1Id)
                )

                projected = stateProjector.project(game.state)
                projected.hasKeyword(brigand, AbilityFlag.CANT_BE_BLOCKED) shouldBe true
            }

            test("an opponent's crime does not make it unblockable") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Nimble Brigand")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val brigand = game.findPermanent("Nimble Brigand")!!

                game.state = game.state.copy(
                    playersWhoCommittedCrimeThisTurn = setOf(game.player2Id)
                )

                val projected = stateProjector.project(game.state)
                projected.hasKeyword(brigand, AbilityFlag.CANT_BE_BLOCKED) shouldBe false
            }
        }
    }
}

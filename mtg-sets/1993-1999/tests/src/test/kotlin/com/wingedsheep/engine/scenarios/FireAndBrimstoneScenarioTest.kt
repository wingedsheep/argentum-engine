package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Fire and Brimstone — "deals 4 damage to target player who attacked this turn
 * and 4 damage to you."
 *
 * "Target player who attacked this turn" is a restriction on the candidate, so the two cases that
 * matter are a player who did attack (legal, takes 4) and one who didn't (no legal target at all).
 * Without the restriction the second cast would sail through — which is the whole point of putting
 * it on the target rather than gating the effect.
 */
class FireAndBrimstoneScenarioTest : ScenarioTestBase() {

    init {
        context("Fire and Brimstone") {

            test("burns a player who attacked this turn, and its caster") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Fire and Brimstone")
                    .withLandsOnBattlefield(1, "Plains", 5)
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withActivePlayer(2)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                game.declareAttackers(mapOf("Grizzly Bears" to 1)).error shouldBe null
                // The attacking player holds priority after declaring; pass it so I can respond.
                game.passPriority()

                game.castSpellTargetingPlayer(1, "Fire and Brimstone", 2).error shouldBe null
                game.resolveStack()

                withClue("4 to the attacker") { game.getLifeTotal(2) shouldBe 16 }
                withClue("and 4 to me, unconditionally") { game.getLifeTotal(1) shouldBe 16 }
            }

            test("a player who has not attacked is not a legal target") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Fire and Brimstone")
                    .withLandsOnBattlefield(1, "Plains", 5)
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withActivePlayer(2)
                    .withPriorityPlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                withClue("nobody has declared an attacker this turn") {
                    game.castSpellTargetingPlayer(1, "Fire and Brimstone", 2).error shouldNotBe null
                }
                withClue("and no damage happened either way") {
                    game.getLifeTotal(1) shouldBe 20
                    game.getLifeTotal(2) shouldBe 20
                }
            }
        }
    }
}

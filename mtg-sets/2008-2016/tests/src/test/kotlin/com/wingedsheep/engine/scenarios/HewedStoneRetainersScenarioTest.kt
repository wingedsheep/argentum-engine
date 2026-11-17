package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Hewed Stone Retainers (FRF #161) — {3} Artifact Creature — Golem 4/4.
 *
 *   Cast this spell only if you've cast another spell this turn.
 *
 * The restriction is a cast-time check against the caster's own cast history: it fails with no
 * spell cast yet (Retainers' own cast doesn't satisfy "another"), passes once the caster has cast
 * a spell, and an opponent's spell doesn't count.
 */
class HewedStoneRetainersScenarioTest : ScenarioTestBase() {

    init {
        context("Hewed Stone Retainers") {

            test("can't be cast when you haven't cast another spell this turn") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Hewed Stone Retainers")
                    .withLandsOnBattlefield(1, "Mountain", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cast = game.castSpell(1, "Hewed Stone Retainers")
                withClue("the cast should be rejected") { cast.error shouldNotBe null }
                game.isInHand(1, "Hewed Stone Retainers") shouldBe true
            }

            test("can be cast after you've cast another spell this turn") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Lightning Bolt")
                    .withCardInHand(1, "Hewed Stone Retainers")
                    .withLandsOnBattlefield(1, "Mountain", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bolt = game.castSpellTargetingPlayer(1, "Lightning Bolt", 2)
                withClue("Lightning Bolt should cast: ${bolt.error}") { bolt.error shouldBe null }
                game.resolveStack()

                val cast = game.castSpell(1, "Hewed Stone Retainers")
                withClue("Retainers should cast: ${cast.error}") { cast.error shouldBe null }
                game.resolveStack()

                game.findPermanent("Hewed Stone Retainers") shouldNotBe null
            }

            test("an opponent's spell doesn't count") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Hewed Stone Retainers")
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    .withCardInHand(2, "Lightning Bolt")
                    .withLandsOnBattlefield(2, "Mountain", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Player 1 passes priority; Player 2 bolts Player 1 at instant speed in Player 1's main phase.
                game.passPriority()
                val bolt = game.castSpellTargetingPlayer(2, "Lightning Bolt", 1)
                withClue("Lightning Bolt should cast: ${bolt.error}") { bolt.error shouldBe null }
                game.resolveStack()

                val cast = game.castSpell(1, "Hewed Stone Retainers")
                withClue("only the opponent has cast a spell, so the cast should be rejected") {
                    cast.error shouldNotBe null
                }
                game.isInHand(1, "Hewed Stone Retainers") shouldBe true
            }
        }
    }
}

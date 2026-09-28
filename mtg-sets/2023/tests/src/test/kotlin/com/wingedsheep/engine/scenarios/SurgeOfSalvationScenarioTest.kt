package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Surge of Salvation — you and permanents you control gain hexproof until end of turn; prevent all
 * damage black and/or red sources would deal to creatures you control this turn.
 */
class SurgeOfSalvationScenarioTest : ScenarioTestBase() {

    init {
        context("Surge of Salvation") {
            test("grants hexproof to your permanents and prevents red damage to your creatures, not to you") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Surge of Salvation")
                    .withCardInHand(1, "Lightning Bolt")
                    .withCardInHand(1, "Lightning Bolt")
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withCardOnBattlefield(2, "Hill Giant", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cast = game.castSpell(1, "Surge of Salvation")
                withClue("Surge should cast: ${cast.error}") { cast.error shouldBe null }
                game.resolveStack()

                val bears = game.findPermanent("Grizzly Bears")!!
                val giant = game.findPermanent("Hill Giant")!!
                val projected = game.state.projectedState
                withClue("Your permanents gain hexproof") { projected.hasKeyword(bears, Keyword.HEXPROOF) shouldBe true }
                withClue("Opponent's permanents don't") { projected.hasKeyword(giant, Keyword.HEXPROOF) shouldBe false }

                // Red damage to your own creature is prevented (hexproof doesn't stop your own targeting).
                val bolt = game.castSpell(1, "Lightning Bolt", bears)
                withClue("Bolt at own Bears should cast: ${bolt.error}") { bolt.error shouldBe null }
                game.resolveStack()
                withClue("Red damage to a creature you control is prevented") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe true
                }

                // Damage to you is not covered by the shield.
                val face = game.castSpellTargetingPlayer(1, "Lightning Bolt", 1)
                withClue("Bolt at yourself should cast: ${face.error}") { face.error shouldBe null }
                game.resolveStack()
                withClue("The shield covers creatures only, not you") { game.getLifeTotal(1) shouldBe 17 }
            }

            test("damage from a non-black, non-red source is not prevented") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Surge of Salvation")
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withCardOnBattlefield(2, "Craw Wurm", summoningSickness = false) // green 6/4
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cast = game.castSpell(1, "Surge of Salvation")
                withClue("Surge should cast: ${cast.error}") { cast.error shouldBe null }
                game.resolveStack()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                val attack = game.declareAttackers(mapOf("Grizzly Bears" to 2))
                withClue("Attack should succeed: ${attack.error}") { attack.error shouldBe null }
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareBlockers(mapOf("Craw Wurm" to listOf("Grizzly Bears")))
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                withClue("A green source's damage is not prevented; Bears die to Craw Wurm") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe false
                }
            }
        }
    }
}

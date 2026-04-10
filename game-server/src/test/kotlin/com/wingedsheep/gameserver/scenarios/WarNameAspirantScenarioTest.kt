package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.*
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContainIgnoringCase

/**
 * Scenario tests for War-Name Aspirant's CantBeBlockedBy blocking restriction.
 *
 * War-Name Aspirant: {1}{R} 2/1 Creature — Human Warrior
 * "Raid — This creature enters with a +1/+1 counter on it if you attacked this turn."
 * "This creature can't be blocked by creatures with power 1 or less."
 */
class WarNameAspirantScenarioTest : ScenarioTestBase() {

    init {
        context("CantBeBlockedBy blocking restriction") {

            test("cannot be blocked by creature with power 1") {
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "War-Name Aspirant")
                    .withCardOnBattlefield(2, "Devoted Hero")  // 1/2, power 1
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val aspirantId = game.findPermanent("War-Name Aspirant")!!
                val heroId = game.findPermanent("Devoted Hero")!!

                val attackResult = game.execute(
                    DeclareAttackers(game.player1Id, mapOf(aspirantId to game.player2Id))
                )
                withClue("Attack should succeed: ${attackResult.error}") {
                    attackResult.error shouldBe null
                }

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                val blockResult = game.execute(
                    DeclareBlockers(game.player2Id, mapOf(heroId to listOf(aspirantId)))
                )

                withClue("Block should fail - Devoted Hero has power 1 (power <= 1)") {
                    blockResult.error shouldNotBe null
                    blockResult.error!! shouldContainIgnoringCase "cannot block"
                }
            }

            test("CAN be blocked by creature with power 2") {
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "War-Name Aspirant")
                    .withCardOnBattlefield(2, "Grizzly Bears")  // 2/2, power 2
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val aspirantId = game.findPermanent("War-Name Aspirant")!!
                val bearsId = game.findPermanent("Grizzly Bears")!!

                val attackResult = game.execute(
                    DeclareAttackers(game.player1Id, mapOf(aspirantId to game.player2Id))
                )
                withClue("Attack should succeed: ${attackResult.error}") {
                    attackResult.error shouldBe null
                }

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                val blockResult = game.execute(
                    DeclareBlockers(game.player2Id, mapOf(bearsId to listOf(aspirantId)))
                )

                withClue("Block should succeed - Grizzly Bears has power 2 (power > 1): ${blockResult.error}") {
                    blockResult.error shouldBe null
                }
            }

            test("cannot be blocked by creature with power 0") {
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "War-Name Aspirant")
                    .withCardOnBattlefield(2, "Archers' Parapet")  // 0/5, power 0
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val aspirantId = game.findPermanent("War-Name Aspirant")!!
                val wallId = game.findPermanent("Archers' Parapet")!!

                val attackResult = game.execute(
                    DeclareAttackers(game.player1Id, mapOf(aspirantId to game.player2Id))
                )
                withClue("Attack should succeed: ${attackResult.error}") {
                    attackResult.error shouldBe null
                }

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                val blockResult = game.execute(
                    DeclareBlockers(game.player2Id, mapOf(wallId to listOf(aspirantId)))
                )

                withClue("Block should fail - Archers' Parapet has power 0 (power <= 1)") {
                    blockResult.error shouldNotBe null
                    blockResult.error!! shouldContainIgnoringCase "cannot block"
                }
            }
        }
    }
}

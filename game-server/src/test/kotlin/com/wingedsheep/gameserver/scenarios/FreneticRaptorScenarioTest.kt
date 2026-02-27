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
 * Scenario tests for Frenetic Raptor's "Beasts can't block" static ability.
 *
 * Frenetic Raptor: {5}{R} 6/6 Creature — Dinosaur Beast
 * "Beasts can't block."
 *
 * This is a global effect that prevents ALL Beasts from blocking, not just Frenetic Raptor itself.
 */
class FreneticRaptorScenarioTest : ScenarioTestBase() {

    init {
        context("Frenetic Raptor - Beasts can't block") {

            test("Frenetic Raptor itself cannot block (it is a Beast)") {
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Devoted Hero")       // 1/1 attacker
                    .withCardOnBattlefield(2, "Frenetic Raptor")    // 6/6 Beast - can't block
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val heroId = game.findPermanent("Devoted Hero")!!
                val raptorId = game.findPermanent("Frenetic Raptor")!!

                val attackResult = game.execute(
                    DeclareAttackers(game.player1Id, mapOf(heroId to game.player2Id))
                )
                withClue("Attack should succeed: ${attackResult.error}") {
                    attackResult.error shouldBe null
                }

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                val blockResult = game.execute(
                    DeclareBlockers(game.player2Id, mapOf(raptorId to listOf(heroId)))
                )

                withClue("Block should fail - Frenetic Raptor is a Beast and can't block") {
                    blockResult.error shouldNotBe null
                    blockResult.error!! shouldContainIgnoringCase "can't block"
                }
            }

            test("other Beast creatures also cannot block while Frenetic Raptor is on the battlefield") {
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Devoted Hero")       // 1/1 attacker
                    .withCardOnBattlefield(2, "Frenetic Raptor")    // 6/6 Beast - prevents Beasts from blocking
                    .withCardOnBattlefield(2, "Enormous Baloth")    // 7/7 Beast - can't block
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val heroId = game.findPermanent("Devoted Hero")!!
                val balothId = game.findPermanent("Enormous Baloth")!!

                val attackResult = game.execute(
                    DeclareAttackers(game.player1Id, mapOf(heroId to game.player2Id))
                )
                withClue("Attack should succeed: ${attackResult.error}") {
                    attackResult.error shouldBe null
                }

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                val blockResult = game.execute(
                    DeclareBlockers(game.player2Id, mapOf(balothId to listOf(heroId)))
                )

                withClue("Block should fail - Enormous Baloth is a Beast and can't block") {
                    blockResult.error shouldNotBe null
                    blockResult.error!! shouldContainIgnoringCase "can't block"
                }
            }

            test("non-Beast creatures CAN still block while Frenetic Raptor is on the battlefield") {
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Devoted Hero")       // 1/1 attacker
                    .withCardOnBattlefield(2, "Frenetic Raptor")    // 6/6 Beast - prevents Beasts from blocking
                    .withCardOnBattlefield(2, "Glory Seeker")       // 2/2 Human Soldier - NOT a Beast
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val heroId = game.findPermanent("Devoted Hero")!!
                val seekerId = game.findPermanent("Glory Seeker")!!

                val attackResult = game.execute(
                    DeclareAttackers(game.player1Id, mapOf(heroId to game.player2Id))
                )
                withClue("Attack should succeed: ${attackResult.error}") {
                    attackResult.error shouldBe null
                }

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                val blockResult = game.execute(
                    DeclareBlockers(game.player2Id, mapOf(seekerId to listOf(heroId)))
                )

                withClue("Block should succeed - Glory Seeker is not a Beast: ${blockResult.error}") {
                    blockResult.error shouldBe null
                }
            }

            test("Frenetic Raptor CAN attack normally") {
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Frenetic Raptor")    // 6/6 attacker
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val raptorId = game.findPermanent("Frenetic Raptor")!!

                val attackResult = game.execute(
                    DeclareAttackers(game.player1Id, mapOf(raptorId to game.player2Id))
                )
                withClue("Frenetic Raptor should be able to attack: ${attackResult.error}") {
                    attackResult.error shouldBe null
                }
            }
        }
    }
}

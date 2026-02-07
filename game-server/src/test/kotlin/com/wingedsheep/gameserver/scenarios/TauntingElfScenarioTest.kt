package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.DeclareAttackers
import com.wingedsheep.engine.core.DeclareBlockers
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain

/**
 * Scenario tests for Taunting Elf's lure ability.
 *
 * Card reference:
 * - Taunting Elf (G): 0/1 Creature - Elf
 *   "All creatures able to block Taunting Elf do so."
 */
class TauntingElfScenarioTest : ScenarioTestBase() {

    init {
        context("Taunting Elf lure effect") {

            test("all opponent creatures must block Taunting Elf when it attacks") {
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Taunting Elf")   // 0/1 attacker with lure
                    .withCardOnBattlefield(2, "Devoted Hero")   // 2/1 blocker
                    .withCardOnBattlefield(2, "Devoted Hero")   // 2/1 blocker
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                // Declare Taunting Elf as attacker
                val tauntingElfId = game.findPermanent("Taunting Elf")!!
                val attackResult = game.execute(
                    DeclareAttackers(
                        game.player1Id,
                        mapOf(tauntingElfId to game.player2Id)
                    )
                )
                withClue("Attackers should be declared successfully: ${attackResult.error}") {
                    attackResult.error shouldBe null
                }

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                // Try to declare no blockers — should FAIL
                val noBlockResult = game.declareNoBlockers()
                withClue("Declaring no blockers should fail when creatures must block Taunting Elf") {
                    noBlockResult.error shouldNotBe null
                    noBlockResult.error!! shouldContain "must block"
                }

                // Try to declare only one blocker — should also FAIL
                val oneBlockerResult = game.declareBlockers(mapOf(
                    "Devoted Hero" to listOf("Taunting Elf")
                ))
                withClue("Declaring only one blocker should fail when both must block") {
                    oneBlockerResult.error shouldNotBe null
                    oneBlockerResult.error!! shouldContain "must block"
                }
            }

            test("both blockers blocking Taunting Elf is valid") {
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Taunting Elf")
                    .withCardOnBattlefield(2, "Devoted Hero")
                    .withCardOnBattlefield(2, "Devoted Hero")
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val tauntingElfId = game.findPermanent("Taunting Elf")!!
                game.execute(
                    DeclareAttackers(
                        game.player1Id,
                        mapOf(tauntingElfId to game.player2Id)
                    )
                )
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                // Both blockers blocking Taunting Elf — should succeed
                val vanguardIds = game.findAllPermanents("Devoted Hero")
                val blockResult = game.execute(
                    DeclareBlockers(
                        game.player2Id,
                        mapOf(
                            vanguardIds[0] to listOf(tauntingElfId),
                            vanguardIds[1] to listOf(tauntingElfId)
                        )
                    )
                )
                withClue("Both creatures blocking Taunting Elf should be valid: ${blockResult.error}") {
                    blockResult.error shouldBe null
                }
            }

            test("flying creature is not forced to block non-flying Taunting Elf") {
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Taunting Elf")   // 0/1 no flying
                    .withCardOnBattlefield(2, "Cloud Spirit")   // 3/1 flying
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val tauntingElfId = game.findPermanent("Taunting Elf")!!
                game.execute(
                    DeclareAttackers(
                        game.player1Id,
                        mapOf(tauntingElfId to game.player2Id)
                    )
                )
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                // Cloud Spirit can't block a non-flying creature, so no blockers is valid
                val noBlockResult = game.declareNoBlockers()
                withClue("No blockers should be valid when blocker can't block non-flyer: ${noBlockResult.error}") {
                    noBlockResult.error shouldBe null
                }
            }

            test("Taunting Elf plus another attacker — opponent must block Taunting Elf") {
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Taunting Elf")   // 0/1 lure
                    .withCardOnBattlefield(1, "Grizzly Bears")  // 2/2 regular attacker
                    .withCardOnBattlefield(2, "Hill Giant")     // 3/3 blocker
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val tauntingElfId = game.findPermanent("Taunting Elf")!!
                val bearsId = game.findPermanent("Grizzly Bears")!!
                val giantId = game.findPermanent("Hill Giant")!!

                // Declare both as attackers
                game.execute(
                    DeclareAttackers(
                        game.player1Id,
                        mapOf(
                            tauntingElfId to game.player2Id,
                            bearsId to game.player2Id
                        )
                    )
                )
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                // Hill Giant blocking Grizzly Bears instead of Taunting Elf — should FAIL
                val wrongBlockResult = game.execute(
                    DeclareBlockers(
                        game.player2Id,
                        mapOf(giantId to listOf(bearsId))
                    )
                )
                withClue("Blocking the non-lure creature should fail") {
                    wrongBlockResult.error shouldNotBe null
                    wrongBlockResult.error!! shouldContain "must block"
                }

                // Hill Giant blocking Taunting Elf — should succeed (Grizzly Bears gets through)
                val correctBlockResult = game.execute(
                    DeclareBlockers(
                        game.player2Id,
                        mapOf(giantId to listOf(tauntingElfId))
                    )
                )
                withClue("Blocking Taunting Elf should succeed: ${correctBlockResult.error}") {
                    correctBlockResult.error shouldBe null
                }
            }
        }
    }
}

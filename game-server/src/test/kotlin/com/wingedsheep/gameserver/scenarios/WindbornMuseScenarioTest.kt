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
 * Scenario tests for Windborn Muse's attack tax ability.
 *
 * Windborn Muse: {3}{W} 2/3 Creature — Spirit
 * Flying
 * Creatures can't attack you unless their controller pays {2} for each creature
 * they control that's attacking you.
 */
class WindbornMuseScenarioTest : ScenarioTestBase() {

    init {
        context("Windborn Muse attack tax") {

            test("attack succeeds when attacker can pay the tax") {
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Hill Giant")
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withCardOnBattlefield(2, "Windborn Muse")
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val hillGiantId = game.findPermanent("Hill Giant")!!

                val attackResult = game.execute(
                    DeclareAttackers(game.player1Id, mapOf(hillGiantId to game.player2Id))
                )

                withClue("Attack should succeed - attacker has enough mana: ${attackResult.error}") {
                    attackResult.error shouldBe null
                }
            }

            test("attack fails when attacker cannot pay the tax") {
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Hill Giant")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withCardOnBattlefield(2, "Windborn Muse")
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val hillGiantId = game.findPermanent("Hill Giant")!!

                val attackResult = game.execute(
                    DeclareAttackers(game.player1Id, mapOf(hillGiantId to game.player2Id))
                )

                withClue("Attack should fail - not enough mana to pay tax") {
                    attackResult.error shouldNotBe null
                    attackResult.error!! shouldContainIgnoringCase "attack tax"
                }
            }

            test("attack fails when attacker has no mana at all") {
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Hill Giant")
                    .withCardOnBattlefield(2, "Windborn Muse")
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val hillGiantId = game.findPermanent("Hill Giant")!!

                val attackResult = game.execute(
                    DeclareAttackers(game.player1Id, mapOf(hillGiantId to game.player2Id))
                )

                withClue("Attack should fail - no mana available") {
                    attackResult.error shouldNotBe null
                    attackResult.error!! shouldContainIgnoringCase "attack tax"
                }
            }

            test("multiple attackers require tax per attacker") {
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Hill Giant")
                    .withCardOnBattlefield(1, "Raging Goblin")
                    .withLandsOnBattlefield(1, "Mountain", 4)
                    .withCardOnBattlefield(2, "Windborn Muse")
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val hillGiantId = game.findPermanent("Hill Giant")!!
                val goblinId = game.findPermanent("Raging Goblin")!!

                val attackResult = game.execute(
                    DeclareAttackers(
                        game.player1Id,
                        mapOf(hillGiantId to game.player2Id, goblinId to game.player2Id)
                    )
                )

                withClue("Attack should succeed - has 4 mana for 2 attackers: ${attackResult.error}") {
                    attackResult.error shouldBe null
                }
            }

            test("multiple attackers fail when not enough mana for all") {
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Hill Giant")
                    .withCardOnBattlefield(1, "Raging Goblin")
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    .withCardOnBattlefield(2, "Windborn Muse")
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val hillGiantId = game.findPermanent("Hill Giant")!!
                val goblinId = game.findPermanent("Raging Goblin")!!

                val attackResult = game.execute(
                    DeclareAttackers(
                        game.player1Id,
                        mapOf(hillGiantId to game.player2Id, goblinId to game.player2Id)
                    )
                )

                withClue("Attack should fail - only 3 mana for 2 attackers needing {4}") {
                    attackResult.error shouldNotBe null
                    attackResult.error!! shouldContainIgnoringCase "attack tax"
                }
            }

            test("no tax required when attacking with zero creatures") {
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Hill Giant")
                    .withCardOnBattlefield(2, "Windborn Muse")
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val attackResult = game.execute(
                    DeclareAttackers(game.player1Id, emptyMap())
                )

                withClue("Empty attack declaration should succeed: ${attackResult.error}") {
                    attackResult.error shouldBe null
                }
            }

            test("no tax when Windborn Muse is not on the battlefield") {
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Hill Giant")
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val hillGiantId = game.findPermanent("Hill Giant")!!

                val attackResult = game.execute(
                    DeclareAttackers(game.player1Id, mapOf(hillGiantId to game.player2Id))
                )

                withClue("Attack should succeed - no Windborn Muse: ${attackResult.error}") {
                    attackResult.error shouldBe null
                }
            }
        }
    }
}

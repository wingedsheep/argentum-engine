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
 *
 * Per the engine's attack-tax UX: the attacker is prompted with a yes/no decision
 * before any mana is auto-tapped. `Yes` pays and the attack proceeds; `No` cancels
 * the attack declaration so the attacker can re-declare with different attackers.
 */
class WindbornMuseScenarioTest : ScenarioTestBase() {

    init {
        context("Windborn Muse attack tax") {

            test("attack succeeds when attacker confirms and can pay the tax") {
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Hill Giant")
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withCardOnBattlefield(2, "Windborn Muse")
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val hillGiantId = game.findPermanent("Hill Giant")!!

                val declared = game.execute(
                    DeclareAttackers(game.player1Id, mapOf(hillGiantId to game.player2Id))
                )

                withClue("Engine pauses for attack-tax confirmation before tapping mana") {
                    declared.isPaused shouldBe true
                }

                val paid = game.answerYesNo(true)
                withClue("Tax {2} payable from 2 untapped Mountains: ${paid.error}") {
                    paid.error shouldBe null
                }
            }

            test("attack fails after confirming when attacker cannot pay the tax") {
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Hill Giant")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withCardOnBattlefield(2, "Windborn Muse")
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val hillGiantId = game.findPermanent("Hill Giant")!!

                val declared = game.execute(
                    DeclareAttackers(game.player1Id, mapOf(hillGiantId to game.player2Id))
                )
                declared.isPaused shouldBe true

                val paid = game.answerYesNo(true)
                withClue("Only 1 Mountain available for {2} tax") {
                    paid.error shouldNotBe null
                    paid.error!! shouldContainIgnoringCase "attack tax"
                }
            }

            test("declining the tax cancels the attack") {
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Hill Giant")
                    .withLandsOnBattlefield(1, "Mountain", 5)
                    .withCardOnBattlefield(2, "Windborn Muse")
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val hillGiantId = game.findPermanent("Hill Giant")!!

                val declared = game.execute(
                    DeclareAttackers(game.player1Id, mapOf(hillGiantId to game.player2Id))
                )
                declared.isPaused shouldBe true

                val declined = game.answerYesNo(false)
                withClue("Decline is a clean no-op: no error, no pause, player stays in declare-attackers") {
                    declined.error shouldBe null
                    declined.isPaused shouldBe false
                }
                withClue("Attacker should not have committed to attacking") {
                    val attackerComponent = game.state.getEntity(hillGiantId)
                        ?.get<com.wingedsheep.engine.state.components.combat.AttackingComponent>()
                    attackerComponent shouldBe null
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

                val declared = game.execute(
                    DeclareAttackers(
                        game.player1Id,
                        mapOf(hillGiantId to game.player2Id, goblinId to game.player2Id)
                    )
                )
                declared.isPaused shouldBe true

                val paid = game.answerYesNo(true)
                withClue("4 Mountains cover the 2×{2} tax: ${paid.error}") {
                    paid.error shouldBe null
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

                val declared = game.execute(
                    DeclareAttackers(
                        game.player1Id,
                        mapOf(hillGiantId to game.player2Id, goblinId to game.player2Id)
                    )
                )
                declared.isPaused shouldBe true

                val paid = game.answerYesNo(true)
                withClue("Only 3 Mountains for {4} tax") {
                    paid.error shouldNotBe null
                    paid.error!! shouldContainIgnoringCase "attack tax"
                }
            }

            test("no tax required when attacking with zero creatures (no pause)") {
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

                withClue("No tax ⇒ no pause: ${attackResult.error}") {
                    attackResult.error shouldBe null
                    attackResult.isPaused shouldBe false
                }
            }

            test("no tax when Windborn Muse is not on the battlefield (no pause)") {
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

                withClue("No Muse ⇒ no pause: ${attackResult.error}") {
                    attackResult.error shouldBe null
                    attackResult.isPaused shouldBe false
                }
            }
        }
    }
}

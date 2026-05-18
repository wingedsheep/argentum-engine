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
 * Scenario tests for Collective Restraint's Domain-scaled attack tax.
 *
 * Collective Restraint: {3}{U} Enchantment (Invasion)
 * Domain — Creatures can't attack you unless their controller pays {X} for each
 * creature they control that's attacking you, where X is the number of basic land
 * types among lands you control.
 *
 * Exercises [com.wingedsheep.sdk.scripting.AttackTax] with a [DynamicAmount] amount,
 * with the source permanent's controller as "you" for the domain count. The attacker
 * is prompted before any mana is auto-tapped.
 */
class CollectiveRestraintScenarioTest : ScenarioTestBase() {

    init {
        context("Collective Restraint domain-scaled attack tax") {

            test("zero basic land types — no tax, attack succeeds without a prompt") {
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Hill Giant")
                    .withCardOnBattlefield(2, "Collective Restraint")
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val hillGiantId = game.findPermanent("Hill Giant")!!

                val attackResult = game.execute(
                    DeclareAttackers(game.player1Id, mapOf(hillGiantId to game.player2Id))
                )

                withClue("Defender has no lands ⇒ X = 0 ⇒ no pause: ${attackResult.error}") {
                    attackResult.error shouldBe null
                    attackResult.isPaused shouldBe false
                }
            }

            test("one basic type controlled by defender — tax is {1} per attacker") {
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Hill Giant")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withCardOnBattlefield(2, "Collective Restraint")
                    .withLandsOnBattlefield(2, "Island", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val hillGiantId = game.findPermanent("Hill Giant")!!

                val declared = game.execute(
                    DeclareAttackers(game.player1Id, mapOf(hillGiantId to game.player2Id))
                )
                declared.isPaused shouldBe true

                val paid = game.answerYesNo(true)
                withClue("Mountain pays the {1} tax: ${paid.error}") {
                    paid.error shouldBe null
                }
            }

            test("three basic types — tax is {3}, declining cancels the attack") {
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Hill Giant")
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    .withCardOnBattlefield(2, "Collective Restraint")
                    .withLandsOnBattlefield(2, "Plains", 1)
                    .withLandsOnBattlefield(2, "Island", 1)
                    .withLandsOnBattlefield(2, "Swamp", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val hillGiantId = game.findPermanent("Hill Giant")!!

                val declared = game.execute(
                    DeclareAttackers(game.player1Id, mapOf(hillGiantId to game.player2Id))
                )
                declared.isPaused shouldBe true

                val declined = game.answerYesNo(false)
                withClue("Decline is a clean no-op (no error banner): ${declined.error}") {
                    declined.error shouldBe null
                    declined.isPaused shouldBe false
                }
                withClue("Hill Giant should not be attacking after decline") {
                    val attackerComponent = game.state.getEntity(hillGiantId)
                        ?.get<com.wingedsheep.engine.state.components.combat.AttackingComponent>()
                    attackerComponent shouldBe null
                }
            }

            test("three basic types — tax is {3}, insufficient mana fails after confirmation") {
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Hill Giant")
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withCardOnBattlefield(2, "Collective Restraint")
                    .withLandsOnBattlefield(2, "Plains", 1)
                    .withLandsOnBattlefield(2, "Island", 1)
                    .withLandsOnBattlefield(2, "Swamp", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val hillGiantId = game.findPermanent("Hill Giant")!!

                val declared = game.execute(
                    DeclareAttackers(game.player1Id, mapOf(hillGiantId to game.player2Id))
                )
                declared.isPaused shouldBe true

                val paid = game.answerYesNo(true)
                withClue("Only {2} available for {3} tax") {
                    paid.error shouldNotBe null
                    paid.error!! shouldContainIgnoringCase "attack tax"
                }
            }

            test("three basic types — tax is {3}, attacker pays and attack succeeds") {
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Hill Giant")
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    .withCardOnBattlefield(2, "Collective Restraint")
                    .withLandsOnBattlefield(2, "Plains", 1)
                    .withLandsOnBattlefield(2, "Island", 1)
                    .withLandsOnBattlefield(2, "Swamp", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val hillGiantId = game.findPermanent("Hill Giant")!!

                val declared = game.execute(
                    DeclareAttackers(game.player1Id, mapOf(hillGiantId to game.player2Id))
                )
                declared.isPaused shouldBe true

                val paid = game.answerYesNo(true)
                withClue("3 Mountains for the {3} tax: ${paid.error}") {
                    paid.error shouldBe null
                }
            }

            test("five basic types — tax is {5} per attacker, two attackers need {10}") {
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Hill Giant")
                    .withCardOnBattlefield(1, "Raging Goblin")
                    .withLandsOnBattlefield(1, "Mountain", 10)
                    .withCardOnBattlefield(2, "Collective Restraint")
                    .withLandsOnBattlefield(2, "Plains", 1)
                    .withLandsOnBattlefield(2, "Island", 1)
                    .withLandsOnBattlefield(2, "Swamp", 1)
                    .withLandsOnBattlefield(2, "Mountain", 1)
                    .withLandsOnBattlefield(2, "Forest", 1)
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
                withClue("10 Mountains pay 2×{5} tax: ${paid.error}") {
                    paid.error shouldBe null
                }
            }

            test("five basic types — two attackers with only {9} fails after confirmation") {
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Hill Giant")
                    .withCardOnBattlefield(1, "Raging Goblin")
                    .withLandsOnBattlefield(1, "Mountain", 9)
                    .withCardOnBattlefield(2, "Collective Restraint")
                    .withLandsOnBattlefield(2, "Plains", 1)
                    .withLandsOnBattlefield(2, "Island", 1)
                    .withLandsOnBattlefield(2, "Swamp", 1)
                    .withLandsOnBattlefield(2, "Mountain", 1)
                    .withLandsOnBattlefield(2, "Forest", 1)
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
                withClue("Only {9} available for {10} tax") {
                    paid.error shouldNotBe null
                    paid.error!! shouldContainIgnoringCase "attack tax"
                }
            }

            test("attacker's own basic land types don't count toward defender's domain") {
                // Defender has 1 basic type, attacker has 5 — tax should be {1}, not {5}
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Hill Giant")
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withCardOnBattlefield(2, "Collective Restraint")
                    .withLandsOnBattlefield(2, "Island", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val hillGiantId = game.findPermanent("Hill Giant")!!

                val declared = game.execute(
                    DeclareAttackers(game.player1Id, mapOf(hillGiantId to game.player2Id))
                )
                declared.isPaused shouldBe true

                val paid = game.answerYesNo(true)
                withClue("Domain reads defender's lands, not attacker's: ${paid.error}") {
                    paid.error shouldBe null
                }
            }
        }
    }
}

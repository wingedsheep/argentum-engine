package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.DeclareAttackers
import com.wingedsheep.engine.core.PassPriority
import com.wingedsheep.engine.core.TurnFaceUp
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Hystrodon.
 *
 * Card reference:
 * - Hystrodon ({4}{G}): Creature — Beast, 3/4
 *   Trample
 *   "Whenever Hystrodon deals combat damage to a player, you may draw a card."
 *   Morph {1}{G}{G}
 */
class HystrodonScenarioTest : ScenarioTestBase() {

    init {
        context("Hystrodon combat damage trigger") {

            test("may draw a card when dealing combat damage to a player") {
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Hystrodon")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val initialHandSize = game.handSize(1)

                // Combat sequence
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Hystrodon" to 2))
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareNoBlockers()

                // Advance until the may decision appears
                var iterations = 0
                while (!game.hasPendingDecision() && iterations < 50) {
                    val p = game.state.priorityPlayerId ?: break
                    game.execute(PassPriority(p))
                    iterations++
                }

                withClue("Should have pending may decision") {
                    game.hasPendingDecision() shouldBe true
                }

                // Choose yes to draw
                game.answerYesNo(true)

                withClue("Should have drawn 1 card") {
                    game.handSize(1) shouldBe initialHandSize + 1
                }

                withClue("Opponent should have taken 3 combat damage") {
                    game.getLifeTotal(2) shouldBe 17
                }
            }

            test("may decline to draw") {
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Hystrodon")
                    .withCardInLibrary(1, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val initialHandSize = game.handSize(1)

                // Combat sequence
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Hystrodon" to 2))
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareNoBlockers()

                // Advance until the may decision appears
                var iterations = 0
                while (!game.hasPendingDecision() && iterations < 50) {
                    val p = game.state.priorityPlayerId ?: break
                    game.execute(PassPriority(p))
                    iterations++
                }

                // Choose no — decline to draw
                game.answerYesNo(false)

                withClue("Should not have drawn a card") {
                    game.handSize(1) shouldBe initialHandSize
                }

                withClue("Opponent should have taken 3 combat damage") {
                    game.getLifeTotal(2) shouldBe 17
                }
            }

            test("does not trigger when blocked") {
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Hystrodon")      // 3/4 trample
                    .withCardOnBattlefield(2, "Towering Baloth") // 7/6 blocker
                    .withCardInLibrary(1, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val initialHandSize = game.handSize(1)

                // Combat sequence
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Hystrodon" to 2))
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareBlockers(mapOf("Towering Baloth" to listOf("Hystrodon")))

                // Advance through combat — Hystrodon (3/4) is blocked by Towering Baloth (7/6)
                // Hystrodon deals 3 to Towering Baloth, no trample damage goes through (3 < 6)
                // No combat damage to player, so trigger should not fire
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                withClue("Should not have drawn (no damage to player)") {
                    game.handSize(1) shouldBe initialHandSize
                }

                withClue("Hystrodon should be dead (took 7 damage with 4 toughness)") {
                    game.isOnBattlefield("Hystrodon") shouldBe false
                }

                withClue("Opponent should be at 20 life") {
                    game.getLifeTotal(2) shouldBe 20
                }
            }

            test("triggers with trample damage through blocker") {
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Hystrodon")     // 3/4 trample
                    .withCardOnBattlefield(2, "Elvish Pioneer") // 1/1 blocker
                    .withCardInLibrary(1, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val initialHandSize = game.handSize(1)

                // Combat sequence
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Hystrodon" to 2))
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareBlockers(mapOf("Elvish Pioneer" to listOf("Hystrodon")))

                // Hystrodon (3/4 trample) blocked by Elvish Pioneer (1/1)
                // 1 damage assigned to blocker, 2 tramples through to player
                // Trigger should fire since combat damage reached the player
                var iterations = 0
                while (!game.hasPendingDecision() && iterations < 50) {
                    val p = game.state.priorityPlayerId ?: break
                    game.execute(PassPriority(p))
                    iterations++
                }

                withClue("Should have pending may decision from trample damage trigger") {
                    game.hasPendingDecision() shouldBe true
                }

                game.answerYesNo(true)

                withClue("Should have drawn 1 card") {
                    game.handSize(1) shouldBe initialHandSize + 1
                }

                withClue("Opponent should have taken 2 trample damage") {
                    game.getLifeTotal(2) shouldBe 18
                }
            }
        }

        context("Hystrodon morph") {

            test("face-down Hystrodon does not trigger draw on combat damage (Rule 707.2)") {
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Hystrodon")
                    .withCardInLibrary(1, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Manually make Hystrodon face-down (simulates morph from a prior turn)
                val hystrodonId = game.findPermanent("Hystrodon")!!
                val container = game.state.getEntity(hystrodonId)!!.with(FaceDownComponent)
                game.state = game.state.withEntity(hystrodonId, container)

                val initialHandSize = game.handSize(1)

                // Attack with the face-down creature
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.execute(DeclareAttackers(game.player1Id, mapOf(hystrodonId to game.player2Id)))
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareNoBlockers()

                // Pass through combat damage — should NOT get a may decision
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                withClue("Should not have drawn (face-down creature has no abilities)") {
                    game.handSize(1) shouldBe initialHandSize
                }

                withClue("Opponent should have taken 2 combat damage (face-down is 2/2)") {
                    game.getLifeTotal(2) shouldBe 18
                }
            }

            test("can be cast face-down and turned face-up") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardInHand(1, "Hystrodon")
                    .withLandsOnBattlefield(1, "Forest", 6) // 3 for morph + 3 for unmorph {1}{G}{G}
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Find the card in hand and cast face-down
                val hand = game.state.getHand(game.player1Id)
                val hystrodonCardId = hand.find { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Hystrodon"
                }!!
                val castResult = game.execute(CastSpell(game.player1Id, hystrodonCardId, castFaceDown = true))
                withClue("Cast morph should succeed: ${castResult.error}") {
                    castResult.error shouldBe null
                }
                game.resolveStack()

                // Find the face-down creature on battlefield
                val faceDownId = game.state.getBattlefield().find { entityId ->
                    game.state.getEntity(entityId)?.has<FaceDownComponent>() == true
                }
                withClue("Should have a face-down creature on battlefield") {
                    (faceDownId != null) shouldBe true
                }

                // Turn face-up by paying {1}{G}{G}
                val turnUpResult = game.execute(TurnFaceUp(game.player1Id, faceDownId!!))
                withClue("Turn face up should succeed: ${turnUpResult.error}") {
                    turnUpResult.error shouldBe null
                }

                // Should now be a 3/4 Hystrodon
                withClue("Hystrodon should be face-up on battlefield") {
                    game.isOnBattlefield("Hystrodon") shouldBe true
                }
            }
        }
    }
}

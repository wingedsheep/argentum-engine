package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.DeclareAttackers
import com.wingedsheep.engine.core.DeclareBlockers
import com.wingedsheep.engine.core.PassPriority
import com.wingedsheep.engine.core.TurnFaceUp
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Frontline Strategist.
 *
 * Card reference:
 * - Frontline Strategist ({W}): Creature — Human Soldier 1/1
 *   Morph {W}
 *   When Frontline Strategist is turned face up, prevent all combat damage
 *   non-Soldier creatures would deal this turn.
 */
class FrontlineStrategistScenarioTest : ScenarioTestBase() {

    init {
        context("Frontline Strategist") {

            test("turning face up prevents combat damage from non-Soldier attacker") {
                val game = scenario()
                    .withPlayers("Defender", "Attacker")
                    .withCardInHand(1, "Frontline Strategist")
                    .withLandsOnBattlefield(1, "Plains", 4)
                    .withCardOnBattlefield(2, "Grizzly Bears") // 2/2 Bear - not a Soldier
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast Frontline Strategist face-down for {3}
                val cardId = game.state.getHand(game.player1Id).first { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Frontline Strategist"
                }
                val castResult = game.execute(CastSpell(game.player1Id, cardId, castFaceDown = true))
                withClue("Cast face-down should succeed: ${castResult.error}") {
                    castResult.error shouldBe null
                }
                game.resolveStack()

                val faceDownId = game.state.getBattlefield().find { entityId ->
                    game.state.getEntity(entityId)?.has<FaceDownComponent>() == true
                }
                withClue("Face-down creature should be on battlefield") {
                    faceDownId shouldNotBe null
                }

                // Advance to P2's attack step
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                // Now it's P2's turn — advance to P2's combat
                if (game.state.activePlayerId == game.player1Id) {
                    game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                }

                val bearsId = game.findPermanent("Grizzly Bears")!!
                game.execute(DeclareAttackers(game.player2Id, mapOf(bearsId to game.player1Id)))

                // Advance to declare blockers
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                // P1 declares no blockers
                game.execute(DeclareBlockers(game.player1Id, emptyMap()))

                // P1 turns Frontline Strategist face up before combat damage
                val turnUpResult = game.execute(TurnFaceUp(game.player1Id, faceDownId!!))
                withClue("Turn face-up should succeed: ${turnUpResult.error}") {
                    turnUpResult.error shouldBe null
                }

                // Triggered ability goes on stack - resolve it
                game.resolveStack()

                // Pass through to combat damage
                game.passUntilPhase(Phase.COMBAT, Step.END_COMBAT)

                // P1 should NOT have taken damage from non-Soldier Grizzly Bears
                val p1Life = game.getLifeTotal(1)
                withClue("Player 1 should not have taken combat damage from non-Soldier creature") {
                    p1Life shouldBe 20
                }
            }

            test("Soldier creatures still deal combat damage after face-up trigger") {
                val game = scenario()
                    .withPlayers("Defender", "Attacker")
                    .withCardInHand(1, "Frontline Strategist")
                    .withLandsOnBattlefield(1, "Plains", 4)
                    .withCardOnBattlefield(2, "Glory Seeker") // 2/2 Human Soldier
                    .withCardOnBattlefield(2, "Grizzly Bears") // 2/2 Bear
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast face-down
                val cardId = game.state.getHand(game.player1Id).first { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Frontline Strategist"
                }
                game.execute(CastSpell(game.player1Id, cardId, castFaceDown = true))
                game.resolveStack()

                val faceDownId = game.state.getBattlefield().find { entityId ->
                    game.state.getEntity(entityId)?.has<FaceDownComponent>() == true
                }!!

                // Advance to P2's combat
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                if (game.state.activePlayerId == game.player1Id) {
                    game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                }

                val bearsId = game.findPermanent("Grizzly Bears")!!
                val glorySeekerIdVal = game.findPermanent("Glory Seeker")!!
                game.execute(DeclareAttackers(game.player2Id, mapOf(
                    bearsId to game.player1Id,
                    glorySeekerIdVal to game.player1Id
                )))

                // Advance to declare blockers
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.execute(DeclareBlockers(game.player1Id, emptyMap()))

                // Turn Frontline Strategist face up
                game.execute(TurnFaceUp(game.player1Id, faceDownId))
                game.resolveStack()

                // Pass through to after combat damage
                game.passUntilPhase(Phase.COMBAT, Step.END_COMBAT)

                // P1 should take 2 damage from Glory Seeker (Soldier) but NOT from Bears
                val p1Life = game.getLifeTotal(1)
                withClue("Player 1 should take damage only from Soldier creature (Glory Seeker)") {
                    p1Life shouldBe 18 // 20 - 2 from Glory Seeker
                }
            }
        }
    }
}

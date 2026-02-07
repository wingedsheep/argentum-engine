package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.RevealedToComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Tests for Aven Soulgazer:
 * {3}{W}{W}
 * Creature — Bird Cleric
 * 3/3
 * Flying
 * {2}{W}: Look at target face-down creature.
 *
 * Cards used:
 * - Aven Soulgazer ({3}{W}{W}, 3/3 Flying, activated: look at face-down creature)
 * - Battering Craghorn ({3}{R}, 3/1 First Strike, Morph {1}{R}) — morph creature to look at
 */
class AvenSoulgazerScenarioTest : ScenarioTestBase() {

    init {
        context("Aven Soulgazer activated ability") {

            test("can look at opponent's face-down creature") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Aven Soulgazer")
                    .withLandsOnBattlefield(1, "Plains", 3)
                    .withCardInHand(2, "Battering Craghorn")
                    .withLandsOnBattlefield(2, "Mountain", 3)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Player 2 casts Battering Craghorn face-down for {3}
                val craghornId = game.state.getHand(game.player2Id).first { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Battering Craghorn"
                }
                val castResult = game.execute(CastSpell(game.player2Id, craghornId, castFaceDown = true))
                withClue("Cast morph should succeed") {
                    castResult.error shouldBe null
                }
                game.resolveStack()

                // Find the face-down creature
                val faceDownId = game.state.getBattlefield().find { entityId ->
                    game.state.getEntity(entityId)?.has<FaceDownComponent>() == true
                }
                withClue("Face-down creature should be on battlefield") {
                    faceDownId shouldNotBe null
                }

                // Player 2 passes priority so Player 1 gets it
                game.passPriority()

                // Player 1 activates Aven Soulgazer's ability targeting the face-down creature
                val soulgazerId = game.findPermanent("Aven Soulgazer")!!
                val cardDef = cardRegistry.getCard("Aven Soulgazer")!!
                val ability = cardDef.script.activatedAbilities.first()

                val activateResult = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = soulgazerId,
                        abilityId = ability.id,
                        targets = listOf(ChosenTarget.Permanent(faceDownId!!))
                    )
                )
                withClue("Activation should succeed: ${activateResult.error}") {
                    activateResult.error shouldBe null
                }

                // Resolve the ability
                game.resolveStack()

                // The face-down creature should now be revealed to Player 1
                val revealedTo = game.state.getEntity(faceDownId)?.get<RevealedToComponent>()
                withClue("Face-down creature should be revealed to Player 1") {
                    revealedTo shouldNotBe null
                    revealedTo!!.isRevealedTo(game.player1Id) shouldBe true
                }

                // It should NOT be revealed to Player 2 (they already know since they own it)
                withClue("Face-down creature should not be marked as revealed to Player 2") {
                    revealedTo!!.isRevealedTo(game.player2Id) shouldBe false
                }
            }

            test("cannot target a face-up creature") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Aven Soulgazer")
                    .withLandsOnBattlefield(1, "Plains", 3)
                    .withCardOnBattlefield(2, "Battering Craghorn") // face-up
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val soulgazerId = game.findPermanent("Aven Soulgazer")!!
                val craghornId = game.findPermanent("Battering Craghorn")!!
                val cardDef = cardRegistry.getCard("Aven Soulgazer")!!
                val ability = cardDef.script.activatedAbilities.first()

                // Try to activate targeting a face-up creature — should fail
                val activateResult = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = soulgazerId,
                        abilityId = ability.id,
                        targets = listOf(ChosenTarget.Permanent(craghornId))
                    )
                )
                withClue("Should not be able to target a face-up creature") {
                    activateResult.error shouldNotBe null
                }
            }
        }
    }
}

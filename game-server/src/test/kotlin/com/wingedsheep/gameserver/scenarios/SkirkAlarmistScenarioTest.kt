package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Skirk Alarmist.
 *
 * Skirk Alarmist: {1}{R} 1/2 Creature — Human Wizard
 * Haste
 * {T}: Turn target face-down creature you control face up. At the beginning of
 * the next end step, sacrifice it.
 */
class SkirkAlarmistScenarioTest : ScenarioTestBase() {

    init {
        context("Skirk Alarmist activated ability") {

            test("turns face-down creature face up and sacrifices it at end step") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Skirk Alarmist")
                    .withCardInHand(1, "Branchsnap Lorian") // Has morph {G}, no trigger
                    .withLandsOnBattlefield(1, "Mountain", 4)
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast Branchsnap Lorian face-down for {3}
                val exterminatorCardId = game.state.getHand(game.player1Id).first { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Branchsnap Lorian"
                }
                val castResult = game.execute(CastSpell(game.player1Id, exterminatorCardId, castFaceDown = true))
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

                // Activate Skirk Alarmist's ability targeting the face-down creature
                val alarmistId = game.findPermanent("Skirk Alarmist")!!
                val cardDef = cardRegistry.getCard("Skirk Alarmist")!!
                val ability = cardDef.script.activatedAbilities.first()

                val activateResult = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = alarmistId,
                        abilityId = ability.id,
                        targets = listOf(ChosenTarget.Permanent(faceDownId!!))
                    )
                )
                withClue("Activation should succeed: ${activateResult.error}") {
                    activateResult.error shouldBe null
                }

                // Resolve the ability (Branchsnap Lorian has no turn-face-up trigger)
                game.resolveStack()

                // The creature should now be face-up
                withClue("Creature should be face-up after Skirk Alarmist activation") {
                    game.state.getEntity(faceDownId)?.has<FaceDownComponent>() shouldBe false
                }
                withClue("Creature should be Branchsnap Lorian") {
                    game.state.getEntity(faceDownId)?.get<CardComponent>()?.name shouldBe "Branchsnap Lorian"
                }
                withClue("Creature should still be on battlefield") {
                    game.isOnBattlefield("Branchsnap Lorian") shouldBe true
                }

                // Advance to the end step - delayed trigger should sacrifice the creature
                game.passUntilPhase(Phase.ENDING, Step.END)

                // The delayed trigger should be on the stack - resolve it
                if (game.state.stack.isNotEmpty()) {
                    game.resolveStack()
                }

                // Creature should have been sacrificed
                withClue("Branchsnap Lorian should be in graveyard after end step sacrifice") {
                    game.isInGraveyard(1, "Branchsnap Lorian") shouldBe true
                }
                withClue("Branchsnap Lorian should not be on battlefield after sacrifice") {
                    game.isOnBattlefield("Branchsnap Lorian") shouldBe false
                }
            }

            test("cannot target face-up creature") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Skirk Alarmist")
                    .withCardOnBattlefield(1, "Branchsnap Lorian") // Face-up
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val alarmistId = game.findPermanent("Skirk Alarmist")!!
                val exterminatorId = game.findPermanent("Branchsnap Lorian")!!
                val cardDef = cardRegistry.getCard("Skirk Alarmist")!!
                val ability = cardDef.script.activatedAbilities.first()

                // Trying to target a face-up creature should fail
                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = alarmistId,
                        abilityId = ability.id,
                        targets = listOf(ChosenTarget.Permanent(exterminatorId))
                    )
                )
                withClue("Should not be able to target face-up creature") {
                    result.error shouldNotBe null
                }
            }
        }
    }
}

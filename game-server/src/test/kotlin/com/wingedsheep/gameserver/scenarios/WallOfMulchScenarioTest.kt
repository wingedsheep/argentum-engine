package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Wall of Mulch.
 *
 * Card reference:
 * - Wall of Mulch ({1}{G}): Creature — Wall, 0/4
 *   "Defender (This creature can't attack.)
 *    {G}, Sacrifice a Wall: Draw a card."
 */
class WallOfMulchScenarioTest : ScenarioTestBase() {

    init {
        context("Wall of Mulch - sacrifice a Wall to draw a card") {

            test("sacrificing itself to draw a card") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Wall of Mulch")
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withCardInLibrary(1, "Mountain")  // card to draw
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val initialHandSize = game.handSize(1)

                val wallId = game.findPermanent("Wall of Mulch")!!
                val cardDef = cardRegistry.getCard("Wall of Mulch")!!
                val ability = cardDef.script.activatedAbilities.first()

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = wallId,
                        abilityId = ability.id,
                        costPayment = AdditionalCostPayment(
                            sacrificedPermanents = listOf(wallId)
                        )
                    )
                )

                withClue("Ability should activate: ${result.error}") {
                    result.error shouldBe null
                }

                // Wall of Mulch should be sacrificed (in graveyard)
                withClue("Wall of Mulch should be in graveyard") {
                    game.isInGraveyard(1, "Wall of Mulch") shouldBe true
                }

                // Resolve the ability
                game.resolveStack()

                // Should have drawn a card
                withClue("Player should have drawn a card") {
                    game.handSize(1) shouldBe initialHandSize + 1
                }
            }

            test("sacrificing a different Wall to draw a card") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Wall of Mulch")
                    .withCardOnBattlefield(1, "Crude Rampart")  // another Wall
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withCardInLibrary(1, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val initialHandSize = game.handSize(1)

                val wallOfMulchId = game.findPermanent("Wall of Mulch")!!
                val crudeRampartId = game.findPermanent("Crude Rampart")!!
                val cardDef = cardRegistry.getCard("Wall of Mulch")!!
                val ability = cardDef.script.activatedAbilities.first()

                // Sacrifice Crude Rampart (a different Wall) using Wall of Mulch's ability
                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = wallOfMulchId,
                        abilityId = ability.id,
                        costPayment = AdditionalCostPayment(
                            sacrificedPermanents = listOf(crudeRampartId)
                        )
                    )
                )

                withClue("Ability should activate: ${result.error}") {
                    result.error shouldBe null
                }

                // Crude Rampart should be sacrificed
                withClue("Crude Rampart should be in graveyard") {
                    game.isInGraveyard(1, "Crude Rampart") shouldBe true
                }

                // Wall of Mulch should still be on the battlefield
                withClue("Wall of Mulch should still be on battlefield") {
                    game.isOnBattlefield("Wall of Mulch") shouldBe true
                }

                // Resolve the ability
                game.resolveStack()

                // Should have drawn a card
                withClue("Player should have drawn a card") {
                    game.handSize(1) shouldBe initialHandSize + 1
                }
            }

            test("cannot sacrifice a non-Wall creature") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Wall of Mulch")
                    .withCardOnBattlefield(1, "Elvish Warrior")  // Elf Warrior, not a Wall
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val wallId = game.findPermanent("Wall of Mulch")!!
                val elfId = game.findPermanent("Elvish Warrior")!!
                val cardDef = cardRegistry.getCard("Wall of Mulch")!!
                val ability = cardDef.script.activatedAbilities.first()

                // Try to sacrifice Elvish Warrior (not a Wall) - should fail
                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = wallId,
                        abilityId = ability.id,
                        costPayment = AdditionalCostPayment(
                            sacrificedPermanents = listOf(elfId)
                        )
                    )
                )

                withClue("Sacrificing a non-Wall creature should fail") {
                    (result.error != null) shouldBe true
                }
            }
        }
    }
}

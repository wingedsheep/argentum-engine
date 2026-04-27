package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Meek Attack.
 *
 * Meek Attack: {2}{R} Enchantment
 * "{1}{R}: You may put a creature card with total power and toughness 5 or less from
 * your hand onto the battlefield. That creature gains haste. At the beginning of the
 * next end step, sacrifice that creature."
 */
class MeekAttackScenarioTest : ScenarioTestBase() {

    init {
        context("Meek Attack activated ability") {

            test("puts a small creature from hand onto the battlefield with haste, sacrificed at end step") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Meek Attack")
                    .withCardInHand(1, "Grizzly Bears") // 2/2, total P+T = 4
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val meekAttackId = game.findPermanent("Meek Attack")!!
                val cardDef = cardRegistry.getCard("Meek Attack")!!
                val ability = cardDef.script.activatedAbilities.first()

                val activateResult = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = meekAttackId,
                        abilityId = ability.id
                    )
                )
                withClue("Activation should succeed: ${activateResult.error}") {
                    activateResult.error shouldBe null
                }
                game.resolveStack()

                // Pipeline pauses for SelectFromCollection — pick Grizzly Bears
                val grizzlyInHand = game.findCardsInHand(1, "Grizzly Bears").first()
                game.selectCards(listOf(grizzlyInHand))

                // Resolve any remaining steps in the ability
                game.resolveStack()

                // Grizzly Bears should now be on the battlefield
                withClue("Grizzly Bears should be on the battlefield") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe true
                }

                val grizzlyOnBattlefield = game.findPermanent("Grizzly Bears")!!
                withClue("Grizzly Bears should have haste") {
                    game.state.projectedState.hasKeyword(grizzlyOnBattlefield, Keyword.HASTE) shouldBe true
                }

                // Advance to end step — delayed trigger sacrifices the creature
                game.passUntilPhase(Phase.ENDING, Step.END)
                if (game.state.stack.isNotEmpty()) {
                    game.resolveStack()
                }

                withClue("Grizzly Bears should be in graveyard after end step sacrifice") {
                    game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                }
                withClue("Grizzly Bears should not be on the battlefield after sacrifice") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe false
                }
            }

            test("filter excludes creatures with total power and toughness greater than 5") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Meek Attack")
                    .withCardInHand(1, "Charging Rhino") // 4/4, total P+T = 8 — excluded
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val meekAttackId = game.findPermanent("Meek Attack")!!
                val cardDef = cardRegistry.getCard("Meek Attack")!!
                val ability = cardDef.script.activatedAbilities.first()

                val activateResult = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = meekAttackId,
                        abilityId = ability.id
                    )
                )
                withClue("Activation should succeed even with no eligible creatures: ${activateResult.error}") {
                    activateResult.error shouldBe null
                }
                game.resolveStack()

                // SelectFromCollection over an empty filtered collection — skip
                if (game.hasPendingDecision()) {
                    game.skipSelection()
                }
                game.resolveStack()

                withClue("Charging Rhino should remain in hand") {
                    val handHas = game.state.getHand(game.player1Id).any { entityId ->
                        game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Charging Rhino"
                    }
                    handHas shouldBe true
                }
                withClue("No creature should have been put onto the battlefield") {
                    game.isOnBattlefield("Charging Rhino") shouldBe false
                }
            }

            test("declining the optional put leaves the creature in hand") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Meek Attack")
                    .withCardInHand(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val meekAttackId = game.findPermanent("Meek Attack")!!
                val cardDef = cardRegistry.getCard("Meek Attack")!!
                val ability = cardDef.script.activatedAbilities.first()

                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = meekAttackId,
                        abilityId = ability.id
                    )
                )
                game.resolveStack()

                // Decline the optional selection — pick zero cards
                game.skipSelection()
                game.resolveStack()

                withClue("Grizzly Bears should remain in hand when player declines") {
                    game.isInHand(1, "Grizzly Bears") shouldBe true
                }
                withClue("Grizzly Bears should not be on the battlefield") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe false
                }

                // No delayed sacrifice trigger should fire at end step since nothing was put
                game.passUntilPhase(Phase.ENDING, Step.END)
                withClue("Grizzly Bears should still be in hand at end step") {
                    game.isInHand(1, "Grizzly Bears") shouldBe true
                }
            }

            test("does not sacrifice the creature if another player controls it at the end step") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Meek Attack")
                    .withCardInHand(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val meekAttackId = game.findPermanent("Meek Attack")!!
                val ability = cardRegistry.getCard("Meek Attack")!!
                    .script.activatedAbilities.first()

                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = meekAttackId,
                        abilityId = ability.id
                    )
                )
                game.resolveStack()

                val grizzlyInHand = game.findCardsInHand(1, "Grizzly Bears").first()
                game.selectCards(listOf(grizzlyInHand))
                game.resolveStack()

                val grizzlyOnBattlefield = game.findPermanent("Grizzly Bears")!!
                game.state = game.state.updateEntity(grizzlyOnBattlefield) {
                    it.with(ControllerComponent(game.player2Id))
                }

                game.passUntilPhase(Phase.ENDING, Step.END)
                if (game.state.stack.isNotEmpty()) {
                    game.resolveStack()
                }

                withClue("Grizzly Bears should remain on the battlefield when Player1 no longer controls it") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe true
                }
                withClue("Grizzly Bears should not be in Player1's graveyard") {
                    game.isInGraveyard(1, "Grizzly Bears") shouldBe false
                }
            }
        }
    }
}

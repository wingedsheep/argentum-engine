package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.gameserver.session.GameSession
import com.wingedsheep.gameserver.session.PlayerSession
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockk
import org.springframework.web.socket.WebSocketSession

/**
 * Scenario tests for Singing Bell Strike.
 *
 * Card reference:
 * - Singing Bell Strike ({1}{U}): Enchantment — Aura
 *   "Enchant creature
 *    When this Aura enters, tap enchanted creature.
 *    Enchanted creature doesn't untap during its controller's untap step.
 *    Enchanted creature has '{6}: Untap this creature.'"
 */
class SingingBellStrikeScenarioTest : ScenarioTestBase() {

    init {
        context("Singing Bell Strike") {

            test("ETB trigger taps the enchanted creature") {
                val game = scenario()
                    .withPlayers()
                    .withCardInHand(1, "Singing Bell Strike")
                    .withCardOnBattlefield(2, "Glory Seeker")
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val creature = game.findPermanent("Glory Seeker")!!

                // Cast Singing Bell Strike targeting the creature
                val castResult = game.castSpell(1, "Singing Bell Strike", creature)
                withClue("Cast should succeed: ${castResult.error}") {
                    castResult.error shouldBe null
                }

                // Resolve everything (aura + ETB trigger)
                game.resolveStack()

                // Check if there's still a trigger to resolve
                if (game.state.stack.isNotEmpty()) {
                    game.resolveStack()
                }

                // Creature should be tapped from the ETB trigger
                val container = game.state.getEntity(creature)!!
                withClue("Glory Seeker should be tapped by ETB trigger. Stack size: ${game.state.stack.size}, pending: ${game.state.pendingDecision}") {
                    container.has<com.wingedsheep.engine.state.components.battlefield.TappedComponent>() shouldBe true
                }
            }

            test("enchanted creature doesn't untap during untap step") {
                val game = scenario()
                    .withPlayers()
                    .withCardOnBattlefield(1, "Singing Bell Strike")
                    .withCardOnBattlefield(2, "Glory Seeker", tapped = true)
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Manually attach the aura to the creature
                val aura = game.findPermanent("Singing Bell Strike")!!
                val creature = game.findPermanent("Glory Seeker")!!
                game.state = game.state.updateEntity(aura) { container ->
                    container.with(com.wingedsheep.engine.state.components.battlefield.AttachedToComponent(creature))
                }.updateEntity(creature) { container ->
                    container.with(com.wingedsheep.engine.state.components.battlefield.AttachmentsComponent(listOf(aura)))
                }

                // Pass through player 2's turn entirely and into player 1's turn,
                // then back to player 2's turn to test untap
                game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

                // Creature should still be tapped (DOESNT_UNTAP prevents untapping)
                val container = game.state.getEntity(creature)!!
                withClue("Glory Seeker should remain tapped due to DOESNT_UNTAP") {
                    container.has<com.wingedsheep.engine.state.components.battlefield.TappedComponent>() shouldBe true
                }
            }

            test("enchanted creature has the granted untap ability in legal actions") {
                val game = scenario()
                    .withPlayers()
                    .withCardOnBattlefield(1, "Singing Bell Strike")
                    .withCardOnBattlefield(2, "Glory Seeker", tapped = true)
                    .withLandsOnBattlefield(2, "Island", 6)
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Manually attach the aura to the creature
                val aura = game.findPermanent("Singing Bell Strike")!!
                val creature = game.findPermanent("Glory Seeker")!!
                game.state = game.state.updateEntity(aura) { container ->
                    container.with(com.wingedsheep.engine.state.components.battlefield.AttachedToComponent(creature))
                }.updateEntity(creature) { container ->
                    container.with(com.wingedsheep.engine.state.components.battlefield.AttachmentsComponent(listOf(aura)))
                }

                // Create a GameSession to check legal actions
                val session = GameSession(cardRegistry = cardRegistry)
                val mockWs1 = mockk<WebSocketSession>(relaxed = true) { every { id } returns "ws1" }
                val mockWs2 = mockk<WebSocketSession>(relaxed = true) { every { id } returns "ws2" }
                val player1Session = PlayerSession(mockWs1, game.player1Id, "Player1")
                val player2Session = PlayerSession(mockWs2, game.player2Id, "Player2")
                session.injectStateForTesting(
                    game.state,
                    mapOf(game.player1Id to player1Session, game.player2Id to player2Session)
                )

                val legalActions = session.getLegalActions(game.player2Id)

                // Glory Seeker should have the untap ability
                val untapAction = legalActions.find {
                    it.actionType == "ActivateAbility" &&
                        (it.action as? ActivateAbility)?.sourceId == creature &&
                        it.description.contains("Untap")
                }
                withClue("Glory Seeker should have the untap ability granted by Singing Bell Strike") {
                    untapAction shouldNotBe null
                }
            }

            test("granted untap ability actually untaps the creature") {
                val game = scenario()
                    .withPlayers()
                    .withCardOnBattlefield(1, "Singing Bell Strike")
                    .withCardOnBattlefield(2, "Glory Seeker", tapped = true)
                    .withLandsOnBattlefield(2, "Island", 6)
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Manually attach the aura to the creature
                val aura = game.findPermanent("Singing Bell Strike")!!
                val creature = game.findPermanent("Glory Seeker")!!
                game.state = game.state.updateEntity(aura) { container ->
                    container.with(com.wingedsheep.engine.state.components.battlefield.AttachedToComponent(creature))
                }.updateEntity(creature) { container ->
                    container.with(com.wingedsheep.engine.state.components.battlefield.AttachmentsComponent(listOf(aura)))
                }

                // Get the ability ID from the card definition
                val singingBellDef = cardRegistry.getCard("Singing Bell Strike")!!
                val grantAbility = singingBellDef.staticAbilities
                    .filterIsInstance<GrantActivatedAbility>()
                    .first()
                val untapAbilityId = grantAbility.ability.id

                // Activate the untap ability
                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player2Id,
                        sourceId = creature,
                        abilityId = untapAbilityId
                    )
                )
                withClue("Activating untap ability should succeed: ${result.error}") {
                    result.error shouldBe null
                }

                // Resolve the ability
                game.resolveStack()

                // Creature should be untapped
                val container = game.state.getEntity(creature)!!
                withClue("Glory Seeker should be untapped after activating the ability") {
                    container.has<com.wingedsheep.engine.state.components.battlefield.TappedComponent>() shouldBe false
                }
            }
        }
    }
}

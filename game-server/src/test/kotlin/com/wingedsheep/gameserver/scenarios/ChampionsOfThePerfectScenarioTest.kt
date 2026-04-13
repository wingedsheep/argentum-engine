package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.state.components.battlefield.LinkedExileComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.nulls.shouldNotBeNull

/**
 * Scenario tests for Champions of the Perfect (Lorwyn Eclipsed).
 *
 * Champions of the Perfect:
 * - {3}{G} Creature — Elf Warrior 6/6
 * - As an additional cost to cast this spell, behold an Elf and exile it.
 * - Whenever you cast a creature spell, draw a card.
 * - When this creature leaves the battlefield, return the exiled card to its owner's hand.
 */
class ChampionsOfThePerfectScenarioTest : ScenarioTestBase() {

    private fun isInExile(game: ScenarioTestBase.TestGame, playerNumber: Int, cardName: String): Boolean {
        val playerId = if (playerNumber == 1) game.player1Id else game.player2Id
        return game.state.getZone(ZoneKey(playerId, Zone.EXILE)).any {
            game.state.getEntity(it)?.get<CardComponent>()?.name == cardName
        }
    }

    init {
        context("Behold and exile cost") {
            test("casting with an Elf on the battlefield exiles it") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Champions of the Perfect")
                    .withCardOnBattlefield(1, "Elvish Pioneer")
                    .withLandsOnBattlefield(1, "Forest", 4)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val castResult = game.castSpellWithBeholdCost(1, "Champions of the Perfect", "Elvish Pioneer")
                withClue("Should cast successfully: ${castResult.error}") {
                    castResult.error shouldBe null
                }

                // Elvish Pioneer should be in exile (beheld and exiled as cost)
                withClue("Elvish Pioneer should be in exile") {
                    isInExile(game, 1, "Elvish Pioneer") shouldBe true
                }

                // Champions should be on the stack
                withClue("Champions should be on the stack") {
                    game.state.stack.any { entityId ->
                        game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Champions of the Perfect"
                    } shouldBe true
                }
            }

            test("casting with an Elf in hand exiles it") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Champions of the Perfect")
                    .withCardInHand(1, "Elvish Pioneer")
                    .withLandsOnBattlefield(1, "Forest", 4)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val castResult = game.castSpellWithBeholdCost(1, "Champions of the Perfect", "Elvish Pioneer")
                withClue("Should cast successfully: ${castResult.error}") {
                    castResult.error shouldBe null
                }

                withClue("Elvish Pioneer should be in exile") {
                    isInExile(game, 1, "Elvish Pioneer") shouldBe true
                }
            }
        }

        context("LinkedExileComponent persists to permanent") {
            test("resolved Champions has LinkedExileComponent pointing to exiled card") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Champions of the Perfect")
                    .withCardOnBattlefield(1, "Elvish Pioneer")
                    .withLandsOnBattlefield(1, "Forest", 4)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val castResult = game.castSpellWithBeholdCost(1, "Champions of the Perfect", "Elvish Pioneer")
                withClue("Should cast successfully: ${castResult.error}") {
                    castResult.error shouldBe null
                }

                // Resolve — Champions enters the battlefield
                game.resolveStack()

                val championsId = game.state.getBattlefield().find { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Champions of the Perfect"
                }
                withClue("Champions should be on the battlefield") {
                    championsId.shouldNotBeNull()
                }

                val linked = game.state.getEntity(championsId!!)?.get<LinkedExileComponent>()
                withClue("Champions should have LinkedExileComponent with the exiled Elf") {
                    linked.shouldNotBeNull()
                    linked.exiledIds.size shouldBe 1
                }
            }
        }
    }
}

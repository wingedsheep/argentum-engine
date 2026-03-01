package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.gameserver.session.GameSession
import com.wingedsheep.gameserver.session.PlayerSession
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.GrantActivatedAbilityToCreatureGroup
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockk
import org.springframework.web.socket.WebSocketSession

/**
 * Scenario tests for Spectral Sliver.
 *
 * Card reference:
 * - Spectral Sliver ({2}{B}): Creature — Sliver Spirit, 2/2
 *   "All Sliver creatures have '{2}: This creature gets +1/+1 until end of turn.'"
 *
 * Tests:
 * 1. A Sliver creature gets the pump ability when Spectral Sliver is on the battlefield
 * 2. The pump ability actually modifies stats
 * 3. Non-Sliver creatures don't get the ability
 */
class SpectralSliverScenarioTest : ScenarioTestBase() {

    init {
        context("Spectral Sliver grants pump ability to Slivers") {

            test("Sliver creature gains activated pump ability from Spectral Sliver") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Spectral Sliver")
                    .withCardOnBattlefield(1, "Blade Sliver") // Another Sliver
                    .withLandsOnBattlefield(1, "Swamp", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

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

                val legalActions = session.getLegalActions(game.player1Id)

                // Blade Sliver should have the pump ability from Spectral Sliver
                val bladeSliver = game.findPermanent("Blade Sliver")!!
                val pumpAction = legalActions.find {
                    it.actionType == "ActivateAbility" &&
                        (it.action as? ActivateAbility)?.sourceId == bladeSliver &&
                        it.description.contains("+1/+1")
                }
                withClue("Blade Sliver should have the pump ability granted by Spectral Sliver") {
                    pumpAction shouldNotBe null
                }
            }

            test("pump ability modifies stats when activated") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Spectral Sliver")
                    .withLandsOnBattlefield(1, "Swamp", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Get the pump ability ID from the Spectral Sliver card definition
                val spectralDef = cardRegistry.getCard("Spectral Sliver")!!
                val grantAbility = spectralDef.staticAbilities
                    .filterIsInstance<GrantActivatedAbilityToCreatureGroup>()
                    .first()
                val pumpAbilityId = grantAbility.ability.id

                val spectralSliver = game.findPermanent("Spectral Sliver")!!

                // Activate the pump ability on Spectral Sliver itself (it's a Sliver)
                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = spectralSliver,
                        abilityId = pumpAbilityId
                    )
                )
                withClue("Activating pump ability should succeed: ${result.error}") {
                    result.error shouldBe null
                }

                // Resolve the ability from the stack
                game.resolveStack()

                // Check projected stats — Spectral Sliver should be 3/3
                val clientState = game.getClientState(1)
                val spectralCard = clientState.cards.values.find { it.name == "Spectral Sliver" }
                withClue("Spectral Sliver should be 3/3 after pump") {
                    spectralCard shouldNotBe null
                    spectralCard!!.power shouldBe 3
                    spectralCard.toughness shouldBe 3
                }
            }

            test("non-Sliver creatures do not get the pump ability") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Spectral Sliver")
                    .withCardOnBattlefield(1, "Grizzly Bears") // Not a Sliver
                    .withLandsOnBattlefield(1, "Swamp", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val session = GameSession(cardRegistry = cardRegistry)
                val mockWs1 = mockk<WebSocketSession>(relaxed = true) { every { id } returns "ws1" }
                val mockWs2 = mockk<WebSocketSession>(relaxed = true) { every { id } returns "ws2" }
                val player1Session = PlayerSession(mockWs1, game.player1Id, "Player1")
                val player2Session = PlayerSession(mockWs2, game.player2Id, "Player2")
                session.injectStateForTesting(
                    game.state,
                    mapOf(game.player1Id to player1Session, game.player2Id to player2Session)
                )

                val legalActions = session.getLegalActions(game.player1Id)

                // Grizzly Bears should NOT have the pump ability
                val grizzlyBears = game.findPermanent("Grizzly Bears")!!
                val pumpAction = legalActions.find {
                    it.actionType == "ActivateAbility" &&
                        (it.action as? ActivateAbility)?.sourceId == grizzlyBears &&
                        it.description.contains("+1/+1")
                }
                withClue("Grizzly Bears should NOT have the pump ability") {
                    pumpAction shouldBe null
                }
            }
        }
    }
}

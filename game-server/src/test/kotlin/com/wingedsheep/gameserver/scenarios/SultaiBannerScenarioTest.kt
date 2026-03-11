package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.gameserver.session.GameSession
import com.wingedsheep.gameserver.session.PlayerSession
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockk
import org.springframework.web.socket.WebSocketSession

/**
 * Scenario tests for Sultai Banner.
 *
 * Sultai Banner: {3} Artifact
 * {T}: Add {B}, {G}, or {U}.
 * {B}{G}{U}, {T}, Sacrifice this artifact: Draw a card.
 *
 * The draw ability requires {B}{G}{U}, {T}, and sacrificing the banner.
 * Since the banner must tap itself as part of the cost, it cannot use its own
 * mana abilities to pay for {B}{G}{U} — external mana sources are required.
 */
class SultaiBannerScenarioTest : ScenarioTestBase() {

    init {
        context("Sultai Banner draw ability") {

            test("cannot activate draw ability when banner is the only mana source") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Sultai Banner")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bannerId = game.findPermanent("Sultai Banner")!!
                val cardDef = cardRegistry.getCard("Sultai Banner")!!
                // The draw ability is the 4th activated ability (after B, G, U mana abilities)
                val drawAbility = cardDef.script.activatedAbilities[3]

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = bannerId,
                        abilityId = drawAbility.id
                    )
                )

                withClue("Draw ability should fail — banner can't tap for mana AND tap as cost") {
                    result.error shouldNotBe null
                }
            }

            test("draw ability should not appear in legal actions when banner is only mana source") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Sultai Banner")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val session = GameSession(cardRegistry = cardRegistry)
                val mockWs1 = mockk<WebSocketSession>(relaxed = true) { every { id } returns "ws1" }
                val mockWs2 = mockk<WebSocketSession>(relaxed = true) { every { id } returns "ws2" }
                val player1Session = PlayerSession(mockWs1, game.player1Id, "Player")
                val player2Session = PlayerSession(mockWs2, game.player2Id, "Opponent")
                session.injectStateForTesting(
                    game.state,
                    mapOf(game.player1Id to player1Session, game.player2Id to player2Session)
                )

                val legalActions = session.getLegalActions(game.player1Id)

                // Mana abilities should still be available
                val manaActions = legalActions.filter {
                    it.description?.contains("Add {B}") == true ||
                    it.description?.contains("Add {G}") == true ||
                    it.description?.contains("Add {U}") == true
                }
                withClue("Mana abilities should be legal") {
                    manaActions.size shouldBe 3
                }

                // Draw ability should NOT be legal
                val drawAction = legalActions.find {
                    it.description?.contains("Draw a card") == true
                }
                withClue("Draw ability should not be legal when banner is the only mana source") {
                    drawAction shouldBe null
                }
            }

            test("can activate draw ability with external mana sources") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Sultai Banner")
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(2, "Swamp")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bannerId = game.findPermanent("Sultai Banner")!!
                val cardDef = cardRegistry.getCard("Sultai Banner")!!
                val drawAbility = cardDef.script.activatedAbilities[3]

                val handSizeBefore = game.state.getZone(game.player1Id, com.wingedsheep.sdk.core.Zone.HAND).size

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = bannerId,
                        abilityId = drawAbility.id
                    )
                )

                withClue("Draw ability should succeed with external mana: ${result.error}") {
                    result.error shouldBe null
                }

                game.resolveStack()

                // Banner should be gone (sacrificed)
                withClue("Banner should be sacrificed") {
                    game.isOnBattlefield("Sultai Banner") shouldBe false
                }

                // Player should have drawn a card
                val handSizeAfter = game.state.getZone(game.player1Id, com.wingedsheep.sdk.core.Zone.HAND).size
                withClue("Player should have drawn a card") {
                    handSizeAfter shouldBe handSizeBefore + 1
                }
            }
        }
    }
}

package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.gameserver.session.GameSession
import com.wingedsheep.gameserver.session.PlayerSession
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.nulls.shouldNotBeNull
import io.mockk.every
import io.mockk.mockk
import org.springframework.web.socket.WebSocketSession

/**
 * Tests that Auras enchanting opponent's creatures are still controlled by the caster.
 * The caster should be able to activate abilities on the Aura (e.g., sacrifice Crown of Suspicion).
 */
class AuraOnOpponentCreatureScenarioTest : ScenarioTestBase() {

    private val stateProjector = StateProjector()

    init {
        context("Aura on opponent's creature") {
            test("caster controls Aura enchanting opponent's creature and can sacrifice it") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Crown of Suspicion")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Find the opponent's creature
                val grizzlyBears = game.findPermanent("Grizzly Bears")
                withClue("Grizzly Bears should be on the battlefield") {
                    grizzlyBears.shouldNotBeNull()
                }

                // Player 1 casts Crown of Suspicion on opponent's creature
                val castResult = game.castSpell(1, "Crown of Suspicion", grizzlyBears)
                withClue("Crown of Suspicion should be cast successfully: ${castResult.error}") {
                    castResult.error shouldBe null
                }

                // Resolve the stack
                game.resolveStack()

                // Crown should be on the battlefield
                val crown = game.findPermanent("Crown of Suspicion")
                withClue("Crown of Suspicion should be on the battlefield") {
                    crown.shouldNotBeNull()
                }

                // Crown should be controlled by Player 1 (the caster)
                val crownController = game.state.getEntity(crown!!)?.get<ControllerComponent>()?.playerId
                withClue("Crown of Suspicion should be controlled by Player 1 (the caster)") {
                    crownController shouldBe game.player1Id
                }

                // Grizzly Bears should have +2/-1 from the static ability (2/2 -> 4/1)
                withClue("Grizzly Bears should have projected power 4 (2+2)") {
                    stateProjector.getProjectedPower(game.state, grizzlyBears!!) shouldBe 4
                }
                withClue("Grizzly Bears should have projected toughness 1 (2-1)") {
                    stateProjector.getProjectedToughness(game.state, grizzlyBears!!) shouldBe 1
                }
            }

            test("legal actions include sacrifice ability for Aura on opponent's creature") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Crown of Suspicion")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Find the opponent's creature and cast the Crown on it
                val grizzlyBears = game.findPermanent("Grizzly Bears")!!
                game.castSpell(1, "Crown of Suspicion", grizzlyBears)
                game.resolveStack()

                // Verify the Crown is on the battlefield and controlled by Player 1
                val crown = game.findPermanent("Crown of Suspicion")
                withClue("Crown should be on the battlefield") {
                    crown.shouldNotBeNull()
                }

                // Create a GameSession and inject the current state
                val session = GameSession(cardRegistry = cardRegistry)
                val mockWs1 = mockk<WebSocketSession>(relaxed = true) { every { id } returns "ws1" }
                val mockWs2 = mockk<WebSocketSession>(relaxed = true) { every { id } returns "ws2" }
                val player1Session = PlayerSession(mockWs1, game.player1Id, "Player1")
                val player2Session = PlayerSession(mockWs2, game.player2Id, "Player2")
                session.injectStateForTesting(
                    game.state,
                    mapOf(game.player1Id to player1Session, game.player2Id to player2Session)
                )

                // Player 1 should have priority (active player after stack resolves)
                withClue("Player 1 should have priority") {
                    game.state.priorityPlayerId shouldBe game.player1Id
                }

                // Get legal actions for Player 1
                val legalActions = session.getLegalActions(game.player1Id)

                // Verify that Player 1 can activate the Crown's sacrifice ability
                val sacrificeAction = legalActions.find {
                    it.actionType == "ActivateAbility" &&
                        (it.action as? ActivateAbility)?.sourceId == crown
                }
                withClue("Legal actions should include sacrifice ability for Crown of Suspicion") {
                    sacrificeAction.shouldNotBeNull()
                }
            }

            test("caster can sacrifice Crown on opponent's creature to apply effect") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Crown of Suspicion")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast Crown on opponent's creature and resolve
                val grizzlyBears = game.findPermanent("Grizzly Bears")!!
                game.castSpell(1, "Crown of Suspicion", grizzlyBears)
                game.resolveStack()

                // Find the Crown and its ability ID
                val crown = game.findPermanent("Crown of Suspicion")!!
                val crownDef = cardRegistry.getCard("Crown of Suspicion")!!
                val abilityId = crownDef.script.activatedAbilities.first().id

                // Player 1 activates the sacrifice ability
                val activateResult = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = crown,
                        abilityId = abilityId
                    )
                )
                withClue("Player 1 should be able to activate Crown's sacrifice ability: ${activateResult.error}") {
                    activateResult.error shouldBe null
                }

                // Resolve the ability
                game.resolveStack()

                // Crown should now be in Player 1's graveyard (sacrificed as cost)
                withClue("Crown should be in Player 1's graveyard") {
                    game.isInGraveyard(1, "Crown of Suspicion") shouldBe true
                }

                // Grizzly Bears should have +2/-1 from the sacrifice effect (2/2 -> 4/1)
                withClue("Grizzly Bears should have projected power 4 after sacrifice effect") {
                    stateProjector.getProjectedPower(game.state, grizzlyBears) shouldBe 4
                }
                withClue("Grizzly Bears should have projected toughness 1 after sacrifice effect") {
                    stateProjector.getProjectedToughness(game.state, grizzlyBears) shouldBe 1
                }
            }
        }

        test("sacrifice ability only buffs creatures sharing a type with the enchanted creature") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Crown of Suspicion")
                .withLandsOnBattlefield(1, "Swamp", 2)
                // Enchanted creature: Glory Seeker = Human Soldier
                .withCardOnBattlefield(1, "Glory Seeker")
                // Same type (Human Soldier) — should get buff
                .withCardOnBattlefield(1, "Gustcloak Sentinel")
                // Different type (Goblin Warrior) — should NOT get buff
                .withCardOnBattlefield(1, "Goblin Sky Raider")
                .withCardInLibrary(1, "Swamp")
                .withCardInLibrary(2, "Mountain")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val glorySeeker = game.findPermanent("Glory Seeker")!!
            val gustcloakSentinel = game.findPermanent("Gustcloak Sentinel")!!
            val goblinSkyRaider = game.findPermanent("Goblin Sky Raider")!!

            // Cast Crown on Glory Seeker (Human Soldier)
            val castResult = game.castSpell(1, "Crown of Suspicion", glorySeeker)
            withClue("Crown cast should succeed: ${castResult.error}") {
                castResult.error shouldBe null
            }
            game.resolveStack()

            val crown = game.findPermanent("Crown of Suspicion")!!
            val crownDef = cardRegistry.getCard("Crown of Suspicion")!!
            val abilityId = crownDef.script.activatedAbilities.first().id

            // Sacrifice the Crown
            val activateResult = game.execute(ActivateAbility(
                playerId = game.player1Id,
                sourceId = crown,
                abilityId = abilityId
            ))
            withClue("Activate should succeed: ${activateResult.error}") {
                activateResult.error shouldBe null
            }
            game.resolveStack()

            // Glory Seeker (Human Soldier): base 2/2 → +2/-1 = 4/1
            withClue("Glory Seeker should be buffed (shares own type): power") {
                stateProjector.getProjectedPower(game.state, glorySeeker) shouldBe 4
            }
            withClue("Glory Seeker should be buffed (shares own type): toughness") {
                stateProjector.getProjectedToughness(game.state, glorySeeker) shouldBe 1
            }

            // Gustcloak Sentinel (Human Soldier): base 3/3 → +2/-1 = 5/2
            withClue("Gustcloak Sentinel should be buffed (shares Human+Soldier): power") {
                stateProjector.getProjectedPower(game.state, gustcloakSentinel) shouldBe 5
            }
            withClue("Gustcloak Sentinel should be buffed (shares Human+Soldier): toughness") {
                stateProjector.getProjectedToughness(game.state, gustcloakSentinel) shouldBe 2
            }

            // Goblin Sky Raider (Goblin Warrior): base 1/2 → should NOT be buffed
            withClue("Goblin Sky Raider should NOT be buffed (Goblin/Warrior ≠ Human/Soldier): power") {
                stateProjector.getProjectedPower(game.state, goblinSkyRaider) shouldBe 1
            }
            withClue("Goblin Sky Raider should NOT be buffed (Goblin/Warrior ≠ Human/Soldier): toughness") {
                stateProjector.getProjectedToughness(game.state, goblinSkyRaider) shouldBe 2
            }
        }
    }
}

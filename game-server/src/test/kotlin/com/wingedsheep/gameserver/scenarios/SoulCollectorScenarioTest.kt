package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Soul Collector.
 *
 * Card reference:
 * - Soul Collector ({3}{B}{B}): Creature — Vampire, 3/4
 *   Flying
 *   Whenever a creature dealt damage by Soul Collector this turn dies,
 *   return that card to the battlefield under your control.
 *   Morph {B}{B}{B}
 */
class SoulCollectorScenarioTest : ScenarioTestBase() {

    init {
        context("Soul Collector combat damage trigger") {
            test("creature killed in combat returns under Soul Collector's controller's control") {
                // Storm Crow has flying (can block Soul Collector) and 1/2 (dies to 3 damage)
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Soul Collector")
                    .withCardOnBattlefield(2, "Storm Crow") // 1/2 flying — dies to 3 damage from Soul Collector
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Advance to combat
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)

                // Attack with Soul Collector
                game.declareAttackers(mapOf("Soul Collector" to 2))

                // Advance to blockers
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                // Block Soul Collector with Storm Crow (both have flying)
                game.declareBlockers(mapOf("Storm Crow" to listOf("Soul Collector")))

                // Advance through combat damage - Storm Crow dies (2 toughness < 3 damage)
                // Soul Collector's trigger fires and returns Storm Crow under Player1's control
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                val stormCrowEntityId = game.findPermanent("Storm Crow")
                withClue("Storm Crow should be on the battlefield") {
                    stormCrowEntityId shouldNotBe null
                }
                withClue("Storm Crow should be under Player1's control") {
                    val controller = game.state.getEntity(stormCrowEntityId!!)?.get<ControllerComponent>()
                    controller?.playerId shouldBe game.player1Id
                }

                withClue("Soul Collector should still be on the battlefield") {
                    game.isOnBattlefield("Soul Collector") shouldBe true
                }
            }

            test("creature that survives combat does not trigger") {
                // Needleshot Gourna has reach (can block flyers) and is 3/6 (survives 3 damage)
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Soul Collector") // 3/4
                    .withCardOnBattlefield(2, "Needleshot Gourna") // 3/6 reach — survives 3 damage
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Advance to combat
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Soul Collector" to 2))
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                // Block with Needleshot Gourna (3/6 reach) - survives the 3 damage
                game.declareBlockers(mapOf("Needleshot Gourna" to listOf("Soul Collector")))

                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                withClue("Needleshot Gourna should still be on battlefield under Player2's control") {
                    val gournaId = game.findPermanent("Needleshot Gourna")
                    gournaId shouldNotBe null
                    val controller = game.state.getEntity(gournaId!!)?.get<ControllerComponent>()
                    controller?.playerId shouldBe game.player2Id
                }
            }

            test("Soul Collector trigger works when creature dealt damage later destroyed by spell") {
                // Needleshot Gourna has reach (can block flyers) and is 3/6 (survives combat)
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Soul Collector")
                    .withCardOnBattlefield(2, "Needleshot Gourna") // 3/6 reach
                    .withCardInHand(1, "Death Pulse") // Destroy target creature
                    .withLandsOnBattlefield(1, "Swamp", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Advance to combat
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Soul Collector" to 2))
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                // Block with Needleshot Gourna - survives 3 damage
                game.declareBlockers(mapOf("Needleshot Gourna" to listOf("Soul Collector")))

                // Advance to postcombat main
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                withClue("Needleshot Gourna should still be alive") {
                    game.isOnBattlefield("Needleshot Gourna") shouldBe true
                }

                // Cast Death Pulse targeting Needleshot Gourna — it was dealt damage by
                // Soul Collector this turn, so the trigger should fire
                val gournaId = game.findPermanent("Needleshot Gourna")!!
                game.castSpell(1, "Death Pulse", gournaId)
                game.resolveStack()

                withClue("Needleshot Gourna should be on battlefield under Player1's control") {
                    val newGournaId = game.findPermanent("Needleshot Gourna")
                    newGournaId shouldNotBe null
                    val controller = game.state.getEntity(newGournaId!!)?.get<ControllerComponent>()
                    controller?.playerId shouldBe game.player1Id
                }
            }
        }
    }
}

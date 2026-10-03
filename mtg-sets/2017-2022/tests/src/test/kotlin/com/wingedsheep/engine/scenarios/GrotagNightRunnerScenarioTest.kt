package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Grotag Night-Runner (ZNR) — whenever it deals combat damage to a player, exile
 * the top card of your library; you may play that card this turn.
 */
class GrotagNightRunnerScenarioTest : ScenarioTestBase() {

    private fun advanceThroughCombatDamage(game: TestGame) {
        game.passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)
        var iterations = 0
        while (game.state.pendingDecision == null && game.state.stack.isNotEmpty() && iterations++ < 20) {
            game.passPriority()
        }
    }

    private fun namesInExile(game: TestGame): Set<String> =
        game.state.getExile(game.player1Id).mapNotNull { id ->
            game.state.getEntity(id)?.get<CardComponent>()?.name
        }.toSet()

    init {
        context("Grotag Night-Runner") {
            test("connecting exiles your top card and lets you play it this turn") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Grotag Night-Runner", summoningSickness = false)
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                game.declareAttackers(mapOf("Grotag Night-Runner" to 2)).error shouldBe null
                advanceThroughCombatDamage(game)

                withClue("the 2/3 deals 2 combat damage") {
                    game.getLifeTotal(2) shouldBe 18
                }
                withClue("the top card of Player1's own library is exiled") {
                    namesInExile(game) shouldBe setOf("Grizzly Bears")
                }
                val exiled = game.state.getExile(game.player1Id).first()
                withClue("Player1 may play the exiled card") {
                    game.state.mayPlayPermissions.any {
                        it.controllerId == game.player1Id && exiled in it.cardIds
                    } shouldBe true
                }
            }

            test("blocked, it deals no damage to a player and exiles nothing") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Grotag Night-Runner", summoningSickness = false)
                    .withCardOnBattlefield(2, "Wall of Wood", summoningSickness = false)
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                game.declareAttackers(mapOf("Grotag Night-Runner" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareBlockers(mapOf("Wall of Wood" to listOf("Grotag Night-Runner"))).error shouldBe null
                advanceThroughCombatDamage(game)

                game.getLifeTotal(2) shouldBe 20
                namesInExile(game) shouldBe emptySet()
            }
        }
    }
}

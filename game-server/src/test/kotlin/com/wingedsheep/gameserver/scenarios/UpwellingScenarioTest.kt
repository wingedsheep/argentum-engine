package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class UpwellingScenarioTest : ScenarioTestBase() {
    init {
        context("Upwelling prevents mana pool emptying") {
            test("mana persists through turn end when Upwelling is on the battlefield") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardOnBattlefield(1, "Upwelling")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .withTurnNumber(2)
                    .inPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
                    .build()

                // Give player 1 some green mana in their pool
                game.state = game.state.updateEntity(game.player1Id) { container ->
                    container.with(ManaPoolComponent(green = 2))
                }

                // Advance through ending/cleanup to opponent's turn
                game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

                // After the turn ends and cleanup runs, mana should still be in pool
                val manaPool = game.state.getEntity(game.player1Id)?.get<ManaPoolComponent>()
                manaPool shouldNotBe null
                manaPool!!.green shouldBe 2
            }

            test("mana empties normally without Upwelling") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .withTurnNumber(2)
                    .inPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
                    .build()

                // Give player 1 some green mana in their pool
                game.state = game.state.updateEntity(game.player1Id) { container ->
                    container.with(ManaPoolComponent(green = 2))
                }

                // Advance through ending/cleanup to opponent's turn
                game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

                // Mana should have been emptied during cleanup
                val manaPool = game.state.getEntity(game.player1Id)?.get<ManaPoolComponent>()
                manaPool shouldNotBe null
                manaPool!!.green shouldBe 0
            }

            test("both players keep mana when Upwelling is on the battlefield") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardOnBattlefield(1, "Upwelling")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .withTurnNumber(2)
                    .inPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
                    .build()

                // Give both players mana
                game.state = game.state.updateEntity(game.player1Id) { container ->
                    container.with(ManaPoolComponent(green = 1, red = 1))
                }
                game.state = game.state.updateEntity(game.player2Id) { container ->
                    container.with(ManaPoolComponent(blue = 3))
                }

                // Advance through the turn
                game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

                // Both players should still have mana
                val p1Mana = game.state.getEntity(game.player1Id)?.get<ManaPoolComponent>()
                p1Mana!!.green shouldBe 1
                p1Mana.red shouldBe 1

                val p2Mana = game.state.getEntity(game.player2Id)?.get<ManaPoolComponent>()
                p2Mana!!.blue shouldBe 3
            }
        }
    }
}

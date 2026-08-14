package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

class VillagersOfEstwaldScenarioTest : ScenarioTestBase() {
    init {
        context("Villagers of Estwald") {
            fun advanceToNextUpkeep(game: TestGame) {
                game.passUntilPhase(Phase.ENDING, Step.END)
                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.resolveStack()
            }

            test("transforms in both directions from the previous turn's spell count") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Villagers of Estwald", summoningSickness = false)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val permanent = game.findPermanent("Villagers of Estwald")!!
                val active = game.state.activePlayerId!!

                game.state = game.state.copy(playerSpellsCastThisTurn = mapOf(active to 0))
                advanceToNextUpkeep(game)
                game.state.getEntity(permanent)!!.get<CardComponent>()!!.name shouldBe "Howlpack of Estwald"

                val nextActive = game.state.activePlayerId!!
                game.state = game.state.copy(playerSpellsCastThisTurn = mapOf(nextActive to 2))
                advanceToNextUpkeep(game)
                game.state.getEntity(permanent)!!.get<CardComponent>()!!.name shouldBe "Villagers of Estwald"

                val thirdActive = game.state.activePlayerId!!
                game.state = game.state.copy(playerSpellsCastThisTurn = mapOf(thirdActive to 1))
                advanceToNextUpkeep(game)
                game.state.getEntity(permanent)!!.get<CardComponent>()!!.name shouldBe "Villagers of Estwald"
            }
        }
    }
}
